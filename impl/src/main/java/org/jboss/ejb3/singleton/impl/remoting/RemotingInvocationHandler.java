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

import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.singleton.spi.ContainerRegistry;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * Implementation of {@link ServerInvocationHandler} which is responsible
 * for handling invocations on the EJB3 remoting connector for the "SIMPLE"
 * subsystem.
 * 
 * <p>
 *  This {@link RemotingInvocationHandler} passes on the invocation to the 
 *  {@link EJBContainer}
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class RemotingInvocationHandler implements ServerInvocationHandler
{

   /**
    * @see org.jboss.remoting.ServerInvocationHandler#addListener(org.jboss.remoting.callback.InvokerCallbackHandler)
    */
   @Override
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      // we don't have anything specific to do 

   }

   /**
    * Processes the incoming {@link InvocationRequest} and passes on the control to
    * {@link EJBContainer#invoke(ContainerInvocation)} method. 
    * <p>
    *   This method expects the <code>invocation</code> to contain the container registry key
    *   as the param of the invocation request (obtained through {@link InvocationRequest#getParameter()}.
    *   It also expects the <code>invocation</code> to contain the {@link ContainerInvocation} as the 
    *   payload with the key "ContainerInvocation". 
    * </p>
    * 
    * @see org.jboss.remoting.ServerInvocationHandler#invoke(org.jboss.remoting.InvocationRequest)
    */
   @Override
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      Object param = invocation.getParameter();

      if (!(param instanceof String))
      {
         throw new IllegalArgumentException("Unexpected invocation param " + param + " from invocation " + invocation);
      }
      String containerRegistryKey = (String) param;
      EJBContainer container = ContainerRegistry.INSTANCE.getContainer(containerRegistryKey);
      Map payload = invocation.getRequestPayload();
      // TODO: Don't hard code the "ContainerInvocation" payload key
      ContainerInvocation containerInvocation = (ContainerInvocation) payload.get("ContainerInvocation");
      return container.invoke(containerInvocation);
   }

   /**
    * @see org.jboss.remoting.ServerInvocationHandler#removeListener(org.jboss.remoting.callback.InvokerCallbackHandler)
    */
   @Override
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      // we don't have anything specific to do

   }

   /**
    * @see org.jboss.remoting.ServerInvocationHandler#setInvoker(org.jboss.remoting.ServerInvoker)
    */
   @Override
   public void setInvoker(ServerInvoker invoker)
   {
      // we don't have anything specific to do

   }

   /**
    * @see org.jboss.remoting.ServerInvocationHandler#setMBeanServer(javax.management.MBeanServer)
    */
   @Override
   public void setMBeanServer(MBeanServer server)
   {
      // we don't have anything specific to do

   }

}
