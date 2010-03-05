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
package org.jboss.ejb3.singleton.impl.remoting;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;

/**
 * A serializable implementation of {@link ContainerInvocation}
 * <p>
 *  {@link RemotableContainerInvocation} can be used for passing the 
 *  container invocation from a remote client to the server side {@link EJBContainer}. 
 * </p>
 *
 * TODO: Might have to be in some common location instead of singleton component
 *  
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class RemotableContainerInvocation implements ContainerInvocation, Serializable
{

   /**
    * Session id
    */
   private Serializable sessionId;

   /**
    * Arguments to be passed to the method
    */
   private Object[] args;

   /**
    * Fully qualified class name of business interface
    */
   private String businessInterfaceClassName;

   /**
    * The method being invoked
    */
   private SerializableMethod serializableMethod;

   /**
    * The business interface on which the method is being invoked
    */
   private transient Class<?> businessInterface;

   /**
    * Creates a {@link RemotableContainerInvocation} for the passed arguments.
    * 
    * @param sessionId Session id (can be null)
    * @param method The method being invoked.
    * @param args The arguments to be passed to the method being invoked
    * @param businessInterface The fully qualified classname of the business interface on which 
    *                   the method is being invoked 
    */
   public RemotableContainerInvocation(Serializable sessionId, Method method, Object[] args,
         String businessIntfClassName)
   {
      this.sessionId = sessionId;
      this.serializableMethod = new SerializableMethod(method);
      this.args = args;
      this.businessInterfaceClassName = businessIntfClassName;
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
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getInvokedBusinessInterface()
    */
   @Override
   public Class<?> getInvokedBusinessInterface()
   {
      if (this.businessInterfaceClassName == null)
      {
         return null;
      }

      if (this.businessInterface == null)
      {
         try
         {
            this.businessInterface = Thread.currentThread().getContextClassLoader().loadClass(
                  this.businessInterfaceClassName);
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException("Could not load business interface class " + this.businessInterfaceClassName,
                  cnfe);
         }
      }
      return this.businessInterface;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getMethod()
    */
   @Override
   public Method getMethod()
   {
      return this.serializableMethod.toMethod();
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getSessionId()
    */
   @Override
   public Serializable getSessionId()
   {
      return this.sessionId;
   }

}
