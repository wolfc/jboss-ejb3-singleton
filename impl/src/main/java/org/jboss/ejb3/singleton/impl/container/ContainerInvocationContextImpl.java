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
package org.jboss.ejb3.singleton.impl.container;

import java.lang.reflect.Method;

import org.jboss.ejb3.container.spi.ContainerInvocationContext;

/**
 * ContainerInvocationContextImpl
 *
 * TODO: This needs to be in a better place like a common container impl
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ContainerInvocationContextImpl implements ContainerInvocationContext
{

   private Method method;
   
   private Object[] args;
   
   public ContainerInvocationContextImpl(Method method, Object[] args)
   {
      this.method = method;
      this.args = args;
   }
   
   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocationContext#getArgs()
    */
   @Override
   public Object[] getArgs()
   {
      return this.args;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocationContext#getMethod()
    */
   @Override
   public Method getMethod()
   {
      return this.method;
   }

   
}
