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
package org.jboss.ejb3.singleton.aop.impl;

import java.util.List;

import javax.ejb.DependsOn;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.instantiator.spi.BeanInstantiator;
import org.jboss.ejb3.singleton.aop.impl.context.LegacySingletonBeanContext;
import org.jboss.ejb3.singleton.impl.container.SingletonEJBInstanceManagerImpl;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;

/**
 * AOPBasedSingletonInstanceManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedSingletonInstanceManager extends SingletonEJBInstanceManagerImpl
{

   /**
    * The container to which this instance manger belongs 
    */
   private AOPBasedSingletonContainer aopBasedContainer;

   /**
    * @param container The AOP based singleton container to which this instance manager belongs
    * @param beanInstantiator {@link BeanInstantiator} used for creating the EJB instances
    * @param dependsOn The list of {@link SingletonEJBInstanceManager instance managers} which will be used 
    *                   to instantiate the {@link DependsOn @DependsOn} singleton beans
    * 
    */
   public AOPBasedSingletonInstanceManager(AOPBasedSingletonContainer container, BeanInstantiator beanInstantiator, List<SingletonEJBInstanceManager> dependsOn)
   {
      super(container.getBeanClass(), container, container, beanInstantiator, dependsOn);
      this.aopBasedContainer = container;
   }

   /**
    * @see org.jboss.ejb3.singleton.impl.container.SingletonEJBInstanceManagerImpl#createBeanContext(java.lang.Object)
    */
   @Override
   protected BeanContext createBeanContext(Object beanInstance)
   {
      return new LegacySingletonBeanContext(this.aopBasedContainer, beanInstance);
   }

}
