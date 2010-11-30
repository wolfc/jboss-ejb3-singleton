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

import java.io.Serializable;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.instantiator.spi.BeanInstantiationException;
import org.jboss.ejb3.instantiator.spi.BeanInstantiator;
import org.jboss.ejb3.instantiator.spi.InvalidConstructionParamsException;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
import org.jboss.logging.Logger;

/**
 * SingletonInstanceManager
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonEJBInstanceManagerImpl implements SingletonEJBInstanceManager
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonEJBInstanceManagerImpl.class);

   /**
    * Bean implementation class
    */
   protected Class<?> beanClass;

   /**
    * The EJB container
    */
   protected EJBContainer container;

   /**
    * An {@link EJBLifecycleHandler} responsible for handling the
    * lifecyle of the bean instances created/destroyed by this
    * {@link EJBInstanceManager}
    */
   protected EJBLifecycleHandler beanInstanceLifecycleHandler;

   /**
    * The singleton bean context
    */
   protected BeanContext singletonBeanContext;

   /**
    * Responsible for instantiating a bean
    */
   protected BeanInstantiator beanInstantiator;

   /**
    * Constructs a {@link SingletonEJBInstanceManagerImpl} for the <code>beanClass</code> and
    * its associated {@link EJBContainer}
    * 
    * @param beanClass The bean implementation class
    * @param container The container managing the bean
    */
   public SingletonEJBInstanceManagerImpl(Class<?> beanClass, EJBContainer container,
         EJBLifecycleHandler lifecycleHandler)
   {
      this(beanClass, container, lifecycleHandler, null);
   }

   /**
    * Constructs a {@link SingletonEJBInstanceManagerImpl} for the <code>beanClass</code> and
    * its associated {@link EJBContainer}
    * 
    * @param beanClass The bean implementation class
    * @param container The container managing the bean
    * @param beanInstantiator {@link BeanInstantiator} which will be used to create a bean instance. This can be null,
    *                       in which case, this {@link SingletonEJBInstanceManagerImpl} will directly instantiate the
    *                       bean instance through a call to {@link Class#newInstance()}.
    */
   public SingletonEJBInstanceManagerImpl(Class<?> beanClass, EJBContainer container,
         EJBLifecycleHandler lifecycleHandler, BeanInstantiator beanInstantiator)
   {
      this.beanClass = beanClass;
      this.container = container;
      this.beanInstanceLifecycleHandler = lifecycleHandler;
      this.beanInstantiator = beanInstantiator;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#create()
    */
   @Override
   public synchronized Serializable create()
   {
      if(this.singletonBeanContext != null)
         throw new IllegalStateException("Singleton " + container + " was already created");

      Object beanInstance = this.createBeanInstance();
      this.singletonBeanContext = this.createBeanContext(beanInstance);

      if (this.beanInstanceLifecycleHandler != null)
      {
         // do post-construct
         try
         {
            this.beanInstanceLifecycleHandler.postConstruct(this.singletonBeanContext);
         }
         catch (Exception e)
         {
            throw new RuntimeException("Could not invoke PostConstruct on the newly created bean instance", e);
         }
      }
      return this.singletonBeanContext.getSessionId();
   }

   /**
    * 
    * @see org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager#get()
    */
   @Override
   public synchronized BeanContext get()
   {
      if (this.singletonBeanContext == null)
      {
         this.create();
      }
      return this.singletonBeanContext;

   }

   /**
    * @see org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager#destroy()
    */
   @Override
   public void destroy()
   {
      synchronized (this)
      {
         if (this.singletonBeanContext == null)
         {
            return; // Or should we throw IllegalStateException?
         }
      }
      try
      {
         this.beanInstanceLifecycleHandler.preDestroy(this.singletonBeanContext);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not preDestroy the singleton bean instance", e);
      }
      synchronized (this)
      {
         this.singletonBeanContext = null;
      }
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
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#destroy(Serializable)
    */
   @Override
   public void destroy(Serializable sessionId) throws IllegalArgumentException, IllegalStateException
   {
      throw new IllegalStateException("destroy(sessionId) cannot be called on a singleton bean's instance manager");

   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBInstanceManager#isSessionAware()
    */
   @Override
   public boolean isSessionAware()
   {
      return false;
   }

   protected BeanContext createBeanContext(Object beanInstance)
   {
      return new SingletonBeanContext(beanInstance, this.container);
   }

   /**
    * Creates an instance of the bean class
    * 
    * @return Returns the bean instance
    */
   protected Object createBeanInstance()
   {
      if (this.beanInstantiator != null)
      {
         try
         {
            return this.beanInstantiator.create(this.beanClass, null);
         }
         catch (IllegalArgumentException iae)
         {
            throw iae;
         }
         catch (InvalidConstructionParamsException ice)
         {
            throw ice;
         }
         catch (BeanInstantiationException bie)
         {
            throw new RuntimeException("Could not create an instance for bean class: " + this.beanClass, bie);
         }
      }
      else
      {
         // fall back on normal instantiation.
         try
         {
            logger.debug("No bean instantiator configured. Using Class.newInstance() to instantiate bean class: "
                  + this.beanClass);
            
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

}
