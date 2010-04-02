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

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.singleton.aop.impl.context.LegacySingletonBeanContext;
import org.jboss.ejb3.singleton.impl.container.SingletonEJBInstanceManagerImpl;

/**
 * AOPBasedSingletonInstanceManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedSingletonInstanceManager extends SingletonEJBInstanceManagerImpl
{

   private AOPBasedSingletonContainer aopBasedContainer;

   /**
    * @param beanClass
    * @param container
    * @param lifecycleHandler
    */
   public AOPBasedSingletonInstanceManager(AOPBasedSingletonContainer container)
   {
      super(container.getBeanClass(), container, container);
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
