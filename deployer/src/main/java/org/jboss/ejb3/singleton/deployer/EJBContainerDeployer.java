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
package org.jboss.ejb3.singleton.deployer;

import java.util.Collection;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.DemandMetaData;
import org.jboss.beans.metadata.spi.DependencyMetaData;
import org.jboss.beans.metadata.spi.SupplyMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.MCDependencyPolicy;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.logging.Logger;

/**
 * EJBContainerDeployer
 * 
 * TODO: Remove this - not used currently
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJBContainerDeployer extends AbstractSimpleRealDeployer<EJB3Deployment>
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(EJBContainerDeployer.class);

   /**
    * @param input
    */
   public EJBContainerDeployer()
   {
      super(EJB3Deployment.class);
      
      
      addOutput(BeanMetaData.class);
   }

   /**
    * @see org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer#deploy(org.jboss.deployers.structure.spi.DeploymentUnit, java.lang.Object)
    */
   @Override
   public void deploy(DeploymentUnit unit, EJB3Deployment ejb3Deployment) throws DeploymentException
   {
      Collection<EJBContainer> ejbContainers = ejb3Deployment.getEJBContainers();
      for (EJBContainer ejbContainer : ejbContainers)
      {
         // create dependencies for the container
         if (ejbContainer instanceof org.jboss.ejb3.EJBContainer)
         {
            org.jboss.ejb3.EJBContainer legacyContainer = (org.jboss.ejb3.EJBContainer) ejbContainer;
            legacyContainer.instantiated();
            legacyContainer.processMetadata();

            DependencyPolicy dependencyPolicy = legacyContainer.getDependencyPolicy();
            if (dependencyPolicy instanceof MCDependencyPolicy)
            {
               // deploy the container as MC bean (by attaching to DU)
               this.createContainerWithDependencies(unit, ejbContainer, (MCDependencyPolicy) dependencyPolicy);
            }

         }

      }

   }

   private void createContainerWithDependencies(DeploymentUnit unit, EJBContainer container,
         MCDependencyPolicy mcDependencyPolicy) throws DeploymentException
   {
      String containerMCBeanName = container.getMetaData().getContainerName();
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(containerMCBeanName, container.getClass()
            .getName());
      builder.setConstructorValue(container);

      logger.info("installing bean: " + containerMCBeanName);
      logger.info("  with dependencies:");
      if (mcDependencyPolicy.getDependencies() != null)
      {
         for (DependencyMetaData dependency : mcDependencyPolicy.getDependencies())
         {
            builder.addDependency(dependency.getDependency());
            logger.info("\t" + dependency.getDependency());
         }
      }
      logger.info("  and demands:");
      if (mcDependencyPolicy.getDemands() != null)
      {
         for (DemandMetaData demand : mcDependencyPolicy.getDemands())
         {
            builder.addDemand(demand.getDemand());
            logger.info("\t" + demand.getDemand());
         }
      }

      logger.info("  and supplies:");
      if (mcDependencyPolicy.getSupplies() != null)
      {
         for (SupplyMetaData supply : mcDependencyPolicy.getSupplies())
         {
            builder.addSupply(supply.getSupply());
            logger.info("\t" + supply.getSupply());
         }
      }

      // Add the singleton container MC bean as an attachment
      // TODO: This does not work for component only deployers. For some reason, the BeanMetadata attachment
      // has to be attached to the "parent"
      //unit.addAttachment(BeanMetaData.class + ":" + singletonContainerMCBeanName, builder.getBeanMetaData());
      //      if (unit.getParent() == null)
      //      {
      //         throw new DeploymentException("DeploymentUnit " + unit + " does not have a parent");
      //      }
      unit.addAttachment(BeanMetaData.class + ":" + containerMCBeanName, builder.getBeanMetaData());
   }

}
