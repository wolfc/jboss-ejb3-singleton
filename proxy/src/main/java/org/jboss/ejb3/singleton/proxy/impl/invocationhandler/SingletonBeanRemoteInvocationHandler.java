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
package org.jboss.ejb3.singleton.proxy.impl.invocationhandler;

import org.jboss.aop.advice.Interceptor;
import org.jboss.ejb3.proxy.impl.handler.session.SessionRemoteProxyInvocationHandler;

/**
 * Responsible for handling invocations on remote business interface proxies
 * of a singleton bean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanRemoteInvocationHandler extends SessionRemoteProxyInvocationHandler
{

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /**
    * (Remoting) Invoker locator URL
    */
   private String url;

   /**
    * Client side interceptors
    */
   private Interceptor[] clientInterceptors;

   /**
    * Constructs a {@link SingletonBeanRemoteInvocationHandler}
    *  
    * @param containerRegistryName The name by which the container is registered
    * @param locatorURL The Remoting invoker locator URL to be used to interact with the remote container
    * @param interceptors The client side interceptors to be used when an invocation is being handled
    */
   public SingletonBeanRemoteInvocationHandler(String containerRegistryName, String containerGUID, String locatorURL,
         Interceptor[] interceptors)
   {
      super(containerRegistryName, containerGUID, interceptors, null, locatorURL);
   }

   /**
    * Constructs a {@link SingletonBeanRemoteInvocationHandler}
    * 
    * @param containerRegistryName The name by which the container is registered
    * @param locatorURL The remoting invoker locator URL to be used to interact with the remote container
    * @param interceptors The client side interceptors to be used when an invocation is being handled
    * @param businessInterface The business interface corresponding to the proxy on which the invocation is being made
    */
   public SingletonBeanRemoteInvocationHandler(String containerRegistryName, String containerGUID, String locatorURL,
         Interceptor[] interceptors, String businessInterfaceType)
   {
      super(containerRegistryName, containerGUID, interceptors, businessInterfaceType, locatorURL);
   }

   
}
