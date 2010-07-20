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
import org.jboss.ejb3.proxy.impl.handler.session.SessionLocalProxyInvocationHandler;

/**
 * Responsible for handling invocations on local business interface proxies of 
 * singleton bean 
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanLocalInvocationHandler extends SessionLocalProxyInvocationHandler
{

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /**
    * Constructs a {@link SingletonBeanLocalInvocationHandler} for a registered container
    * 
    * @param containerRegistryName The name by which the container is registered
    * @param containerGUID The globally unique id of the container
    */
   public SingletonBeanLocalInvocationHandler(String containerRegistryName, String containerGUID)
   {
      super(containerRegistryName, containerGUID, null);
   }

   /**
    * Constructs a {@link SingletonBeanLocalInvocationHandler} for a registered container and a 
    * business interface
    * 
    * @param containerRegistryName The name by which the container is registered
    * @param containerGUID The globally unique id of the container
    * @param businessInterface The business interface on which the invocation is being made
    */
   public SingletonBeanLocalInvocationHandler(String containerRegistryName, String containerGUID, String businessInterfaceType)
   {
      super(containerRegistryName, containerGUID, null, businessInterfaceType);
   }
   
   
   /**
    * Constructs a {@link SingletonBeanLocalInvocationHandler} for a registered container
    * 
    * @param containerRegistryName The name by which the container is registered
    * @param containerGUID The globally unique id of the container
    * @param clientInterceptors The client side AOP interceptors that will be used when this 
    *                           {@link SingletonBeanLocalInvocationHandler} is invoked. This can be null 
    */
   public SingletonBeanLocalInvocationHandler(String containerRegistryName, String containerGUID, Interceptor[] clientInterceptors)
   {
      super(containerRegistryName, containerGUID, clientInterceptors);
   }

   /**
    * Constructs a {@link SingletonBeanLocalInvocationHandler} for a registered container and a 
    * business interface
    * 
    * @param containerRegistryName The name by which the container is registered
    * @param containerGUID The globally unique id of the container
    * @param businessInterface The business interface on which the invocation is being made
    * @param clientInterceptors The client side AOP interceptors that will be used when this 
    *                           {@link SingletonBeanLocalInvocationHandler} is invoked. This can be null
    */
   public SingletonBeanLocalInvocationHandler(String containerRegistryName, String containerGUID, String businessInterfaceType, Interceptor[] interceptors)
   {
      super(containerRegistryName, containerGUID, interceptors, businessInterfaceType);
   }

   
}
