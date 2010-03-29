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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.session.SessionContainerInvocation;
import org.jboss.ejb3.singleton.aop.impl.context.SingletonBeanContext;
import org.jboss.injection.inject.spi.Injector;

/**
 * A AOP based implementation of the {@link InterceptorRegistry} for a
 * {@link AOPBasedSingletonContainer}
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedInterceptorRegistry implements InterceptorRegistry
{

   /**
    * The container to which this interceptor registry belongs
    */
   private AOPBasedSingletonContainer aopBasedSingletonContainer;

   /**
    * A hack actually. Ideally this shouldn't be stored in the interceptor
    * registry and instead be available through {@link AOPBasedSingletonContainer}.
    * But right now, i don't see any value of adding a public API to 
    * the {@link AOPBasedSingletonContainer} to expose this {@link SingletonBeanContext}
    */
   private SingletonBeanContext legacySingletonBeanContext;

   private Map<Class<?>, List<Injector<Object>>> interceptorInjectors = new HashMap<Class<?>, List<Injector<Object>>>();

   /**
    * Construct an {@link AOPBasedInterceptorRegistry} for a {@link AOPBasedSingletonContainer}
    * 
    * @param aopBasedContainer The container for which the interceptor registry is being
    *                       created. 
    */
   public AOPBasedInterceptorRegistry(AOPBasedSingletonContainer aopBasedContainer)
   {
      this.aopBasedSingletonContainer = aopBasedContainer;
   }

   /**
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getEJBContainer()
    */
   @Override
   public EJBContainer getEJBContainer()
   {
      return this.aopBasedSingletonContainer;
   }

   /**
    * Processes the <code>targetBeanContext</code> through a chain of AOP based
    * interceptors
    *  
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#intercept(ContainerInvocation, BeanContext)
    */
   @Override
   public Object intercept(ContainerInvocation containerInvocation, BeanContext targetBeanContext) throws Exception
   {
      // we handle only AOP based invocation
      if (!(containerInvocation instanceof AOPBasedContainerInvocation))
      {
         throw new IllegalArgumentException(AOPBasedInterceptorRegistry.class + " can only handle "
               + AOPBasedContainerInvocation.class + " , was passed " + containerInvocation.getClass());
      }
      AOPBasedContainerInvocation aopInvocationContext = (AOPBasedContainerInvocation) containerInvocation;
      // form a AOP invocation
      MethodInfo methodInfo = aopInvocationContext.getMethodInfo();

      EJBContainerInvocation<AOPBasedSingletonContainer, SingletonBeanContext> invocation = new SessionContainerInvocation<AOPBasedSingletonContainer, SingletonBeanContext>(
            containerInvocation.getInvokedBusinessInterface(), aopInvocationContext.getMethodInfo());
      invocation.setAdvisor(methodInfo.getAdvisor());
      invocation.setArguments(containerInvocation.getArgs());
      // we ignore the passed bean context and always used the singleton bean context that we have locally
      // TODO: Rethink on this approach
      // set the target bean context of the AOP invocation
      invocation.setBeanContext(this.legacySingletonBeanContext);

      
         // fire the AOP invocation
         try
         {
            return invocation.invokeNext();
         }
         catch (Exception e)
         {
            // throw Exception(s) as-is
            throw e;
         }
         catch (Throwable t)
         {
            // wrap throwable (errors) as Exception
            throw new Exception(t);
         }
      
   }

   /**
    * This is a no-op since singleton beans do not have a post-activate lifecycle
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePostActivate(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePostActivate(BeanContext targetBeanContext) throws Exception
   {
      // nothing to do for singleton beans

   }

   /**
    * Invokes the post-construct lifecycle on the <code>targetBeanContext</code>
    * by processing it through a chain of AOP interceptors (and user defined javax.interceptor.Interceptor)
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePostConstruct(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePostConstruct(BeanContext targetBeanContext) throws Exception
   {
      // fallback on legacy AOP based lifecycle impl
      this.legacySingletonBeanContext = new SingletonBeanContext(this.aopBasedSingletonContainer,
            targetBeanContext);
      // TODO: This passes the bean context through a series of injectors
      // which are responsible for carrying out the injection on the bean context (ex: field based
      // injection like for @PersistenceContext).
      // THIS HAS TO BE IN THE EJBLifecycleHandler SO THAT THE INJECTIONS
      // CAN HAPPEN ON LIFECYCLE EVENTS (LIKE BEAN INSTANTIATION)
      //this.aopBasedSingletonContainer.injectBeanContext(legacySingletonBeanContext);

      // Note: The method name initialiseInterceptorInstances() gives a 
      // wrong impression. This method not just instantiates a interceptor instance,
      // but it also injects (through the help of InjectionHanlder(s)) the interceptor
      // instance for any injectable fields
      // TODO: Ideally, this should be split up into separate instantiation and injection
      // calls.
      //legacySingletonBeanContext.initialiseInterceptorInstances();
      for (Class<?> interceptorClass : this.getInterceptorClasses())
      {
         Object interceptorInstance = legacySingletonBeanContext.getInterceptor(interceptorClass);
         List<Injector<Object>> injectors = this.interceptorInjectors.get(interceptorClass);
         if (injectors == null)
         {
            continue;
         }
         for (Injector<Object> injector : injectors)
         {
            injector.inject(interceptorInstance);
         }
      }

      // invoke post construct lifecycle on the bean/interceptor instances
      this.aopBasedSingletonContainer.invokePostConstruct(legacySingletonBeanContext);

   }

   /**
    * Invokes the pre-destroy lifecycle on the <code>targetBeanContext</code>
    * by processing it through a chain of AOP interceptors (and user defined javax.interceptor.Interceptor)
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePreDestroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePreDestroy(BeanContext targetBeanContext) throws Exception
   {
      // fallback on legacy AOP based lifecycle impl
      org.jboss.ejb3.BeanContext<?> legacyBeanContext = new SingletonBeanContext(this.aopBasedSingletonContainer,
            targetBeanContext);
      this.aopBasedSingletonContainer.invokePreDestroy(legacyBeanContext);

   }

   /**
    * This is a no-op since singleton beans do not have a pre-passivate lifecycle
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePrePassivate(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePrePassivate(BeanContext targetBeanContext) throws Exception
   {
      // nothing to do for singleton beans

   }

   /**
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getInterceptorClasses()
    */
   @Override
   public List<Class<?>> getInterceptorClasses()
   {
      return this.aopBasedSingletonContainer.getBeanContainer().getInterceptorRegistry().getInterceptorClasses();
   }

   /**
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getInterceptorInjectors()
    */
   @Override
   public Map<Class<?>, List<Injector<Object>>> getInterceptorInjectors()
   {
      return this.interceptorInjectors;
   }

   /**
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#setInterceptorInjectors(java.util.List)
    */
   @Override
   public void setInterceptorInjectors(Map<Class<?>, List<Injector<Object>>> interceptorInjectors)
   {
      this.interceptorInjectors = interceptorInjectors;

   }

}
