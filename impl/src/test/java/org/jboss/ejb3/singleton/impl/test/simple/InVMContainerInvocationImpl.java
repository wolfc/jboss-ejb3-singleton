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
package org.jboss.ejb3.singleton.impl.test.simple;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jboss.ejb3.container.spi.ContainerInvocation;

/**
 * ContainerInvocationContextImpl
 *
 * TODO: This needs to be in a better place like a common container impl
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class InVMContainerInvocationImpl implements ContainerInvocation
{

   private Method method;
   
   private Object[] args;
   
   private Serializable sessionId;
   
   private Class<?> businessInterface;
   
   public InVMContainerInvocationImpl(Method method, Object[] args)
   {
      this.method = method;
      this.args = args;
   }
   
   public InVMContainerInvocationImpl(Method method, Object[] args, Serializable sessionId, Class<?> businessInterface)
   {
      this(method,args,sessionId);
      this.businessInterface = businessInterface;
   }
   
   public InVMContainerInvocationImpl(Method method, Object[] args, Serializable sessionId)
   {
      this.method = method;
      this.args = args;
      this.sessionId = sessionId;
   }
   
   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getArgs()
    */
   @Override
   public Object[] getArgs()
   {
      return this.args;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getMethod()
    */
   @Override
   public Method getMethod()
   {
      return this.method;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getSessionId()
    */
   @Override
   public Serializable getSessionId()
   {
      return this.sessionId;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getInvokedBusinessInterface()
    */
   @Override
   public Class<?> getInvokedBusinessInterface()
   {
      return this.businessInterface;
   }

   
}
