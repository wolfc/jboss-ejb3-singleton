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
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.remote.RemotingContainer;
import org.jboss.ejb3.singleton.spi.ContainerRegistry;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * Implementation of {@link RemotingContainer} which is responsible for
 * handling container invocations on the client side and passing on the
 * control to the remote container.
 * 
 * <p>
 *  {@link JBossRemotingContainer} uses JBoss Remoting 2.x to interact with the
 *  remote container. It expects a "SIMPLE" subsystem to be present on the 
 *  JBoss Remoting connector through which it will interact with the remote container
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class JBossRemotingContainer implements RemotingContainer, Serializable
{

   /**
    * JBoss Remoting InvokerLocator URL
    */
   private String remotingURL;

   /**
    * The key of the remote {@link EJBContainer} with which this {@link RemotingContainer} will be interacting.
    * The {@link EJBContainer} is expected to be registered with this key, in the {@link ContainerRegistry}
    * on the remote server side. 
    */
   private String containerRegistryKey;

   /**
    * Creates a {@link JBossRemotingContainer} for a {@link EJBContainer} represented by the
    * <code>remoteContainerKey</code>
    * 
    * @param remoteContainerKey Key of the remote {@link EJBContainer} with which the container is registered
    *                           in {@link ContainerRegistry}
    * @param remotingURL The InvokerLocator url of the JBoss Remoting connector configuration                          
    */
   public JBossRemotingContainer(String remoteContainerKey, String remotingURL)
   {
      this.remotingURL = remotingURL;
      this.containerRegistryKey = remoteContainerKey;
   }

   /**
    * Creates a {@link ContainerInvocation} for the passed parameters and then 
    * invokes the {@link EJBContainer#invoke(ContainerInvocation)}.  
    * <p>
    *   The {@link EJBContainer} is identified by the <code>remoteContainerKey</code> which was passed
    *   to the constructor of this {@link JBossRemotingContainer} 
    * </p>
    * @see org.jboss.ejb3.container.spi.remote.RemotingContainer#invoke(java.io.Serializable, java.lang.reflect.Method, java.lang.Object[], java.lang.Class)
    */
   @Override
   public Object invoke(Serializable sessionId, Method method, Object[] args, Class<?> businessIntf)
   {
      InvokerLocator locator = null;
      try
      {
         locator = new InvokerLocator(this.remotingURL);
      }
      catch (MalformedURLException e)
      {
         throw new RuntimeException("Could not create " + InvokerLocator.class.getSimpleName() + " to url \""
               + this.remotingURL + "\"", e);
      }
      Client client = null;
      try
      {
         // we connect to "SIMPLE" subsystem
         client = new Client(locator, "SIMPLE");
         // create a container invocation
         String businessIntfClassName = businessIntf == null ? null : businessIntf.getName();
         ContainerInvocation containerInvocation = new RemotableContainerInvocation(sessionId, method, args, businessIntfClassName);
         Map payload = new HashMap();
         payload.put("ContainerInvocation", containerInvocation);

         client.connect();
         Object response = client.invoke(this.containerRegistryKey, payload);
         return response;
      }
      catch (Throwable t)
      {
         throw new RuntimeException(t);
      }
      finally
      {
         if (client != null)
         {
            client.disconnect();
         }
      }

   }

}
