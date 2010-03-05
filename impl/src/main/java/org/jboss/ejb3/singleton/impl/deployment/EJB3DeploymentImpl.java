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
package org.jboss.ejb3.singleton.impl.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * EJB3DeploymentImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJB3DeploymentImpl implements EJB3Deployment
{

   private String name;
   
   private DeploymentUnit deploymentUnit;

   private JBossMetaData jbossMetaData;

   /**
    * @param deploymentUnit
    * @param unit
    * @param deploymentScope
    * @param metaData
    */
   public EJB3DeploymentImpl(String name, DeploymentUnit deploymentUnit, JBossMetaData jbossMetaData)
   {
      this.name = name;
      this.deploymentUnit = deploymentUnit;
      this.jbossMetaData = jbossMetaData;
   }

   /**
    * EJB name --> EJB container map
    */
   private Map<String, EJBContainer> containers = new HashMap<String, EJBContainer>();

   /**
    * @see org.jboss.ejb3.container.spi.deployment.EJB3Deployment#addContainer(org.jboss.ejb3.container.spi.EJBContainer)
    */
   @Override
   public void addContainer(EJBContainer ejbContainer) throws IllegalArgumentException
   {
      String ejbName = ejbContainer.getEJBName();
      if (this.containers.containsKey(ejbName))
      {
         throw new IllegalArgumentException("Container for ejb named " + ejbName + " is already registered");
      }
      this.containers.put(ejbName, ejbContainer);
   }

   /**
    * @see org.jboss.ejb3.container.spi.deployment.EJB3Deployment#getEJBContainer(java.lang.String)
    */
   @Override
   public EJBContainer getEJBContainer(String ejbName)
   {
      return this.containers.get(ejbName);
   }

   /**
    * @see org.jboss.ejb3.container.spi.deployment.EJB3Deployment#getEJBContainers()
    */
   @Override
   public Collection<EJBContainer> getEJBContainers()
   {
      return Collections.unmodifiableCollection(this.containers.values());
   }

   /**
    * @see org.jboss.ejb3.container.spi.deployment.EJB3Deployment#removeEJBContainer(org.jboss.ejb3.container.spi.EJBContainer)
    */
   @Override
   public void removeEJBContainer(EJBContainer ejbContainer) throws IllegalArgumentException
   {
      String ejbName = ejbContainer.getEJBName();
      if (!this.containers.containsKey(ejbName))
      {
         throw new IllegalArgumentException("EJB container for ejb name " + ejbName + " is not registered");
      }
      this.containers.remove(ejbName);
   }

   /**
    * @see org.jboss.ejb3.container.spi.deployment.EJB3Deployment#getName()
    */
   @Override
   public String getName()
   {
      return this.name;
   }

}
