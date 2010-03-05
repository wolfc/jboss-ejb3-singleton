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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.jboss.ejb3.container.spi.remote.RemotingContainer;

/**
 * SingletonBeanRemoteInvocationHandler
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanRemoteInvocationHandler implements InvocationHandler, Serializable
{

   private RemotingContainer remotingContainer;
   
   private Class<?> businessInterface;
   
   public SingletonBeanRemoteInvocationHandler(RemotingContainer remotingContainer)
   {
      this.remotingContainer = remotingContainer;
   }
   
   public SingletonBeanRemoteInvocationHandler(RemotingContainer remotingContainer, Class<?> businessInterface)
   {
      this(remotingContainer);
      this.businessInterface = businessInterface;
   }
   
   /**
    * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
    */
   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      return this.remotingContainer.invoke(null, method, args, this.businessInterface);
   }

}
