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

import org.jboss.dependency.spi.ControllerState;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

/**
 * Responsible for creating the instance of a startup singleton bean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class StartupSingletonInitiator
{

   /** Logger */
   private static Logger logger = Logger.getLogger(StartupSingletonInitiator.class);
   
   /**
    * The EJB container of the @Startup @Singleton bean 
    */
   private EJBContainer container;
   
   /**
    * 
    * @param container The EJB container of the @Startup @Singleton bean
    * @throws IllegalArgumentException If the passed <code>container</code> is null or if the <code>container</code>
    *                                   doesn't correspond to a @Startup @Singleton bean
    */
   public StartupSingletonInitiator(EJBContainer container)
   {
      if (container == null)
      {
         throw new IllegalArgumentException("Container cannot be null while creating " + this.getClass().getName());
      }
      if (!this.isStartupSingletonBean(container.getMetaData()))
      {
         throw new IllegalArgumentException(container.getEJBName() + " is not a @Startup @Singleton bean");
      }
      
      this.container = container;
   }
   
   /**
    * Creates a instance of the EJB corresponding to the passed {@link EJBContainer},
    * if the EJB represents a startup singleton bean.
    * 
    * @param container The {@link EJBContainer} which reached the {@link ControllerState#INSTALLED} state
    */
   public void start()
   {
      // create the instance
      EJBInstanceManager instanceManager = this.container.getBeanInstanceManager();
      if (instanceManager instanceof SingletonEJBInstanceManager)
      {
         SingletonEJBInstanceManager singletonBeanInstanceManager = (SingletonEJBInstanceManager) instanceManager;
         // get the instance (Note: don't call create, since create() throws an exception
         // if a singleton instance is already created)
         singletonBeanInstanceManager.get();
      }
      else
      {
         // fallback on the create() method (instead of get() on SingletonEJBInstanceManager) of the EJBInstanceManager
         instanceManager.create();
      }
      logger.debug("Created an instance of @Startup @Singleton bean: " + this.container.getEJBName());
   }
   
   /**
    * Returns true if the passed {@link JBossEnterpriseBeanMetaData bean metadata} corresponds to a
    * singleton startup bean. Else returns false
    * 
    * @param enterpriseBean The bean metadata.
    * @return
    */
   private boolean isStartupSingletonBean(JBossEnterpriseBeanMetaData enterpriseBean)
   {
      if (!enterpriseBean.getJBossMetaData().isEJB31())
      {
         // since Singleton are only available starting 3.1 version
         return false;
      }
      // we only process @Singleton session beans
      if (!enterpriseBean.isSession())
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
