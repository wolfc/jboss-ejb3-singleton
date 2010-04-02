/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.ejb3.singleton.deployer;

import java.util.Hashtable;
import java.util.Set;

import org.jboss.aop.AspectManager;
import org.jboss.aop.Domain;
import org.jboss.aop.DomainDefinition;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.DemandMetaData;
import org.jboss.beans.metadata.spi.DependencyMetaData;
import org.jboss.beans.metadata.spi.SupplyMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployerWithInput;
import org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.MCDependencyPolicy;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.common.resolvers.spi.EjbReferenceResolver;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.ejb3.resolvers.MessageDestinationReferenceResolver;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.reloaded.naming.deployers.javaee.JavaEEComponentInformer;
import org.jboss.reloaded.naming.spi.JavaEEComponent;

/**
 * A MC based deployer for deploying a {@link EJBContainer} as a MC bean
 * for each singleton bean.
 * <p>
 *  Expects {@link JBossEnterpriseBeanMetaData} as an input and processes the metadata
 *  for any potential singleton beans. If any singleton bean is found, then this deployer
 *  creates a {@link BeanMetaData} for a container corresponding to the singleton
 *  bean. The {@link BeanMetaData} is then attached to the {@link DeploymentUnit} so that MC
 *  can deploy it as MC bean. 
 * </p>
 * <p>
 * Before deploying the container as a MC bean, this deployer sets up appropriate dependencies for 
 * the container.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonContainerDeployer extends AbstractRealDeployerWithInput<JBossEnterpriseBeanMetaData> implements DeploymentVisitor<JBossEnterpriseBeanMetaData>
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonContainerDeployer.class);
   
   /**
    * Message destination resolver to be set in the container, which this
    * deployer creates
    */
   private MessageDestinationReferenceResolver messageDestinationResolver;
   
   /**
    * Ejb reference resolver to be set in the container, which this
    * deployer creates
    */
   private EjbReferenceResolver ejbReferenceResolver;
   
   /**
    * Persistence unit resolver to be set in the container, which this
    * deployer creates
    */
   private PersistenceUnitDependencyResolver puResolver;

   /**
    * component informer
    */
   private JavaEEComponentInformer javaeeComponentInformer;
   
   /**
    * Constructs a {@link SingletonContainerDeployer} for
    * processing singleton beans
    */
   public SingletonContainerDeployer()//JavaEEComponentInformer javaCompInformer)
   {
      //setInput(JBossMetaData.class);
      //this.javaeeComponentInformer = javaCompInformer;
//      Set<String> inputs = new HashSet<String>();
//      inputs.add(JBossEnterpriseBeanMetaData.class.getName());
//      List<String> javaCompRequiredAttachments = Arrays.asList(this.javaeeComponentInformer.getRequiredAttachments());
//      inputs.addAll(javaCompRequiredAttachments);
//      
//      this.setInputs(inputs);
      this.setDeploymentVisitor(this);
      this.setInput(JBossEnterpriseBeanMetaData.class);
      this.setComponentsOnly(true);
     // setStage(DeploymentStages.REAL);

      // we output the container as a MC bean
      addOutput(BeanMetaData.class);
      addOutput(org.jboss.ejb3.EJBContainer.class);

      // ordering
      // new SPI based EJB3Deployment
      addInput(EJB3Deployment.class);
      addInput(AttachmentNames.PROCESSED_METADATA);
   }


   /**
    * Processes a {@link DeploymentUnit} with {@link JBossEnterpriseBeanMetaData} to
    * check if it's a singleton bean.
    * <p>
    *  If it's a singleton bean then this method creates a {@link EJBContainer}, for that singleton bean and 
    *  deploys it as a MC bean
    * </p>
    * 
    */
   @Override
   public void deploy(DeploymentUnit unit, JBossEnterpriseBeanMetaData beanMetaData) throws DeploymentException
   {
      // we are only interested in EJB3.1
      if (!beanMetaData.getJBossMetaData().isEJB31())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Ignoring non-EJB3.1 bean " + beanMetaData.getName());
         }
         return;
      }
      // we are not interested in non-session beans 
      if (!beanMetaData.isSession())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Ignoring non-session bean " + beanMetaData.getName());
         }
         return;
      }
      // one last check to make sure we have got the right type!
      if (!(beanMetaData instanceof JBossSessionBean31MetaData))
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Ignoring bean " + beanMetaData.getName() + " because its metadata is not of type "
                  + JBossSessionBean31MetaData.class);
         }
         return;
      }

      // now start with actual processing
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) beanMetaData;
      // we are only concerned with Singleton beans
      if (!sessionBean.isSingleton())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Ignoring non-singleton bean " + sessionBean.getName());
         }
         return;
      }

      // Create a singleton container
      ClassLoader classLoader = unit.getClassLoader();
      String domainName = AOPBasedSingletonContainer.getAOPDomainName();
      DomainDefinition singletonContainerAOPDomain = AspectManager.instance().getContainer(domainName);
      if (singletonContainerAOPDomain == null)
      {
         throw new DeploymentException(domainName + " AOP domain not configured - cannot deploy EJB named "
               + beanMetaData.getEjbName() + " in unit " + unit);
      }
      Hashtable<String, String> ctxProperties = new Hashtable<String, String>();
      AOPBasedSingletonContainer singletonContainer;
      try
      {
         singletonContainer = new AOPBasedSingletonContainer(classLoader, sessionBean.getEjbClass(), sessionBean
               .getEjbName(), (Domain) singletonContainerAOPDomain.getManager(), ctxProperties, sessionBean, unit);
      }
      catch (ClassNotFoundException cnfe)
      {
         throw new DeploymentException(cnfe);
      }

      singletonContainer.setEjbReferenceResolver(this.ejbReferenceResolver);
      singletonContainer.setMessageDestinationResolver(this.messageDestinationResolver);
      singletonContainer.setPersistenceUnitResolver(this.puResolver);
      
      singletonContainer.instantiated();

      singletonContainer.processMetadata();

      // Register the newly created container with the new SPI based EJB3Deployment
      this.registerWithEJB3Deployment(unit, singletonContainer);
      
      this.installContainer(unit, singletonContainer.getObjectName().getCanonicalName(), singletonContainer);
      
   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.AbstractDeployer#undeploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void undeploy(DeploymentUnit unit, JBossEnterpriseBeanMetaData enterpriseBean)
   {

      //super.undeploy(unit);
      // TODO: need to do proper implementation here - to unregister the container from the EJB3Deployment 
      // and other stuff
   }

   /** 
    * @see org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor#getVisitorType()
    */
   @Override
   public Class<JBossEnterpriseBeanMetaData> getVisitorType()
   {
      return JBossEnterpriseBeanMetaData.class;
   }
   
   /**
    * 
    * @param unit Deployment unit
    * @param container EJB container
    */
   private void registerWithEJB3Deployment(DeploymentUnit unit, EJBContainer container)
   {
      EJB3Deployment ejb3Deployment = unit.getAttachment(EJB3Deployment.class);
      if (ejb3Deployment != null)
      {
         ejb3Deployment.addContainer(container);
      }
      return;
   }
   
   /**
    * Creates a  MC bean for the <code>container</code> and adds it as a {@link BeanMetaData} to the unit
    * <p>
    *   Appropriate dependencies are set on the container MC bean 
    * </p>
    * @param unit The deployment unit
    * @param containerMCBeanName The MC bean name of the container
    * @param container The container being installed
    */
   private void installContainer(DeploymentUnit unit, String containerMCBeanName, AOPBasedSingletonContainer container)
   {
      BeanMetaDataBuilder containerBMDBuilder = BeanMetaDataBuilder.createBuilder(containerMCBeanName, container.getClass().getName());
      containerBMDBuilder.setConstructorValue(container);
      
      // TODO: Alternate way (instead of relying on DependencyPolicy of EJBContainer
      //      JBossSessionBean31MetaData sessionbean = (JBossSessionBean31MetaData) container.getMetaData();
//      String localhome = sessionbean.getLocalHome();
//      containerBMDBuilder.addSupply("Class:"+localhome);
//      String remoteHome = sessionbean.getHome();
//      containerBMDBuilder.addSupply("Class:"+remoteHome);
//      BusinessLocalsMetaData businessLocals = sessionbean.getBusinessLocals();
//      if (businessLocals != null)
//      {
//         for (String businessLocal : businessLocals)
//         {
//            containerBMDBuilder.addSupply("Class:"+businessLocal);
//         }
//      }
//      BusinessRemotesMetaData businessRemotes = sessionbean.getBusinessRemotes();
//      if (businessRemotes != null)
//      {
//         for (String businessRemote : businessRemotes)
//         {
//            containerBMDBuilder.addSupply("Class:"+businessRemote);
//         }
//      }
      if (container.getDependencyPolicy() instanceof MCDependencyPolicy)
      {
         MCDependencyPolicy policy = (MCDependencyPolicy) container.getDependencyPolicy();
         // depends
         Set<DependencyMetaData> dependencies = policy.getDependencies();
         if (dependencies != null && dependencies.isEmpty() == false)
         {
            for (DependencyMetaData dependency : dependencies)
            {
               containerBMDBuilder.addDependency(dependency.getDependency());
            }
         }
         // demands
         Set<DemandMetaData> demands = policy.getDemands();
         if (demands != null && demands.isEmpty() == false)
         {
            for (DemandMetaData demand : demands)
            {
               containerBMDBuilder.addDemand(demand.getDemand());
            }
         }
         // supplies
         Set<SupplyMetaData> supplies = policy.getSupplies();
         if (supplies != null && supplies.isEmpty() == false)
         {
            for (SupplyMetaData supply : supplies)
            {
               containerBMDBuilder.addSupply(supply.getSupply());
            }
         }
      }
      // Add inject metadata on container
      String javaCompMCBeanName = this.getJavaEEComponentMCBeanName(unit);
      AbstractInjectionValueMetaData javaCompInjectMetaData = new AbstractInjectionValueMetaData(javaCompMCBeanName);
      // Too bad we have to know the field name. Need to do more research on MC to see if we can
      // add property metadata based on type instead of field name.
      containerBMDBuilder.addPropertyMetaData("javaComp", javaCompInjectMetaData);
      
      // TODO: This is an undocumented nonsense of MC
      DeploymentUnit parentUnit = unit.getParent();
      parentUnit.addAttachment(BeanMetaData.class + ":" + containerMCBeanName, containerBMDBuilder.getBeanMetaData());
      unit.addAttachment(org.jboss.ejb3.EJBContainer.class + ":" + containerMCBeanName, container);
   }

   /**
    * Returns the {@link JavaEEComponent} MC bean name
    * @param deploymentUnit
    * @return
    */
   private String getJavaEEComponentMCBeanName(DeploymentUnit deploymentUnit)
   {
      String applicationName = this.javaeeComponentInformer.getApplicationName(deploymentUnit);
      String moduleName = this.javaeeComponentInformer.getModulePath(deploymentUnit);
      String componentName = this.javaeeComponentInformer.getComponentName(deploymentUnit); 
 
      final StringBuilder builder = new StringBuilder("jboss.naming:");
      if(applicationName != null)
      {
         builder.append("application=").append(applicationName).append(",");
      }
      builder.append("module=").append(moduleName);
      if(componentName != null)
      {
         builder.append(",component=").append(componentName);
      }
      return builder.toString();
   }
   
   
   @Inject
   public void setPersistenceUnitResolver(PersistenceUnitDependencyResolver puResolver)
   {
      this.puResolver = puResolver;
   }

   @Inject
   public void setMessageDestinationResolver(MessageDestinationReferenceResolver messageDestResolver)
   {
      this.messageDestinationResolver = messageDestResolver;
   }
   
   @Inject
   public void setEjbRefResolver(EjbReferenceResolver ejbRefResolver)
   {
      this.ejbReferenceResolver = ejbRefResolver;
   }
   
   @Inject
   public void setJavaEEComponentInformer(JavaEEComponentInformer componentInformer)
   {
      this.javaeeComponentInformer = componentInformer;
   }
}
