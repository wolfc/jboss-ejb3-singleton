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
package org.jboss.ejb3.singleton.proxy.impl;

import java.lang.reflect.InvocationHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.remote.RemotingContainer;
import org.jboss.ejb3.proxy.spi.ProxyFactory;
import org.jboss.ejb3.singleton.impl.remoting.JBossRemotingContainer;
import org.jboss.ejb3.singleton.spi.ContainerRegistry;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.jboss.jndi.resolver.impl.JNDIPolicyBasedJNDINameResolverFactory;
import org.jboss.metadata.ejb.jboss.jndi.resolver.spi.SessionBeanJNDINameResolver;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.DefaultJNDIBindingPolicyFactory;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DefaultJndiBindingPolicy;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.util.naming.Util;

/**
 * SingletonBeanJNDIRegistrar
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanRemoteJNDIBinder
{

   private EJBContainer container;

   private JBossSessionBeanMetaData sessionBeanMetadata;

   public SingletonBeanRemoteJNDIBinder(EJBContainer container)
   {
      this.container = container;
      JBossEnterpriseBeanMetaData enterpriseBean = this.container.getMetaData();
      if (!enterpriseBean.isSession())
      {
         throw new IllegalStateException("Bean " + enterpriseBean.getEjbName() + " is not a session bean");
      }
      //      if (!sessionbean.isSingleton())
      //      {
      //         throw new IllegalStateException("Bean " + sessionbean.getEjbName() + " is not a singleton bean");
      //      }
      this.sessionBeanMetadata = (JBossSessionBeanMetaData) enterpriseBean;

   }

   /**
    * Binds the no-interface view of the bean <code>beanClass</code> to the JNDI,
    * at the provided <code>jndiCtx</code> context.
    * 
    * @param jndiCtx The jndi context to which the no-interface view has to be bound
    * @param beanClass The EJB class
    * @param beanMetaData The metadata of the bean
    * 
    * @return Returns the jndi-name where the no-interface view has been bound
    * @throws NamingException If any exception while binding to JNDI
    * @throws IllegalStateException If a no-interface view is not applicable for this bean
    */
   public void bind(Context jndiCtx) throws NamingException, IllegalStateException
   {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();// get from container
      BusinessRemotesMetaData businessRemotes = this.sessionBeanMetadata.getBusinessRemotes();
      if (businessRemotes == null || businessRemotes.size() == 0)
      {
         throw new IllegalStateException("No business remotes found for bean " + sessionBeanMetadata.getEjbName());
      }
      Set<Class<?>> interfaces = new HashSet<Class<?>>();
      for (String businessRemote : businessRemotes)
      {
         try
         {
            Class<?> businessRemoteIntf = cl.loadClass(businessRemote);
            interfaces.add(businessRemoteIntf);
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException("Could not load business remote interface " + businessRemote, cnfe);
         }

      }
      // TODO : Move this out of here and generate the key through some SPI
      String containerRegistryKey = this.sessionBeanMetadata.getEjbName() + "/" + UUID.randomUUID();
      // TODO: Move this out of here
      // register the container in a registry

      ContainerRegistry.INSTANCE.registerContainer(containerRegistryKey, container);
      // TODO: Obviously should NOT be hardcoded and should not even be here probably
      final String DEFAULT_REMOTING_URL = "socket://0.0.0.0:3873";
      // create a remoting container
      // TODO: Not the responsibility of a jndibinder, so move this out of here
      RemotingContainer remotingContainer = new JBossRemotingContainer(containerRegistryKey, DEFAULT_REMOTING_URL);
      // create an invocation handler
      InvocationHandler invocationHandler = new SingletonBeanRemoteInvocationHandler(remotingContainer);

      // time to create a proxy
      ProxyFactory proxyFactory = new SingletonProxyFactory();
      Object proxy = proxyFactory.createProxy(interfaces.toArray(new Class<?>[interfaces.size()]), invocationHandler);

      // bind to jndi
      JBossSessionBeanMetaData sessionBeanMetadata = (JBossSessionBeanMetaData) container.getMetaData();
      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            sessionBeanMetadata, jndibindingPolicy);
      String defaultRemoteJNDIName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(sessionBeanMetadata);
      Util.bind(jndiCtx, defaultRemoteJNDIName, proxy);

      
   }

   /**
    * Unbind the no-interface view of the bean <code>beanClass</code> from the JNDI
    * at the provided <code>jndiCtx</code> context.
    * 
    * @param jndiCtx The jndi context from where the no-interface view has to be unbound
    * @param beanClass The EJB class
    * @param beanMetaData The metadata of the bean
    * 
    * @throws NamingException If any exception while unbinding from JNDI
    * @throws IllegalStateException If a no-interface view is not applicable for this bean 
    */
   public void unbind(Context jndiCtx) throws NamingException, IllegalStateException
   {
      DefaultJndiBindingPolicy jndibindingPolicy = DefaultJNDIBindingPolicyFactory.getDefaultJNDIBindingPolicy();
      SessionBeanJNDINameResolver jndiNameResolver = JNDIPolicyBasedJNDINameResolverFactory.getJNDINameResolver(
            this.sessionBeanMetadata, jndibindingPolicy);
      String defaultRemoteJNDIName = jndiNameResolver.resolveRemoteBusinessDefaultJNDIName(this.sessionBeanMetadata);
      Util.unbind(jndiCtx, defaultRemoteJNDIName);
   }

}
