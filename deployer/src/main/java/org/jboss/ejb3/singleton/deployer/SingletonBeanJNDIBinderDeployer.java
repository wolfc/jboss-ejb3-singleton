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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.aop.AspectManager;
import org.jboss.aop.advice.AdviceStack;
import org.jboss.aop.advice.Interceptor;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployerWithInput;
import org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Registry;
import org.jboss.ejb3.proxy.impl.remoting.ProxyRemotingUtils;
import org.jboss.ejb3.proxy.reflect.ReflectProxyFactory;
import org.jboss.ejb3.proxy.spi.factory.ProxyFactory;
import org.jboss.ejb3.singleton.deployer.jndi.Binding;
import org.jboss.ejb3.singleton.deployer.jndi.SingletonBeanJndiBinder;
import org.jboss.ejb3.singleton.proxy.impl.invocationhandler.SingletonBeanLocalInvocationHandler;
import org.jboss.ejb3.singleton.proxy.impl.invocationhandler.SingletonBeanRemoteInvocationHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.jboss.LocalBindingMetaData;
import org.jboss.metadata.ejb.jboss.RemoteBindingMetaData;
import org.jboss.metadata.ejb.jboss.jndi.resolver.impl.JNDIPolicyBasedJNDINameResolverFactory;
import org.jboss.metadata.ejb.jboss.jndi.resolver.spi.SessionBeanJNDINameResolver;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.DefaultJNDIBindingPolicyFactory;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DefaultJndiBindingPolicy;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;

/**
 * Deployer which is responsible for deploying (multiple) MC based 
 * JNDI binders for a singleton bean
 *  
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanJNDIBinderDeployer extends AbstractRealDeployerWithInput<EJBContainer>
      implements
         DeploymentVisitor<EJBContainer>
{

   /**
    * Client side AOP interceptors for the remote business interface(s) of the singleton bean
    */
   private final static String REMOTE_CLIENT_INTERCEPTOR_STACK_NAME = "SingletonSessionClientInterceptors";
   
   /**
    * Client side AOP interceptors for the local business interface(s) of the singleton bean
    */
   private static final String LOCAL_CLIENT_INTERCEPTOR_STACK_NAME = "LocalClientInterceptors";

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonBeanJNDIBinderDeployer.class);
   

   /**
    * 
    */
   public SingletonBeanJNDIBinderDeployer()
   {
      this.setDeploymentVisitor(this);
      this.setAllInputs(true);
      this.setInput(EJBContainer.class);
      this.setComponentsOnly(true);

      // we output the jndi binders as a MC bean
      addOutput(BeanMetaData.class);
   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor#deploy(org.jboss.deployers.structure.spi.DeploymentUnit, java.lang.Object)
    */
   @Override
   public void deploy(DeploymentUnit unit, EJBContainer container) throws DeploymentException
   {
      JBossEnterpriseBeanMetaData enterpriseBean = container.getXml();
      if (!enterpriseBean.isSession())
      {
         return;
      }
      JBossSessionBeanMetaData sessionBean = (JBossSessionBeanMetaData) enterpriseBean;
      if (sessionBean instanceof JBossSessionBean31MetaData == false)
      {
         return;
      }
      JBossSessionBean31MetaData sessionBean31 = (JBossSessionBean31MetaData) sessionBean;
      if (!sessionBean31.isSingleton())
      {
         return;
      }
      process(unit, sessionBean31, container);

   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor#getVisitorType()
    */
   @Override
   public Class<EJBContainer> getVisitorType()
   {
      return EJBContainer.class;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void undeploy(DeploymentUnit unit, EJBContainer deployment)
   {
      // TODO Auto-generated method stub

   }

   /**
    * Process the container and create appropriate JNDI binders for binding
    * the remote and local proxies of a singleton bean
    * 
    * @param unit The deployment unit being processed
    * @param sessionBean The bean metadata
    * @param container The container being processed
    */
   private void process(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean, EJBContainer container)
   {

      List<Binding> bindings = new ArrayList<Binding>();
      // process remote business interfaces
      bindings.addAll(processRemoteBusinessInterfaces(unit, sessionBean, container));
      // process local business interfaces
      bindings.addAll(processLocalBusinessInterfaces(unit, sessionBean, container));
      
      Context initCtx;
      try
      {
         initCtx = new InitialContext();
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }
      
      SingletonBeanJndiBinder jndiBinder = new SingletonBeanJndiBinder(initCtx, bindings);
      // attach the jndi binder as BMD to the unit
      this.createAndAttachJNDIBinerBeanMetaData(unit, jndiBinder, container);

      // just print out the bindings
      log.info("Binding the following entries in JNDI for singleton bean: " + container.getEjbName());
      log.info("\n\n");
      for (Binding binding :bindings)
      {
         log.info(binding);
         log.info("\n");
      }
   }

   /**
    * Responsible for creating jndi binders for each of the remote business interface views 
    * of a singleton bean
    * 
    * @param unit The deployment unit being processed
    * @param sessionBean The bean metadata
    * @param container The container being processed
    */
   private Collection<Binding> processRemoteBusinessInterfaces(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean,
         EJBContainer container)
   {
      BusinessRemotesMetaData businessRemotes = sessionBean.getBusinessRemotes();
      if (businessRemotes == null || businessRemotes.size() == 0)
      {
         return Collections.emptySet();
      }
      Set<Binding> bindings = new HashSet<Binding>();
      
      String defaultInvokerLocatorURL = ProxyRemotingUtils.getDefaultClientBinding();

      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBean, jndibindingPolicy);

      String containerRegistryKey = container.getObjectName().getCanonicalName();
      String containerGUID = Ejb3Registry.guid(container);

      ProxyFactory proxyFactory = new ReflectProxyFactory();
      Interceptor[] clientInterceptors = this.getRemoteClientInterceptors(container);

      Set<Class<?>> allRemoteinterfaces = new HashSet<Class<?>>();
      for (String businessRemote : businessRemotes)
      {
         try
         {
            Class<?> businessRemoteIntf = unit.getClassLoader().loadClass(businessRemote);
            allRemoteinterfaces.add(businessRemoteIntf);

            InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(containerRegistryKey, containerGUID,
                  defaultInvokerLocatorURL, clientInterceptors, businessRemote);

            // time to create a proxy
            Object proxy = proxyFactory.createProxy(new Class<?>[]
            {businessRemoteIntf}, invocationHandler);

            // bind to jndi

            String jndiName = jndiNameResolver.resolveJNDIName(sessionBean, businessRemote);

            Binding binding = new Binding(jndiName, proxy, "EJB3.x Remote Business Interface");
            bindings.add(binding);

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

         InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(containerRegistryKey, containerGUID,
               defaultInvokerLocatorURL, clientInterceptors);
         String defaultRemoteJNDIName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(sessionBean);
         Object proxy = proxyFactory.createProxy(allRemoteinterfaces.toArray(new Class<?>[allRemoteinterfaces.size()]),
               invocationHandler);
         Binding binding = new Binding(defaultRemoteJNDIName, proxy, "EJB3.x Default Remote Business Interface");
         bindings.add(binding);

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
            String invokerLocatorURL = getClientBindURL(remoteBinding);
            InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(containerRegistryKey, containerGUID,
                  invokerLocatorURL, clientInterceptors);
            Object proxy = proxyFactory.createProxy(allRemoteinterfaces
                  .toArray(new Class<?>[allRemoteinterfaces.size()]), invocationHandler);
            Binding binding = new Binding(jndiName, proxy, "EJB3.x Default Remote Business Interface");
            bindings.add(binding);
         }

      }
      return bindings;
   }

   /**
    * Responsible for creating jndi binders for each of the local business interface views 
    * of a singleton bean
    * 
    * @param unit The deployment unit being processed
    * @param sessionBean The bean metadata
    * @param container The container being processed
    */
   private Collection<Binding> processLocalBusinessInterfaces(DeploymentUnit unit, JBossSessionBean31MetaData sessionBean,
         EJBContainer container)
   {
      BusinessLocalsMetaData businessLocals = sessionBean.getBusinessLocals();
      if (businessLocals == null || businessLocals.size() == 0)
      {
         return Collections.emptySet();
      }
      
      Set<Binding> bindings = new HashSet<Binding>();
      
      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBean, jndibindingPolicy);

      // TODO: Rethink
      String containerRegistryKey = container.getObjectName().getCanonicalName();
      String containerGUID = Ejb3Registry.guid(container);

      ProxyFactory proxyFactory = new ReflectProxyFactory();

      Set<Class<?>> allLocalinterfaces = new HashSet<Class<?>>();
      // get the client side AOP interceptor(s) for local business interface of the singleton bean 
      Interceptor[] clientInterceptors = this.getLocalClientInterceptors(container);

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

         InvocationHandler invocationHandler = new SingletonBeanLocalInvocationHandler(containerRegistryKey,
               businessLocal, clientInterceptors);

         // time to create a proxy
         Object proxy = proxyFactory.createProxy(new Class<?>[]
         {businessLocalIntf}, invocationHandler);

         // bind to jndi

         String jndiName = jndiNameResolver.resolveJNDIName(sessionBean, businessLocal);

         Binding binding = new Binding(jndiName, proxy, "EJB3.x Local Business Interface");
         bindings.add(binding);

      }

      List<LocalBindingMetaData> localBindings = sessionBean.getLocalBindings();
      if (localBindings == null || localBindings.isEmpty())
      {
         InvocationHandler invocationHandler = new SingletonBeanLocalInvocationHandler(containerRegistryKey, containerGUID, clientInterceptors);
         String defaultBusinessLocalJNDIName = jndiNameResolver.resolveLocalBusinessDefaultJNDIName(sessionBean);
         Object proxy = proxyFactory.createProxy(allLocalinterfaces.toArray(new Class<?>[allLocalinterfaces.size()]),
               invocationHandler);
         Binding binding = new Binding(defaultBusinessLocalJNDIName, proxy, "EJB3.x Default Local Business Interface");
         bindings.add(binding);
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
            InvocationHandler invocationHandler = new SingletonBeanLocalInvocationHandler(containerRegistryKey, containerGUID, clientInterceptors);
            Object proxy = proxyFactory.createProxy(
                  allLocalinterfaces.toArray(new Class<?>[allLocalinterfaces.size()]), invocationHandler);
            Binding binding = new Binding(jndiName, proxy, "EJB3.x Default Local Business Interface");
            bindings.add(binding);

         }

      }
      return bindings;
   }

   /**
    * Returns the appropriate client bind url from a {@link RemoteBindingMetaData} 
    * @param remoteBinding
    * @return
    */
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
         generatedClientBindURL = ProxyRemotingUtils.getClientBinding(invokerName);
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
      return bindURL == null ? ProxyRemotingUtils.getDefaultClientBinding() : bindURL;
   }

   /**
    * Returns the client side interceptors for the remote business interface of the bean
    * represented by the <code>container</code>
    * 
    * @param container The {@link EJBContainer}
    * @return
    */
   private Interceptor[] getRemoteClientInterceptors(EJBContainer container)
   {
      // Obtain interceptors by stack name via Aspect Manager
      AspectManager manager = AspectManager.instance();
      AdviceStack stack = manager.getAdviceStack(REMOTE_CLIENT_INTERCEPTOR_STACK_NAME);

      if (stack == null)
      {
         throw new RuntimeException("Could not find Advice Stack with name: " + REMOTE_CLIENT_INTERCEPTOR_STACK_NAME);
      }
      Interceptor[] interceptors = stack.createInterceptors(container.getAdvisor(), null);
      return interceptors;
   }
   
   
   /**
    * Returns the client side interceptors for the local business interface of the bean
    * represented by the <code>container</code>
    * 
    * @param container The {@link EJBContainer}
    * @return
    */
   private Interceptor[] getLocalClientInterceptors(EJBContainer container)
   {
      // Obtain interceptors by stack name via Aspect Manager
      AspectManager manager = AspectManager.instance();
      AdviceStack stack = manager.getAdviceStack(LOCAL_CLIENT_INTERCEPTOR_STACK_NAME);

      if (stack == null)
      {
         throw new RuntimeException("Could not find Advice Stack with name: " + LOCAL_CLIENT_INTERCEPTOR_STACK_NAME);
      }
      Interceptor[] interceptors = stack.createInterceptors(container.getAdvisor(), null);
      return interceptors;
   }


   private void createAndAttachJNDIBinerBeanMetaData(DeploymentUnit unit, SingletonBeanJndiBinder jndiBinder, EJBContainer container)
   {
      String containerName = container.getXml().getContainerName();
      String binderName = containerName + ",type=singleton-bean-jndi-binder";
      
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(binderName, jndiBinder.getClass().getName());
      builder.setConstructorValue(jndiBinder);

      // the proxies/invocation handlers for singleton do NOT push the container
      // states. So let's just depend on a START state container, so that when the 
      // jndi binder binds the proxies, they will be ready to use (i.e. will be sure that
      // the container is available for invocation).
      builder.addDemand(containerName, ControllerState.START, ControllerState.START, null);
      for (Binding binding : jndiBinder.getBindings())
      {
         builder.addSupply("jndi:" + binding.getJndiName());
      }
      if (unit.isComponent())
      {
         // Attach it to parent since we are processing a component DU and BeanMetaDataDeployer doesn't
         // pick up BeanMetaData from component DU
         unit.getParent().addAttachment(BeanMetaData.class + ":" + binderName, builder.getBeanMetaData());
      }
      else
      {
         unit.addAttachment(BeanMetaData.class + ":" + binderName, builder.getBeanMetaData());
      }
   }
}
