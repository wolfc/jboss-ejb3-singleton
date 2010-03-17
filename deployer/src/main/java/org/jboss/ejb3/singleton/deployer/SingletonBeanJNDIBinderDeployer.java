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

import java.lang.reflect.InvocationHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.common.registrar.spi.Ejb3RegistrarLocator;
import org.jboss.ejb3.common.registrar.spi.NotBoundException;
import org.jboss.ejb3.container.spi.remote.RemotingContainer;
import org.jboss.ejb3.proxy.reflect.ReflectProxyFactory;
import org.jboss.ejb3.proxy.spi.factory.ProxyFactory;
import org.jboss.ejb3.singleton.impl.remoting.JBossRemotingContainer;
import org.jboss.ejb3.singleton.proxy.impl.SingletonBeanRemoteInvocationHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.LocalBindingMetaData;
import org.jboss.metadata.ejb.jboss.RemoteBindingMetaData;
import org.jboss.metadata.ejb.jboss.jndi.resolver.impl.JNDIPolicyBasedJNDINameResolverFactory;
import org.jboss.metadata.ejb.jboss.jndi.resolver.spi.SessionBeanJNDINameResolver;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.DefaultJNDIBindingPolicyFactory;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DefaultJndiBindingPolicy;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * SingletonBeanJNDIRegistrar
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanJNDIBinderDeployer extends AbstractDeployer
{

   //private JBossSessionBeanMetaData sessionBeanMetadata;

   private static Logger logger = Logger.getLogger(SingletonBeanJNDIBinderDeployer.class);

   private final static String DEFAULT_REMOTING_URL = "socket://" + System.getProperty("jboss.bind.address", "0.0.0.0")
         + ":3873";

   /**
    * The name under which the Remoting Connector is bound in MC
    */
   private static final String REMOTING_CONNECTOR_MC_BEAN_NAME = "org.jboss.ejb3.RemotingConnector";

   public SingletonBeanJNDIBinderDeployer()
   {
      this.setStage(DeploymentStages.REAL);
      this.setInput(JBossMetaData.class);

      this.addInput(AttachmentNames.PROCESSED_METADATA);

      this.addOutput(BeanMetaData.class);
   }

   /**
    * @see org.jboss.deployers.spi.deployer.Deployer#deploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      JBossMetaData jbossMetaData = unit.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class);
      if (jbossMetaData == null || jbossMetaData.isEJB3x() == false)
      {
         return;
      }
      JBossEnterpriseBeansMetaData enterpriseBeans = jbossMetaData.getEnterpriseBeans();
      if (enterpriseBeans == null || enterpriseBeans.isEmpty())
      {
         return;
      }
      for (JBossEnterpriseBeanMetaData enterpriseBean : enterpriseBeans)
      {
         if (!enterpriseBean.isSession() || !(enterpriseBean instanceof JBossSessionBean31MetaData))
         {
            continue;
         }
         JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) enterpriseBean;
         if (!sessionBean.isSingleton())
         {
            continue;
         }
         process(unit, sessionBean);
      }

   }

   private void process(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean) throws DeploymentException
   {
      this.processRemoteBusinessInterfaces(unit, sessionBean);
      this.processLocalBusinessInterfaces(unit, sessionBean);
   }

   private void processRemoteBusinessInterfaces(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean)
         throws DeploymentException
   {
      BusinessRemotesMetaData businessRemotes = sessionBean.getBusinessRemotes();
      if (businessRemotes == null || businessRemotes.size() == 0)
      {
         return;
      }
      String defaultInvokerLocatorURL = this.getDefaultInvokerLocatorURL();

      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBean, jndibindingPolicy);

      // TODO: Rethink
      String containerRegistryKey = sessionBean.getContainerName();
      Context jndiCtx = null;
      try
      {
         jndiCtx = new InitialContext();
      }
      catch (NamingException ne)
      {
         throw new DeploymentException("Could not create jndi context for jndi binders", ne);
      }

      ProxyFactory proxyFactory = new ReflectProxyFactory();

      Set<Class<?>> allRemoteinterfaces = new HashSet<Class<?>>();
      for (String businessRemote : businessRemotes)
      {
         try
         {
            Class<?> businessRemoteIntf = unit.getClassLoader().loadClass(businessRemote);
            allRemoteinterfaces.add(businessRemoteIntf);

            RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey,
                  defaultInvokerLocatorURL);
            InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer,
                  businessRemoteIntf);

            // time to create a proxy
            Object proxy = proxyFactory.createProxy(new Class<?>[]
            {businessRemoteIntf}, invocationHandler);

            // bind to jndi

            String jndiName = jndiNameResolver.resolveJNDIName(sessionBean, businessRemote);

            JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, jndiName, proxy);
            String binderMCBeanName = "<JNDIBinder><BusinessRemote:" + businessRemote + "><Bean:"
                  + sessionBean.getEjbName() + "><Unit:" + unit.getName() + ">";
            BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
            unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);

         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException("Could not load business remote interface " + businessRemote, cnfe);
         }

      }

      // time to create a proxy
      List<RemoteBindingMetaData> remoteBindings = sessionBean.getRemoteBindings();
      if (remoteBindings == null || remoteBindings.isEmpty())
      {
         RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey,
               defaultInvokerLocatorURL);
         InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer);
         String defaultRemoteJNDIName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(sessionBean);
         Object proxy = proxyFactory.createProxy(allRemoteinterfaces.toArray(new Class<?>[allRemoteinterfaces.size()]),
               invocationHandler);
         JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, defaultRemoteJNDIName, proxy);
         String binderMCBeanName = "<DefaultRemoteJNDIBinder><Bean:" + sessionBean.getEjbName() + "><Unit:"
               + unit.getName() + ">";
         BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
         unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);

      }
      else
      {
         for (RemoteBindingMetaData remoteBinding : remoteBindings)
         {
            String jndiName = remoteBinding.getJndiName();
            // if not explicitly specified, then use the default jndi name
            if (jndiName == null)
            {
               jndiName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(sessionBean);
            }
            String invokerLocatorURL = this.getClientBindURL(remoteBinding);
            RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey, invokerLocatorURL);
            InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer);
            Object proxy = proxyFactory.createProxy(allRemoteinterfaces
                  .toArray(new Class<?>[allRemoteinterfaces.size()]), invocationHandler);
            JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, jndiName, proxy);
            String binderMCBeanName = "<DefaultRemoteJNDIBinder><InvokerLocatorURL:" + invokerLocatorURL + "><Bean:"
                  + sessionBean.getEjbName() + "><Unit:" + unit.getName() + ">";
            BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
            unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);
         }

      }

   }

   private void processLocalBusinessInterfaces(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean)
         throws DeploymentException
   {
      BusinessLocalsMetaData businessLocals = sessionBean.getBusinessLocals();
      if (businessLocals == null || businessLocals.size() == 0)
      {
         return;
      }
      String defaultInvokerLocatorURL = this.getDefaultInvokerLocatorURL();

      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBean, jndibindingPolicy);

      // TODO: Rethink
      String containerRegistryKey = sessionBean.getContainerName();

      ProxyFactory proxyFactory = new ReflectProxyFactory();

      Context jndiCtx = null;
      try
      {
         jndiCtx = new InitialContext();
      }
      catch (NamingException ne)
      {
         throw new DeploymentException("Could not create jndi context for jndi binders", ne);
      }
      Set<Class<?>> allLocalinterfaces = new HashSet<Class<?>>();
      for (String businessLocal : businessLocals)
      {
         Class<?> businessLocalIntf = null;
         try
         {
            businessLocalIntf = unit.getClassLoader().loadClass(businessLocal);
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException("Could not load business local interface " + businessLocal, cnfe);
         }
         allLocalinterfaces.add(businessLocalIntf);

         RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey,
               defaultInvokerLocatorURL);
         InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer,
               businessLocalIntf);

         // time to create a proxy
         Object proxy = proxyFactory.createProxy(new Class<?>[]
         {businessLocalIntf}, invocationHandler);

         // bind to jndi

         String jndiName = jndiNameResolver.resolveJNDIName(sessionBean, businessLocal);

         JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, jndiName, proxy);
         String binderMCBeanName = "<JNDIBinder><BusinessLocal:" + businessLocal + "><Bean:"
               + sessionBean.getEjbName() + "><Unit:" + unit.getName() + ">";
         BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
         unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);

      }

      List<LocalBindingMetaData> localBindings = sessionBean.getLocalBindings();
      if (localBindings == null || localBindings.isEmpty())
      {
         RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey,
               defaultInvokerLocatorURL);
         InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer);
         String defaultBusinessLocalJNDIName = jndiNameResolver.resolveLocalBusinessDefaultJNDIName(sessionBean);
         Object proxy = proxyFactory.createProxy(allLocalinterfaces.toArray(new Class<?>[allLocalinterfaces.size()]),
               invocationHandler);
         JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, defaultBusinessLocalJNDIName, proxy);
         String binderMCBeanName = "<DefaultLocalJNDIBinder><Bean:" + sessionBean.getEjbName() + "><Unit:"
               + unit.getName() + ">";
         BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
         unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);
      }
      else
      {
         for (LocalBindingMetaData localBinding : localBindings)
         {
            String jndiName = localBinding.getJndiName();
            // if not explicitly specified, then use the default jndi name
            if (jndiName == null)
            {
               jndiName = jndiNameResolver.resolveLocalBusinessDefaultJNDIName(sessionBean);
            }
            RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey,
                  defaultInvokerLocatorURL);
            InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer);
            Object proxy = proxyFactory.createProxy(
                  allLocalinterfaces.toArray(new Class<?>[allLocalinterfaces.size()]), invocationHandler);
            JNDIBinderImpl jndiBinder = new JNDIBinderImpl(jndiCtx, jndiName, proxy);
            String binderMCBeanName = "<DefaultLocalJNDIBinder><Bean:" + sessionBean.getEjbName() + "><Unit:"
                  + unit.getName() + ">" + UUID.randomUUID();
            BeanMetaData jndiBinderBMD = this.createBeanMetaData(binderMCBeanName, jndiBinder, null);
            unit.addAttachment(BeanMetaData.class + ":" + binderMCBeanName, jndiBinderBMD);

         }

      }

   }

   private String getClientBindURL(RemoteBindingMetaData remoteBinding)
   {

      // RemoteBinding allows for specifying the invoker (MC bean) name through
      // which the invoker locator url can be obtained. check if it's explicitly 
      // specified
      String invokerName = remoteBinding.getInvokerName();
      String generatedClientBindURL = null;
      if (invokerName != null && invokerName.trim().isEmpty() == false)
      {
         invokerName = invokerName.trim();
         generatedClientBindURL = this.getInvokerLocatorURL(invokerName);
      }
      // RemoteBinding also allows for specifying clientBindURL explicitly
      // Check if it's specified. Obviously, specifying both client bind url
      // and the invokername is wrong. So let's print out a WARN and use the
      // client bind url generated through invokerName (that way, the client
      // bind url can be overridden through xml).
      String explicitClientBindURL = remoteBinding.getClientBindUrl();
      if (explicitClientBindURL != null && explicitClientBindURL.trim().isEmpty())
      {
         explicitClientBindURL = null;
      }
      if (explicitClientBindURL != null && generatedClientBindURL != null)
      {
         logger
               .warn("Both invokerName and clientBindURL specified on RemoteBinding. Ignoring the explicitly specified bind url "
                     + explicitClientBindURL + " and instead using " + generatedClientBindURL);
      }
      String bindURL = generatedClientBindURL == null ? explicitClientBindURL : generatedClientBindURL;
      return bindURL == null ? this.getDefaultInvokerLocatorURL() : bindURL;
   }

   /**
    * Obtains the client binding for the specified 
    * invokerName (supplied as the Object Store bind name in
    * MC)
    * 
    * @param invokerName
    * @return
    * @throws NotBoundException If the specified invokerName is not bound in MC
    */
   private String getInvokerLocatorURL(String invokerName) throws NotBoundException
   {
      // Initialize
      String url = null;
      Connector connector = null;

      // Lookup the Connector in MC
      try
      {
         connector = Ejb3RegistrarLocator.locateRegistrar().lookup(invokerName, Connector.class);
      }
      catch (NotBoundException nbe)
      {
         // Log and rethrow
         logger.warn("Could not find the remoting connector for the specified invoker name, " + invokerName + " in MC");
         throw nbe;
      }

      // Use the binding specified by the Connector
      try
      {
         url = connector.getInvokerLocator();
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not obtain " + InvokerLocator.class.getSimpleName()
               + " from EJB3 Remoting Connector", e);
      }

      // Return
      return url;
   }

   private String getDefaultInvokerLocatorURL()
   {
      String invokerURL = this.getInvokerLocatorURL(REMOTING_CONNECTOR_MC_BEAN_NAME);
      return invokerURL == null ? DEFAULT_REMOTING_URL : invokerURL;
   }

   private BeanMetaData createBeanMetaData(String beanName, Object beanInstance, List<String> dependencies)
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanName, beanInstance.getClass().getName());
      builder.setConstructorValue(beanInstance);

      if (dependencies != null)
      {
         for (String dependency : dependencies)
         {
            if (dependency == null)
            {
               continue;
            }
            builder.addDependency(dependency);
         }
      }
      return builder.getBeanMetaData();

   }
}
