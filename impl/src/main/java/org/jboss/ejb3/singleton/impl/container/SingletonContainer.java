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

import java.lang.reflect.Method;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocationContext;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBDeploymentInfo;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
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

   /**
    * Creates a {@link SingletonContainer} for the EJB class <code>beanClass</code>
    * and the associated session bean metadata <code>sessionBeanMetaData</code>
    *  
    * @param beanClass The EJB implementation class
    * @param sessionBeanMetaData The session bean metadata
    * @throws IllegalArgumentException If either <code>beanClass</code> or <code>sessionBeanMetaData</code> 
    *                               is null.
    * @throws IllegalStateException If the <code>sesssionBeanMetadata</code> does not represent a singleton
    *               bean - which is checked by a call to {@link JBossSessionBean31MetaData#isSingleton()}
    */
   public SingletonContainer(Class<?> beanClass, JBossSessionBean31MetaData sessionBeanMetaData)
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

      this.beanClass = beanClass;
      this.sessionBeanMetaData = sessionBeanMetaData;

      // create instance manager
      this.instanceManager = new SingletonEJBInstanceManagerImpl(beanClass, this);
      // create an empty interceptor registry
      this.interceptorRegistry = new EmptyInterceptorRegistry();
   }

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
    *               ({@link #invoke(ContainerInvocationContext)})
    * @throws IllegalArgumentException If any of the passed parameters is null.
    * @throws IllegalStateException If the <code>sesssionBeanMetadata</code> does not represent a singleton
    *               bean - which is checked by a call to {@link JBossSessionBean31MetaData#isSingleton()}                
    */
   public SingletonContainer(Class<?> beanClass, JBossSessionBean31MetaData sessionBeanMetaData,
         InterceptorRegistry interceptorRegistry)
   {
      this(beanClass, sessionBeanMetaData);
      if (interceptorRegistry == null)
      {
         throw new IllegalArgumentException(SingletonContainer.class.getSimpleName()
               + " cannot be constructed out of a null interceptor registry");
      }
      this.interceptorRegistry = interceptorRegistry;
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
    * @see org.jboss.ejb3.container.spi.EJBContainer#getDeploymentInfo()
    */
   @Override
   public EJBDeploymentInfo getDeploymentInfo()
   {
      // TODO: Implement this (and rethink whether we need this method at all)
      return null;
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
    * @see org.jboss.ejb3.container.spi.EJBContainer#invoke(org.jboss.ejb3.container.spi.ContainerInvocationContext)
    */
   @Override
   public Object invoke(ContainerInvocationContext containerInvocation) throws Exception
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
    * @see org.jboss.ejb3.container.spi.EJBContainer#getEJBLifecycleHandler()
    */
   @Override
   public EJBLifecycleHandler getEJBLifecycleHandler()
   {
      return this;
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#postConstruct(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void postConstruct(BeanContext beanContext)
   {
      // TODO: Implement this
      logger.warn("postConstruct() not yet implemented in " + this.getClass());
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#preDestroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void preDestroy(BeanContext beanContext)
   {
      // TODO: Implement this
      logger.warn("preDestroy() not yet implemented in " + this.getClass());
   }

   /**
    * 
    * EmptyInterceptorRegistry
    * 
    * <p>
    * An implementation of the {@link InterceptorRegistry}. This does NOT apply any interceptors
    * to the invocation on the target object during the {@link #intercept(ContainerInvocationContext, Object)}
    * call. Instead, it directly invokes the method on the target object 
    * </p>
    * TODO: This {@link EmptyInterceptorRegistry} probably needs to be in a more better place to be used
    * commonly by other containers. Probably this class needs to be an API.
    * 
    * @author Jaikiran Pai
    * @version $Revision: $
    */
   private class EmptyInterceptorRegistry implements InterceptorRegistry
   {

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getEJBContainer()
       */
      @Override
      public EJBContainer getEJBContainer()
      {
         return SingletonContainer.this;
      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#intercept(ContainerInvocationContext, BeanContext)
       */
      @Override
      public Object intercept(ContainerInvocationContext containerInvocation, BeanContext targetBeanContext) throws Exception
      {
         // just directly invoke on the target object
         Method methodToBeInvoked = containerInvocation.getMethod();
         Object args[] = containerInvocation.getArgs();

         // invoke
         return methodToBeInvoked.invoke(targetBeanContext.getBeanInstance(), args);
      }

   }

}
