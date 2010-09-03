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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.MCDependencyPolicy;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
import org.jboss.ejb3.instantiator.spi.BeanInstantiator;
import org.jboss.ejb3.kernel.JNDIKernelRegistryPlugin;
import org.jboss.ejb3.resolvers.MessageDestinationReferenceResolver;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.singleton.impl.resolver.EjbLinkResolver;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.jndi.resolver.impl.JNDIPolicyBasedJNDINameResolverFactory;
import org.jboss.metadata.ejb.jboss.jndi.resolver.spi.SessionBean31JNDINameResolver;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.DefaultJNDIBindingPolicyFactory;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DefaultJndiBindingPolicy;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
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
public class SingletonContainerDeployer extends AbstractRealDeployerWithInput<JBossEnterpriseBeanMetaData>
      implements
         DeploymentVisitor<JBossEnterpriseBeanMetaData>
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
      //addInput(EJB3Deployment.class);
      addInput(AttachmentNames.PROCESSED_METADATA);
      addInput(org.jboss.ejb3.async.spi.AttachmentNames.ASYNC_INVOCATION_PROCESSOR);
      addInput(org.jboss.ejb3.instantiator.spi.AttachmentNames.NAME_BEAN_INSTANCE_INSTANTIATOR);
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
      if (!isSingletonBean(beanMetaData))
      {
         return;
      }

      ExecutorService asyncExecutorService = (ExecutorService) unit.getAttachment(org.jboss.ejb3.async.spi.AttachmentNames.ASYNC_INVOCATION_PROCESSOR);
      if (asyncExecutorService == null)
      {
         throw new IllegalStateException("No async executor available for deployment unit " + unit);
      }
      
      BeanInstantiator beanInstantiator = (BeanInstantiator) unit.getAttachment(org.jboss.ejb3.instantiator.spi.AttachmentNames.NAME_BEAN_INSTANCE_INSTANTIATOR);
      if (beanInstantiator == null)
      {
         throw new IllegalStateException("Bean instantiator not available in deployment unit: " + unit);
      }
      
      // now start with actual processing
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) beanMetaData;

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
               .getEjbName(), (Domain) singletonContainerAOPDomain.getManager(), ctxProperties, sessionBean, unit, asyncExecutorService);
      }
      catch (ClassNotFoundException cnfe)
      {
         throw new DeploymentException(cnfe);
      }

      // EJBTHREE-2106 https://jira.jboss.org/browse/EJBTHREE-2106
      // One more of those mysterious security integration params.
      // We set the JaccContextId to the simple name of the Deployment unit (don't ask why)
      // It's "copied" from the current implementation in Ejb3AnnotationHandler.getJaccContextId()
      singletonContainer.setJaccContextId(unit.getSimpleName());

      singletonContainer.setEjbReferenceResolver(this.ejbReferenceResolver);
      singletonContainer.setMessageDestinationResolver(this.messageDestinationResolver);
      singletonContainer.setPersistenceUnitResolver(this.puResolver);
      singletonContainer.setBeanInstantiator(beanInstantiator);

      singletonContainer.instantiated();

      singletonContainer.processMetadata();

      // attach the container to the deployment unit, with appropriate MC dependencies
      this.installContainer(unit, singletonContainer.getObjectName().getCanonicalName(), singletonContainer);

   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.AbstractDeployer#undeploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void undeploy(DeploymentUnit unit, JBossEnterpriseBeanMetaData enterpriseBean)
   {
      // nothing to do since the deploy() actually attaches MC beans, to the unit,
      // which will have their own lifecycle methods
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
    * Returns true if the passed <code>beanMetaData</code> corresponds to a Singleton bean.
    * Else returns false.
    * 
    * @param beanMetaData The bean metadata
    * @return
    */
   private boolean isSingletonBean(JBossEnterpriseBeanMetaData beanMetaData)
   {
      // we are only interested in EJB3.1
      if (!beanMetaData.getJBossMetaData().isEJB31())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Not a EJB3.1 bean " + beanMetaData.getName());
         }
         return false;
      }
      // we are not interested in non-session beans 
      if (!beanMetaData.isSession())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Not a session bean " + beanMetaData.getName());
         }
         return false;
      }
      // one last check to make sure we have got the right type!
      if (!(beanMetaData instanceof JBossSessionBean31MetaData))
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Session bean " + beanMetaData.getName() + " is not of type "
                  + JBossSessionBean31MetaData.class);
         }
         return false;
      }

      // now start with actual processing
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) beanMetaData;
      // we are only concerned with Singleton beans
      if (!sessionBean.isSingleton())
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Not a singleton bean " + sessionBean.getName());
         }
         return false;
      }
      // it's a singleton bean
      return true;
   }

   /**
    * Creates a  MC bean for the <code>container</code> and adds it as a {@link BeanMetaData} to the unit
    * <p>
    *   Appropriate dependencies identified through the {@link org.jboss.ejb3.EJBContainer#getDependencyPolicy()} 
    *   are set on the container MC bean. 
    * </p>
    * <p>
    *   This method also processes any depends-on present on the singleton bean and adds the appropriate dependencies
    *   to the container MC bean. When a singleton bean depends on the other singleton bean, we add the following dependencies
    *   to the container MC bean:
    *   <ul>
    *       <li>  A dependency on the target EJB container </li>
    *        <li> A dependency on each of the exposed JNDI names of the target EJB (so that the target EJB can be 
    *               accessed through JNDI within the dependent EJB)</li>
    *   </ul>            
    *
    * @param unit The deployment unit
    * @param containerMCBeanName The MC bean name of the container
    * @param container The container being installed
    */
   private void installContainer(DeploymentUnit unit, String containerMCBeanName, AOPBasedSingletonContainer container)
   {
      BeanMetaDataBuilder containerBMDBuilder = BeanMetaDataBuilder.createBuilder(containerMCBeanName, container
            .getClass().getName());
      containerBMDBuilder.setConstructorValue(container);

      DependencyPolicy containerDependencyPolicy = container.getDependencyPolicy();

      // Process @DependsOn/depends-on
      // Add any dependencies based on @DependsOn/depends-on elements
      JBossSessionBean31MetaData sessionBeanMetaData = (JBossSessionBean31MetaData) container.getMetaData();
      String[] dependsOn = sessionBeanMetaData.getDependsOn();
      if (dependsOn != null)
      {
         EjbLinkResolver ejbLinkResolver = new EjbLinkResolver();
         for (String dependency : dependsOn)
         {
            // resolve the EJB through the ejb link in depends-on
            JBossEnterpriseBeanMetaData dependencyBean = ejbLinkResolver.resolveEJB(dependency, unit);
            if (dependencyBean == null)
            {
               throw new RuntimeException("Could not resolve bean for @DependsOn/depends-on with ejb-name: "
                     + dependency + " while processing EJB named " + container.getEJBName());
            }
            if (isSingletonBean(dependencyBean) == false)
            {
               throw new RuntimeException("@DependsOn/depends-on can only refer to Singleton beans. "
                     + dependencyBean.getEjbClass() + " is not a singleton bean");
            }
            // when a singleton bean depends on the other singleton bean, we add:
            // 1) A dependency on the target EJB container
            // 2) A dependency on each of the exposed JNDI names of the target EJB (so that the 
            // target EJB can be accessed through JNDI within the dependent EJB)
            
            // get the exposed JNDI names
            List<String> jndiNames = this.getExposedJNDINames((JBossSessionBean31MetaData) dependencyBean);
            for (String jndiName : jndiNames)
            {
               // add each jndi name as a dependency
               // Note: The dependency resolution of a dependency with prefix JNDIKernelRegistryPlugin.JNDI_DEPENDENCY_PREFIX
               // is handled by JNDIKernelRegistryPlugin, which does a JNDI lookup to see if the dependency is resolved.
               // Effectively, none of the MC beans have to add the jndi name as an explicit supply. So when the 
               // corresponding jndi binder binds this jndi name to JNDI tree, the dependency will be marked as resolved
               containerDependencyPolicy.addDependency(JNDIKernelRegistryPlugin.JNDI_DEPENDENCY_PREFIX + jndiName);
            }
            String dependencyBeanContainerName = dependencyBean.getContainerName();
            containerDependencyPolicy.addDependency(dependencyBeanContainerName);

         }
      }

      logger.info("Installing container for EJB " + container.getEJBName());
      if (containerDependencyPolicy instanceof MCDependencyPolicy)
      {
         MCDependencyPolicy policy = (MCDependencyPolicy) containerDependencyPolicy;
         // depends
         logger.info("with dependencies: ");
         Set<DependencyMetaData> dependencies = policy.getDependencies();
         if (dependencies != null && dependencies.isEmpty() == false)
         {
            for (DependencyMetaData dependency : dependencies)
            {
               logger.info(dependency.getDependency());
               containerBMDBuilder.addDependency(dependency.getDependency());
            }
         }
         // demands
         logger.info("with demands: ");
         Set<DemandMetaData> demands = policy.getDemands();
         if (demands != null && demands.isEmpty() == false)
         {
            for (DemandMetaData demand : demands)
            {
               logger.info(demand.getDemand());
               containerBMDBuilder.addDemand(demand.getDemand());
            }
         }
         // supplies
         logger.info("with supplies: ");
         Set<SupplyMetaData> supplies = policy.getSupplies();
         if (supplies != null && supplies.isEmpty() == false)
         {
            for (SupplyMetaData supply : supplies)
            {
               logger.info(supply.getSupply());
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
      if (applicationName != null)
      {
         builder.append("application=").append(applicationName).append(",");
      }
      builder.append("module=").append(moduleName);
      if (componentName != null)
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

   /**
    * Returns the JNDI names that are exposed by this <code>sessionBean</code>.
    * Returns an empty list if there are no JNDI names exposed by this session bean.
    * <p>
    *   This method checks whether the bean exposes a remote view, a local view and/or a nointerface view.
    *   Depending on what views the bean exposes, the returned list will contain either or all of the
    *   following JNDI names:
    *   <ul>
    *       <li> default local business interface JNDI name</li>
    *       <li> default remote business interface JNDI name</li>
    *       <li> nointerface view JNDI name</li>
    *   </ul>
    *       
    * </p>
    * @param sessionBean The session bean.
    * @return
    * TODO: Should we even consider business interface specific JNDI names? Will the dependent bean clients
    * expect use these jndi names for lookup?
    */
   private List<String> getExposedJNDINames(JBossSessionBean31MetaData sessionBean)
   {
      List<String> jndiNames = new ArrayList<String>();
    
      DefaultJndiBindingPolicy jndiPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBean31JNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBean, jndiPolicy);
      
      // Determine if there are local/remote views
      BusinessRemotesMetaData businessRemotes = sessionBean.getBusinessRemotes();
      BusinessLocalsMetaData businessLocals = sessionBean.getBusinessLocals();
      
      boolean hasLocalBusinessView = (businessLocals != null && businessLocals.size() > 0);
      boolean hasRemoteBusinessView = (businessRemotes != null && businessRemotes.size() > 0);
      
      // It's got a local business view, so resolve the default local business JNDI name
      // and add it to the list
      if (hasLocalBusinessView)
      {
         String defaultLocalJNDIName = jndiNameResolver.resolveLocalBusinessDefaultJNDIName(sessionBean);
         if (defaultLocalJNDIName != null)
         {
            jndiNames.add(defaultLocalJNDIName);
         }
      }
      
      // It's got a remote business view, so resolve the default remote business JNDI name
      // and add it to the list
      if (hasRemoteBusinessView)
      {
         String defaultRemoteJNDIName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(sessionBean);
         if (defaultRemoteJNDIName != null)
         {
            jndiNames.add(defaultRemoteJNDIName);
         }
      }
      
      // It's got a nointerface view, so resolve the jndi name and add it to the list
      if (sessionBean.isNoInterfaceBean())
      {
         String noInterfaceJNDIName = jndiNameResolver.resolveNoInterfaceJNDIName(sessionBean);
         if (noInterfaceJNDIName != null)
         {
            jndiNames.add(noInterfaceJNDIName);
         }

      }
      // return the exposed JNDI names
      return jndiNames;
   }
}
