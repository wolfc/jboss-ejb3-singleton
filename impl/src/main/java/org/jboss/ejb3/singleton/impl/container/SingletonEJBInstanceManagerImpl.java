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
package org.jboss.ejb3.singleton.impl.container;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;

/**
 * SingletonInstanceManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonEJBInstanceManagerImpl implements SingletonEJBInstanceManager
{

   /**
    * Bean implementation class
    */
   private Class<?> beanClass;

   /**
    * The EJB container
    */
   private EJBContainer container;

   /**
    * The singleton bean context
    */
   private BeanContext singletonBeanContext;

   /**
    * Constructs a {@link SingletonEJBInstanceManagerImpl} for the <code>beanClass</code> and
    * its associated {@link EJBContainer}
    * 
    * @param beanClass The bean implementation class
    * @param container The container managing the bean
    */
   public SingletonEJBInstanceManagerImpl(Class<?> beanClass, EJBContainer container)
   {
      this.beanClass = beanClass;
      this.container = container;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#create()
    */
   @Override
   public BeanContext create()
   {
      if (this.singletonBeanContext == null)
      {
         boolean newInstanceCreated = false;
         synchronized (this)
         {
            if (this.singletonBeanContext == null)
            {
               Object beanInstance = this.createBeanInstance();
               this.singletonBeanContext = new SingletonBeanContext(beanInstance, this.container);
               newInstanceCreated = true;
            }
         }
         if (newInstanceCreated)
         {
            // do post-construct
            EJBLifecycleHandler beanLifecycleHandler = this.getEJBContainer().getEJBLifecycleHandler();
            beanLifecycleHandler.postConstruct(this.singletonBeanContext);
         }
      }

      return this.singletonBeanContext;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#get()
    */
   @Override
   public BeanContext get()
   {
      if (this.singletonBeanContext == null)
      {
         this.create();
      }
      return this.singletonBeanContext;

   }

   /**
    * @see org.jboss.ejb3.container.spi.StatefulEJBInstanceManager#getEJBContainer()
    */
   @Override
   public EJBContainer getEJBContainer()
   {
      return this.container;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#destroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void destroy(BeanContext beanContext) throws IllegalArgumentException
   {
      // TODO Auto-generated method stub

   }

   /**
    * Creates an instance of the bean class
    * 
    * @return Returns the bean instance
    */
   private Object createBeanInstance()
   {
      try
      {
         return this.beanClass.newInstance();
      }
      catch (InstantiationException ie)
      {
         throw new RuntimeException("Could not create an instance of the bean classs: " + this.beanClass, ie);
      }
      catch (IllegalAccessException iae)
      {
         throw new RuntimeException("Could not create an instance of the bean class: " + this.beanClass, iae);
      }
   }

}
