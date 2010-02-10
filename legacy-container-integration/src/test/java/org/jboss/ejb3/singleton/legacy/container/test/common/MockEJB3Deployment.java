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
package org.jboss.ejb3.singleton.legacy.container.test.common;

import javax.security.jacc.PolicyConfiguration;

import org.jboss.deployers.structure.spi.helpers.AbstractDeploymentUnit;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.DeploymentScope;
import org.jboss.ejb3.DeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.deployers.JBoss5DependencyPolicy;
import org.jboss.ejb3.javaee.JavaEEComponent;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * MockEJB3Deployment
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class MockEJB3Deployment extends Ejb3Deployment
{
   public MockEJB3Deployment()
   {
      this(new AbstractDeploymentUnit(), new MockDeploymentUnit(), null, null);
   }

   /**
    * @param deploymentUnit
    * @param unit
    * @param deploymentScope
    * @param metaData
    */
   public MockEJB3Deployment(org.jboss.deployers.structure.spi.DeploymentUnit deploymentUnit, DeploymentUnit unit,
         DeploymentScope deploymentScope, JBossMetaData metaData)
   {
      super(deploymentUnit, unit, deploymentScope, metaData);
      // TODO Auto-generated constructor stub
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Ejb3Deployment#createDependencyPolicy(org.jboss.ejb3.javaee.JavaEEComponent)
    */
   @Override
   public DependencyPolicy createDependencyPolicy(JavaEEComponent component)
   {
      return new JBoss5DependencyPolicy(component);
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Ejb3Deployment#createPolicyConfiguration()
    */
   @Override
   protected PolicyConfiguration createPolicyConfiguration() throws Exception
   {
      // TODO Auto-generated method stub
      //return null;
      throw new RuntimeException("NYI");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Ejb3Deployment#putJaccInService(javax.security.jacc.PolicyConfiguration, org.jboss.ejb3.DeploymentUnit)
    */
   @Override
   protected void putJaccInService(PolicyConfiguration pc, DeploymentUnit unit)
   {
      // TODO Auto-generated method stub
      //
      throw new RuntimeException("NYI");
   }

}
