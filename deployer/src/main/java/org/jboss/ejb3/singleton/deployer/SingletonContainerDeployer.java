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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.jboss.aop.AspectManager;
import org.jboss.aop.Domain;
import org.jboss.aop.DomainDefinition;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.ejb3.container.spi.injection.DependencyBasedInjector;
import org.jboss.ejb3.container.spi.injection.EJBContainerENCInjector;
import org.jboss.ejb3.container.spi.injection.InjectorFactory;
import org.jboss.ejb3.container.spi.injection.InstanceInjector;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.singleton.aop.impl.injection.PersistenceContextInjectorFactory;
import org.jboss.ejb3.singleton.proxy.impl.SingletonBeanRemoteJNDIBinder;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.logging.Logger;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;

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

   private PersistenceUnitDependencyResolver persistenceUnitResolver;

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
      //      Ejb3Deployment ejb3Deployment = unit.getAttachment(Ejb3Deployment.class);
      //      if (ejb3Deployment == null)
      //      {
      //         throw new DeploymentException("Could not find " + Ejb3Deployment.class
      //               + " for creating singleton container for bean " + sessionBean.getEjbName() + " in unit " + unit);
      //      }
      AOPBasedSingletonContainer singletonContainer = null;
      try
      {
         singletonContainer = new AOPBasedSingletonContainer(classLoader, sessionBean.getEjbClass(), sessionBean
               .getEjbName(), (Domain) singletonContainerAOPDomain.getManager(), ctxProperties, sessionBean, unit);
      }
      catch (ClassNotFoundException cnfe)
      {
         throw new DeploymentException("Could not find class during deployment of bean named "
               + sessionBean.getEjbName() + " in unit " + unit, cnfe);
      }

      // Register the newly created container with the new SPI based EJB3Deployment
      this.registerWithEJB3Deployment(unit, singletonContainer);

      String singletonContainerMCBeanName;
      try
      {
         // get the name for the MC bean 
         singletonContainerMCBeanName = this.getContainerName(unit, sessionBean);

         //TODO: One more ugly hack (to allow backward compatibility)
         // Webservices expects the metadata to provide container name
         sessionBean.setContainerName(singletonContainerMCBeanName);
      }
      catch (MalformedObjectNameException e)
      {
         throw new DeploymentException("Could not obtain a container name for bean " + sessionBean.getName()
               + " in unit " + unit);
      }

      // Here we let the injection handlers to setup appropriate dependencies
      // on the container and also create injectors for the container
      singletonContainer.instantiated();
      // singletonContainer.processMetadata();

      List<InjectorFactory> injectorFactories = new ArrayList<InjectorFactory>();
      PersistenceContextInjectorFactory pcInjectorFactory = new PersistenceContextInjectorFactory(unit,
            this.persistenceUnitResolver);
      injectorFactories.add(pcInjectorFactory);
      List<EJBContainerENCInjector> encInjectors = new ArrayList<EJBContainerENCInjector>();
      encInjectors.addAll(pcInjectorFactory.createENCInjectors(sessionBean));
      List<InstanceInjector> beanInstanceInjectors = new ArrayList<InstanceInjector>();

      try
      {
         Class<?> beanClass = unit.getClassLoader().loadClass(sessionBean.getEjbClass());
         Set<Method> methods = this.getInjectableMethods(beanClass, new HashSet<Method>(), injectorFactories);
         for (Method method : methods)
         {
            for (InjectorFactory injectorFactory : injectorFactories)
            {
               EJBContainerENCInjector encInjector = injectorFactory.createENCInjector(method);
               if (encInjector != null)
               {
                  encInjectors.add(encInjector);
               }
               InstanceInjector beanInstanceInjector = injectorFactory.createInstanceInjector(method);
               if (beanInstanceInjector != null)
               {
                  beanInstanceInjectors.add(beanInstanceInjector);
               }
            }
         }
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(e);
      }
      singletonContainer.setENCInjectors(encInjectors);
      singletonContainer.setEJBInjectors(beanInstanceInjectors);

      List<DependencyBasedInjector> allInjectors = new ArrayList<DependencyBasedInjector>();
      allInjectors.addAll(singletonContainer.getEJBInjectors());
      allInjectors.addAll(singletonContainer.getENCInjectors());
      this.installContainer(unit, singletonContainerMCBeanName, singletonContainer, allInjectors);

      // TODO: This shouldn't be here
      SingletonBeanRemoteJNDIBinder jndiBinder = new SingletonBeanRemoteJNDIBinder(singletonContainer);
      try
      {
         jndiBinder.bind(new InitialContext());
      }
      catch (Exception e)
      {
         throw new DeploymentException(e);
      }

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

   /**
    * Ultimately, the container name should come from the <code>sessionBeanMetadata</code>.
    * However because of the current behaviour where the container on its start sets the containername
    * in the metadata, its not possible to get this information even before the container is started.
    *
    * Hence let's for the time being create the container name from all the information that we have
    * in the <code>unit</code>
    *
    * @param unit The deployment unit
    * @param sessionBeanMetadata Session bean metadata
    * @return Returns the container name for the bean corresponding to the <code>sessionBeanMetadata</code> in the <code>unit</code>
    *
    * @throws MalformedObjectNameException
    */
   private String getContainerName(DeploymentUnit unit, JBossSessionBeanMetaData sessionBeanMetadata)
         throws MalformedObjectNameException
   {
      // TODO the base ejb3 jmx object name comes from Ejb3Module.BASE_EJB3_JMX_NAME, but
      // we don't need any reference to ejb3-core. Right now just hard code here, we need
      // a better way/place for this later
      StringBuilder containerName = new StringBuilder("jboss.j2ee:service=EJB3" + ",");

      // Get the top level unit for this unit (ex: the top level might be an ear and this unit might be the jar
      // in that ear
      DeploymentUnit toplevelUnit = unit.getTopLevel();
      if (toplevelUnit != null)
      {
         // if top level is an ear, then create the name with the ear reference
         if (isEar(toplevelUnit))
         {
            containerName.append("ear=");
            containerName.append(toplevelUnit.getSimpleName());
            containerName.append(",");

         }
      }
      // now work on the passed unit, to get the jar name
      if (unit.getSimpleName() == null)
      {
         containerName.append("*");
      }
      else
      {
         containerName.append("jar=");
         containerName.append(unit.getSimpleName());
      }
      // now the ejbname
      containerName.append(",name=");
      containerName.append(sessionBeanMetadata.getEjbName());

      if (logger.isTraceEnabled())
      {
         logger.trace("Container name generated for ejb = " + sessionBeanMetadata.getEjbName() + " in unit " + unit
               + " is " + containerName);
      }
      ObjectName containerJMXName = new ObjectName(containerName.toString());
      return containerJMXName.getCanonicalName();
   }

   /**
    * Returns true if this <code>unit</code> represents an .ear deployment
    *
    * @param unit
    * @return
    */
   private boolean isEar(DeploymentUnit unit)
   {
      return unit.getSimpleName().endsWith(".ear") || unit.getAttachment(JBossAppMetaData.class) != null;
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
   public void setPersistenceUnitResolver(PersistenceUnitDependencyResolver puResolver)
   {
      this.persistenceUnitResolver = puResolver;
   }

   private Set<Method> getInjectableMethods(Class<?> clazz, Set<Method> visitedMethods,
         List<InjectorFactory> injectorFactories)
   {
      if (clazz == null || clazz.equals(Object.class))
      {
         return Collections.EMPTY_SET;
      }
      Method[] methods = clazz.getDeclaredMethods();
      //List<InstanceInjector> injectors = new ArrayList<InstanceInjector>();
      for (Method method : methods)
      {
         if (method.getParameterTypes().length != 1)
            continue;

         if (!Modifier.isPrivate(method.getModifiers()))
         {
            if (visitedMethods.contains(method.getName()))
            {
               continue;
            }
            visitedMethods.add(method);
         }

         //         if (injectorFactories != null)
         //         {
         //            for (InjectorFactory injectorFactory : injectorFactories)
         //            {
         //               InstanceInjector injector = injectorFactory.createInstanceInjector(method);
         //               if (injector != null)
         //               {
         //                  injectors.add(injector);
         //               }
         //            }
         //         }
      }
      // recursion needs to come last as the method could be overriden and we don't want the overriding method to be ignored
      getInjectableMethods(clazz.getSuperclass(), visitedMethods, injectorFactories);

      return visitedMethods;
   }

   private void installContainer(DeploymentUnit unit, String containerMCBeanName, EJBContainer container,
         List<DependencyBasedInjector> injectors)
   {
      BeanMetaDataBuilder containerBMDBuilder = BeanMetaDataBuilder.createBuilder(containerMCBeanName, container
            .getClass().getName());
      containerBMDBuilder.setConstructorValue(container);

      // TODO: Hack! (for quick testing)
      JBossSessionBean31MetaData sessionbean = (JBossSessionBean31MetaData) container.getMetaData();
      String localhome = sessionbean.getLocalHome();
      containerBMDBuilder.addSupply("Class:" + localhome);
      String remoteHome = sessionbean.getHome();
      containerBMDBuilder.addSupply("Class:" + remoteHome);
      BusinessLocalsMetaData businessLocals = sessionbean.getBusinessLocals();
      if (businessLocals != null)
      {
         for (String businessLocal : businessLocals)
         {
            containerBMDBuilder.addSupply("Class:" + businessLocal);
         }
      }
      BusinessRemotesMetaData businessRemotes = sessionbean.getBusinessRemotes();
      if (businessRemotes != null)
      {
         for (String businessRemote : businessRemotes)
         {
            containerBMDBuilder.addSupply("Class:" + businessRemote);
         }
      }

      for (DependencyBasedInjector injector : injectors)
      {
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(injector.toString(), injector.getClass()
               .getName());
         builder.setConstructorValue(injector);
         for (String dep : injector.getDependencies())
         {
            builder.addDependency(dep);
         }
         unit.addAttachment(BeanMetaData.class + ":" + injector.toString(), builder.getBeanMetaData());

         containerBMDBuilder.addDependency(injector.toString());
      }

      unit.addAttachment(BeanMetaData.class + ":" + containerMCBeanName, containerBMDBuilder.getBeanMetaData());
   }
}
