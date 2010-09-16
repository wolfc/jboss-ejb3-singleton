/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.beans.metadata.api.annotations.Install;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

/**
 * Responsible for creating the instance of a startup singleton bean,
 * when the corresponding {@link EJBContainer} reaches {@link ControllerState#INSTALLED}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class StartupSingletonInitiator
{

   /** Logger */
   private static Logger logger = Logger.getLogger(StartupSingletonInitiator.class);
   
   /**
    * Creates a instance of the EJB corresponding to the passed {@link EJBContainer},
    * if the EJB represents a startup singleton bean.
    * 
    * @param container The {@link EJBContainer} which reached the {@link ControllerState#INSTALLED} state
    */
   @Install
   public void onInstall(EJBContainer container)
   {
      // get the metadata
      JBossEnterpriseBeanMetaData metadata = container.getMetaData();
      // if it's not a session bean, then we don't have anything to do
      if (!metadata.isSession() || !(metadata instanceof JBossSessionBean31MetaData))
      {
         return;
      }
      JBossSessionBean31MetaData sessionBean31 = (JBossSessionBean31MetaData) metadata;
      // if it's not a @Startup @Singleton bean, then we don't have anything to do
      if(!sessionBean31.isSingleton() || !sessionBean31.isInitOnStartup())
      {
         return;
      }
      // create the instance
      container.getBeanInstanceManager().create();
      
      logger.debug("Created an instance of @Startup @Singleton bean: " + sessionBean31.getEjbName());
   }
}
