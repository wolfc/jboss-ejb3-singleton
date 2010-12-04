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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

/**
 * StartupSingletonInitiatorDeployer
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class StartupSingletonInitiatorDeployer extends AbstractDeployer
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(StartupSingletonInitiatorDeployer.class);
   
   public StartupSingletonInitiatorDeployer()
   {
      this.setStage(DeploymentStages.INSTALLED);
      this.setTopLevelOnly(true);
   }
   
   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      Boolean isStartupSingletonPresentInDU = unit.getAttachment(SingletonContainerDeployer.IS_STARTUP_SINGLETON_PRESENT_IN_DU, Boolean.class);
      if (isStartupSingletonPresentInDU == null || isStartupSingletonPresentInDU == false)
      {
         return;
      }
      
      Collection<DeploymentUnit> ejb31DeploymentUnits = this.getEJB31DeploymentUnits(unit);
      if (ejb31DeploymentUnits.isEmpty())
      {
         return;
      }
      for (DeploymentUnit ejb31DeploymentUnit : ejb31DeploymentUnits)
      {
         Collection<DeploymentUnit> enterpriseBeanComponentDUs = this.getEnterpriseBeanComponentUnits(ejb31DeploymentUnit);
         if (enterpriseBeanComponentDUs.isEmpty())
         {
            continue;
         }
         for (DeploymentUnit enterpriseBeanComponentDU : enterpriseBeanComponentDUs)
         {
            JBossEnterpriseBeanMetaData enterpriseBean = enterpriseBeanComponentDU.getAttachment(JBossEnterpriseBeanMetaData.class);
            this.deploy(enterpriseBeanComponentDU, enterpriseBean);
         }
         
      }
   }

   private void deploy(DeploymentUnit unit, JBossEnterpriseBeanMetaData enterpriseBean)
   {
      if (!this.isStartupSingletonBean(enterpriseBean))
      {
         if (logger.isTraceEnabled())
         {
            logger.trace("Bean: " + enterpriseBean.getEjbName() + " in unit " + unit + " is *not* a @Startup @Singleton");
         }
         return;
      }
      EJBContainer ejbContainer = unit.getAttachment(EJBContainer.class);
      if (ejbContainer == null)
      {
         logger.warn("Could not instantiate @Startup @Singleton bean: " + enterpriseBean.getEjbName() + " in unit " + unit + " due to missing EJBContainer");
         return;
      }
      ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(ejbContainer.getClassLoader());
      try
      {
         EJBInstanceManager instanceManager = ejbContainer.getBeanInstanceManager();
         if (instanceManager instanceof SingletonEJBInstanceManager)
         {
            ((SingletonEJBInstanceManager) instanceManager).get();
         }
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldCL);
      }
      logger.debug("Created an instance of @Startup @Singleton bean: " + enterpriseBean.getEjbName() + " in unit " + unit);
   }
   
   private Collection<DeploymentUnit> getEJB31DeploymentUnits(DeploymentUnit unit)
   {
      Collection<DeploymentUnit> ejb31DeploymentUnits = new ArrayList<DeploymentUnit>();
      JBossMetaData jbossMetaData = unit.getAttachment(JBossMetaData.class);
      if (jbossMetaData != null && jbossMetaData.isEJB31())
      {
         ejb31DeploymentUnits.add(unit);
      }
      List<DeploymentUnit> children = unit.getChildren();
      for (DeploymentUnit child : children)
      {
         jbossMetaData = child.getAttachment(JBossMetaData.class);
         if (jbossMetaData != null && jbossMetaData.isEJB31())
         {
            ejb31DeploymentUnits.add(child);
         }
      }
      return ejb31DeploymentUnits;
   }
   
   private Collection<DeploymentUnit> getEnterpriseBeanComponentUnits(DeploymentUnit unit)
   {
      Collection<DeploymentUnit> enterpriseBeanComponents = new HashSet<DeploymentUnit>();
      List<DeploymentUnit> components = unit.getComponents();
      if (components == null)
      {
         return enterpriseBeanComponents;
      }
      for (DeploymentUnit componentDU : components)
      {
         if (componentDU.isAttachmentPresent(JBossEnterpriseBeanMetaData.class))
         {
            enterpriseBeanComponents.add(componentDU);
         }
      }
      return enterpriseBeanComponents;
   }
   
   private boolean isStartupSingletonBean(JBossEnterpriseBeanMetaData enterpriseBean)
   {
      if (!enterpriseBean.getJBossMetaData().isEJB31())
      {
         // since Singleton are only available starting 3.1 version
         return false;
      }
      // we only process @Singleton session beans
      if(!enterpriseBean.isSession())
      {
         return false;  
      }
      // (ugly) check
      if (!(enterpriseBean instanceof JBossSessionBean31MetaData))
      {
         return false;
      }
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) enterpriseBean;
      if (!sessionBean.isSingleton())
      {
         return false;
      }
      if (!sessionBean.isInitOnStartup())
      {
         return false;
      }
      return true;
   }
}
