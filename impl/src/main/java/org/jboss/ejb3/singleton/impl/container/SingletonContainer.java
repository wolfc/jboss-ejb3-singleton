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

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
import org.jboss.injection.inject.spi.Injector;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

/**
 * SingletonContainer
 * <p>
 * Implementation of {@link EJBContainer} for a @Singleton bean.
 * </p>
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonContainer implements EJBContainer, EJBLifecycleHandler
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonContainer.class);

   /**
    * Bean implementation class 
    */
   private Class<?> beanClass;

   /**
    * Session bean metadata
    */
   private JBossSessionBean31MetaData sessionBeanMetaData;

   /**
    * EJB instance manager
    */
   private SingletonEJBInstanceManager instanceManager;

   /**
    * The interceptor registry for this container
    */
   private InterceptorRegistry interceptorRegistry;
   
   
   private List<Injector<Object>> ejbInstanceInjectors = new ArrayList<Injector<Object>>();

   /**
    * Creates a {@link SingletonContainer} for the EJB class <code>beanClass</code>
    * and the associated session bean metadata <code>sessionBeanMetaData</code>. The
    * <code>interceptorRegistry</code> will be used for intercepting the calls on the 
    * target bean instance
    * 
    * @param beanClass The EJB implementation class 
    * @param sessionBeanMetaData The session bean metadata
    * @param interceptorRegistry The interceptor registry which will be used to intercept
    *               the calls to the target bean instance during the invocation on the container 
    *               ({@link #invoke(ContainerInvocation)})
    * @throws IllegalArgumentException If any of the passed parameters is null.
    * @throws IllegalStateException If the <code>sesssionBeanMetadata</code> does not represent a singleton
    *               bean - which is checked by a call to {@link JBossSessionBean31MetaData#isSingleton()}                
    */
   public SingletonContainer(Class<?> beanClass, JBossSessionBean31MetaData sessionBeanMetaData,
         InterceptorRegistry interceptorRegistry)
   {
      if (beanClass == null || sessionBeanMetaData == null)
      {
         throw new IllegalArgumentException(SingletonContainer.class.getSimpleName()
               + " cannot be constructed out of a null bean class or null bean metadata");
      }
      // we handle only singleton beans. If this is not a singleton bean
      // then throw an exception
      if (!sessionBeanMetaData.isSingleton())
      {
         throw new IllegalStateException("Bean named " + sessionBeanMetaData.getEjbName() + " with class "
               + sessionBeanMetaData.getEjbClass() + " is NOT a singleton bean");
      }
      if (interceptorRegistry == null)
      {
         throw new IllegalArgumentException(SingletonContainer.class.getSimpleName()
               + " cannot be constructed out of a null interceptor registry");
      }

      this.beanClass = beanClass;
      this.sessionBeanMetaData = sessionBeanMetaData;

      // create instance manager
      this.instanceManager = new SingletonEJBInstanceManagerImpl(beanClass, this, this);
      // set the interceptor registry
      this.interceptorRegistry = interceptorRegistry;
      
      
   }
   
   /**
    * TODO: Think whether this needs to be made available in 
    * a lifecycle aware {@link EJBContainer} 
    */
   public void create()
   {
      
   }

   /**
    * TODO: Think whether this needs to be made available in 
    * a lifecycle aware {@link EJBContainer} 
    */
   public void start()
   {
      // TODO: If @Startup, then create a singleton bean context here
      
//      // Use ENCInjectors to setup ENC
//      for (EJBContainerENCInjector encInjector : this.encInjectors)
//      {
//         encInjector.inject(this);
//      }
      
      if (this.sessionBeanMetaData.isInitOnStartup())
      {
         this.instanceManager.create();
      }
   }
   
   /**
    * TODO: Think whether this needs to be made available in 
    * a lifecycle aware {@link EJBContainer} 
    */
   public void stop()
   {
      
   }

   /**
    * TODO: Think whether this needs to be made available in 
    * a lifecycle aware {@link EJBContainer} 
    */
   public void destroy()
   {
      this.instanceManager.destroy();
   }
   
   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getBeanInstanceManager()
    */
   @Override
   public EJBInstanceManager getBeanInstanceManager()
   {
      return this.instanceManager;
   }


   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getEJBClass()
    */
   @Override
   public String getEJBClass()
   {
      return this.beanClass.getName();
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getEJBName()
    */
   @Override
   public String getEJBName()
   {
      return this.sessionBeanMetaData.getEjbName();
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getMetaData()
    */
   @Override
   public JBossEnterpriseBeanMetaData getMetaData()
   {
      return this.sessionBeanMetaData;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#invoke(org.jboss.ejb3.container.spi.ContainerInvocation)
    */
   @Override
   public Object invoke(ContainerInvocation containerInvocation) throws Exception
   {
      BeanContext beanContext = this.instanceManager.get();

      // TODO: Should container managed concurrency be implemented here in this container,
      // or in some interceptor within the interceptor chain maintained by the interceptor
      // registry?
      return this.interceptorRegistry.intercept(containerInvocation, beanContext);

   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getInterceptorRegistry()
    */
   @Override
   public InterceptorRegistry getInterceptorRegistry()
   {
      return this.interceptorRegistry;
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#postConstruct(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void postConstruct(BeanContext beanContext) throws Exception
   {
      for (Injector<Object> injector : this.getEJBInjectors())
      {
         injector.inject(beanContext.getBeanInstance());
      }
      // pass the bean context to the interceptor registry to do its job.
      this.interceptorRegistry.invokePostConstruct(beanContext);
      
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#preDestroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void preDestroy(BeanContext beanContext) throws Exception
   {
      // pass the bean context to the interceptor registry to do its job.
      // we don't have anything specific/additional to do here.
      this.interceptorRegistry.invokePreDestroy(beanContext);
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getClassLoader()
    */
   @Override
   public ClassLoader getClassLoader()
   {
      return this.beanClass.getClassLoader();
   }
   
   public Context getENC()
   {
      // TODO: Hack!
      try
      {
         return new InitialContext();
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getEJBInjectors()
    */
   @Override
   public List<Injector<Object>> getEJBInjectors()
   {
      return this.ejbInstanceInjectors;
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#setEJBInjectors(java.util.List)
    */
   @Override
   public void setEJBInjectors(List<Injector<Object>> injectors)
   {
      this.ejbInstanceInjectors = injectors;
      
   }
   
}
