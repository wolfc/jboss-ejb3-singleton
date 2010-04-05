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

import org.jboss.dependency.spi.Controller;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.ejb3.container.spi.deployment.EJB3Deployment;
import org.jboss.ejb3.singleton.impl.deployment.EJB3DeploymentImpl;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * A deployer responsible for creating a {@link EJB3Deployment} and deploying
 * it as a MC bean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJB3DeploymentDeployer extends AbstractDeployer
{
   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(EJB3DeploymentDeployer.class);

   private Controller kernelController;

   /**
    * Constructs a {@link SingletonContainerDeployer} for
    * processing singleton beans
    */
   public EJB3DeploymentDeployer()
   {
      // Set the Stage to post-CL
      this.setStage(DeploymentStages.POST_CLASSLOADER);

      this.setInput(JBossMetaData.class);
      // ordering
      this.addInput(AttachmentNames.PROCESSED_METADATA);

      this.addOutput(Ejb3Deployment.class);
      // also add the new SPI based EJB3Deployment as output
      this.addOutput(EJB3Deployment.class);

   }

   /**
    * Processes EJB3.x {@link JBossMetaData} and creates a {@link EJB3Deployment} corresponding to this unit 
    *   
    * @see org.jboss.deployers.spi.deployer.Deployer#deploy(org.jboss.deployers.structure.spi.DeploymentUnit)
    */
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if ((unit instanceof VFSDeploymentUnit) == false)
      {
         return;
      }
      
      // get metadata
      JBossMetaData metadata = unit.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class);
      // we only process EJB3.x
      if (metadata == null || !metadata.isEJB3x())
      {
         return;
      }

      EJB3Deployment ejb3Deployment = new EJB3DeploymentImpl(unit.getSimpleName(), unit, metadata);

      // add as an attachment
      unit.addAttachment(EJB3Deployment.class, ejb3Deployment);

   }

}
