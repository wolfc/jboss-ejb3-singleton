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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jboss.aop.AspectManager;
import org.jboss.aop.Domain;
import org.jboss.aop.DomainDefinition;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.singleton.spi.ContainerRegistry;
import org.jboss.injection.inject.naming.InjectionProcessor;
import org.jboss.injection.inject.spi.Injector;
import org.jboss.injection.resolve.spi.EnvironmentMetaDataVisitor;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.javaee.spec.ResourceInjectionMetaData;

/**
 * A MC based deployer for deploying a {@link EJBContainer} as a MC bean
 * for each singleton bean.
 * <p>
 *  Expects {@link JBossEnterpriseBeanMetaData} as an input and processes the metadata
 *  for any potential singleton beans. If any singleton bean is found, then this deployer
 *  creates a {@link BeanMetaData} for a {@link EJBContainer} corresponding to the singleton
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
public class SingletonContainerDeployer extends AbstractDeployer
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonContainerDeployer.class);

   private List<EnvironmentMetaDataVisitor<ResourceInjectionMetaData>> environmentMetadataVisitors = new ArrayList<EnvironmentMetaDataVisitor<ResourceInjectionMetaData>>();

   /**
    * Constructs a {@link SingletonContainerDeployer} for
    * processing singleton beans
    */
   public SingletonContainerDeployer()
   {
      setInput(JBossMetaData.class);
      setStage(DeploymentStages.REAL);

      // we output the container as a MC bean
      addOutput(BeanMetaData.class);

      // ordering
      // addInput(Ejb3Deployment.class);
      // new SPI based EJB3Deployment
      addInput(EJB3Deployment.class);
      addInput(AttachmentNames.PROCESSED_METADATA);
   }

   /**
    * @see org.jboss.deployers.spi.deployer.Deployer#deploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      JBossMetaData metadata = unit.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class);
      if (metadata == null || metadata.getEnterpriseBeans() == null)
      {
         return;
      }
      JBossEnterpriseBeansMetaData enterpriseBeans = metadata.getEnterpriseBeans();
      for (JBossEnterpriseBeanMetaData enterpriseBean : enterpriseBeans)
      {
         this.deploy(unit, enterpriseBean);
      }

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
   private void deploy(DeploymentUnit unit, JBossEnterpriseBeanMetaData beanMetaData) throws DeploymentException
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

      // Register the newly created container with the new SPI based EJB3Deployment
      this.registerWithEJB3Deployment(unit, singletonContainer);

      // TODO: Remove this hack
      ContainerRegistry.INSTANCE.registerContainer(sessionBean.getContainerName(), singletonContainer);

      try
      {
         this.createInjectors(singletonContainer);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Could not process deployment unit " + unit + " for injectors", e);
      }
   }

   private void createInjectors(EJBContainer ejbContainer) throws Exception
   {
      String ejbName = ejbContainer.getEJBName();
      JBossEnterpriseBeanMetaData beanMetadata = ejbContainer.getMetaData();
      ClassLoader containerClassLoader = ejbContainer.getClassLoader();

      InjectionProcessor injectionProcessor = new InjectionProcessor(this.getEnvironmentInjectionVisitors());
      // TODO: We need to add dependency so that we get get the correct ENC for the container
      List<Injector<Object>> beanInstanceInjectors = injectionProcessor.process(ejbContainer.getENC(),
            containerClassLoader, beanMetadata);
      ejbContainer.setEJBInjectors(beanInstanceInjectors);

      // now create injectors for interceptors
      ClassLoader containerCL = ejbContainer.getClassLoader();
      InterceptorsMetaData interceptorsMetadata = JBossMetaData.getInterceptors(ejbName, beanMetadata
            .getJBossMetaData());
      Map<Class<?>, List<Injector<Object>>> interceptorInjectors = new HashMap<Class<?>, List<Injector<Object>>>();
      for (InterceptorMetaData interceptorMetadata : interceptorsMetadata)
      {
         List<Injector<Object>> interceptorInstanceInjectors = injectionProcessor.process(ejbContainer.getENC(),
               containerClassLoader, interceptorMetadata);
         Class<?> interceptorClass = containerCL.loadClass(interceptorMetadata.getInterceptorClass());
         interceptorInjectors.put(interceptorClass, interceptorInstanceInjectors);
      }
      ejbContainer.getInterceptorRegistry().setInterceptorInjectors(interceptorInjectors);
   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.AbstractDeployer#undeploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void undeploy(DeploymentUnit unit)
   {

      super.undeploy(unit);
      // TODO: need to do proper implementation here - to unregister the container from the EJB3Deployment 
      // and other stuff
   }

   private void registerWithEJB3Deployment(DeploymentUnit unit, EJBContainer container)
   {
      EJB3Deployment ejb3Deployment = unit.getAttachment(EJB3Deployment.class);
      if (ejb3Deployment != null)
      {
         ejb3Deployment.addContainer(container);
      }
      return;
   }

   @Inject
   public void setEnvironmentInjectionVisitors(
         List<EnvironmentMetaDataVisitor<ResourceInjectionMetaData>> envMetadataVisitors)
   {
      this.environmentMetadataVisitors = envMetadataVisitors;
   }

   public List<EnvironmentMetaDataVisitor<ResourceInjectionMetaData>> getEnvironmentInjectionVisitors()
   {
      return this.environmentMetadataVisitors;
   }

}
