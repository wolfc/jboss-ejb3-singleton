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
package org.jboss.ejb3.singleton.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.ejb3.container.spi.EJBContainer;

/**
 * ContainerRegistry
 *
 * TODO: This is WIP and needs better javadoc. But before that, i need
 * to figure out the scope of this registry and how/whether it relates to
 * Ejb3Registry
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ContainerRegistry
{

   public static final ContainerRegistry INSTANCE = new ContainerRegistry();

   private Map<String, EJBContainer> containers = new ConcurrentHashMap<String, EJBContainer>();

   private ContainerRegistry()
   {

   }

   
   
   public void registerContainer(String containerRegistryKey, EJBContainer container)
   {
      this.containers.put(containerRegistryKey, container);
   }

   public void unregisterContainer(String containerRegistryKey)
   {
      if (!this.containers.containsKey(containerRegistryKey))
      {
         throw new IllegalArgumentException("Container with key " + containerRegistryKey
               + " was not registered, so cannot be unregistered");
      }
      this.containers.remove(containerRegistryKey);
   }

   public EJBContainer getContainer(String containerRegistryKey)
   {
      if (!this.containers.containsKey(containerRegistryKey))
      {
         throw new IllegalArgumentException("Container with key " + containerRegistryKey + " was not registered");
      }
      return this.containers.get(containerRegistryKey);
   }

}
