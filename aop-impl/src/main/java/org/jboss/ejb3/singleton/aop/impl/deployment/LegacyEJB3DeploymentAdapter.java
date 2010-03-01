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
package org.jboss.ejb3.singleton.aop.impl.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.jacc.PolicyConfiguration;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.DeploymentScope;
import org.jboss.ejb3.DeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.ejb3.deployers.JBoss5DependencyPolicy;
import org.jboss.ejb3.javaee.JavaEEComponent;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * LegacyEJB3DeploymentAdapter
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class LegacyEJB3DeploymentAdapter extends Ejb3Deployment implements EJB3Deployment
{

   
   private PersistenceUnitDependencyResolver persistenceUnitResolver;
   
   private org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit;
   
   /**
    * @param deploymentUnit
    * @param unit
    * @param deploymentScope
    * @param metaData
    */
   public LegacyEJB3DeploymentAdapter(org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit,
         DeploymentUnit unit, DeploymentScope deploymentScope, JBossMetaData metaData)
   {
      super(deploymentUnit, unit, deploymentScope, metaData);
      this.deploymentUnit = deploymentUnit;
   }

   /**
    * EJB name --> EJB container map
    */
   private Map<String, EJBContainer> containers = new HashMap<String, EJBContainer>();
   
   /**
    * @see org.jboss.ejb3.Ejb3Deployment#createDependencyPolicy(org.jboss.ejb3.javaee.JavaEEComponent)
    */
   @Override
   public DependencyPolicy createDependencyPolicy(JavaEEComponent component)
   {
      return new JBoss5DependencyPolicy(component);
   }

   /**
    * @see org.jboss.ejb3.Ejb3Deployment#createPolicyConfiguration()
    */
   @Override
   protected PolicyConfiguration createPolicyConfiguration() throws Exception
   {
      throw new UnsupportedOperationException("NYI - createPolicyConfiguration");
   }

   /**
    * @see org.jboss.ejb3.Ejb3Deployment#putJaccInService(javax.security.jacc.PolicyConfiguration, org.jboss.ejb3.DeploymentUnit)
    */
   @Override
   protected void putJaccInService(PolicyConfiguration pc, DeploymentUnit unit)
   {
      throw new UnsupportedOperationException("NYI - putJaccInService");
      
   }

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
   
   @Inject
   public void setPersistenceUnitResolver(PersistenceUnitDependencyResolver puResolver)
   {
      this.persistenceUnitResolver = puResolver;
   }
   
   /**
    * @see org.jboss.ejb3.Ejb3Deployment#resolvePersistenceUnitSupplier(java.lang.String)
    */
   @Override
   protected String resolvePersistenceUnitSupplier(String persistenceUnitName)
   {
      if (this.persistenceUnitResolver == null)
      {
         return null;
      }
      return this.persistenceUnitResolver.resolvePersistenceUnitSupplier(this.deploymentUnit, persistenceUnitName);
   }

}
