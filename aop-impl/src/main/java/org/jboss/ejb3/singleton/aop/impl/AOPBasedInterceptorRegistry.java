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

import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;

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
      if (!(containerInvocation instanceof AOPBasedContainerInvocationContext))
      {
         throw new IllegalArgumentException(AOPBasedInterceptorRegistry.class + " can only handle "
               + AOPBasedContainerInvocationContext.class + " , was passed " + containerInvocation.getClass());
      }
      AOPBasedContainerInvocationContext aopInvocationContext = (AOPBasedContainerInvocationContext) containerInvocation;
      // form a AOP invocation
      MethodInfo methodInfo = aopInvocationContext.getMethodInfo();
      EJBContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext> invocation = new EJBContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext>(
            aopInvocationContext.getMethodInfo());
      invocation.setAdvisor(methodInfo.getAdvisor());
      invocation.setArguments(containerInvocation.getArgs());
      // get bean context (we could have used the passed one, but we need org.jboss.ejb3.BeanContext for the 
      // AOP based invocation. Hence get it from the AOP based container)
      org.jboss.ejb3.BeanContext<?> singletonBeanContext = new LegacySingletonBeanContext(
            this.aopBasedSingletonContainer, targetBeanContext);
      
      // set the target bean context of the AOP invocation
      invocation.setBeanContext(singletonBeanContext);

      try
      {
         // fire the AOP invocation
         return invocation.invokeNext();
      }
      catch (Throwable t)
      {
         throw new RuntimeException(t);
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
      org.jboss.ejb3.BeanContext<?> legacyBeanContext = new LegacySingletonBeanContext(this.aopBasedSingletonContainer,
            targetBeanContext);
      // TODO: This passes the bean context through a series of injectors
      // which are responsible for carrying out the injection on the bean context (ex: field based
      // injection like for @PersistenceContext).
      // THIS HAS TO BE IN THE EJBLifecycleHandler SO THAT THE INJECTIONS
      // CAN HAPPEN ON LIFECYCLE EVENTS (LIKE BEAN INSTANTIATION)
      this.aopBasedSingletonContainer.injectBeanContext(legacyBeanContext);
      
      // Note: The method name initialiseInterceptorInstances() gives a 
      // wrong impression. This method not just instantiates a interceptor instance,
      // but it also injects (through the help of InjectionHanlder(s)) the interceptor
      // instance for any injectable fields
      // TODO: Ideally, this should be split up into separate instantiation and injection
      // calls.
      legacyBeanContext.initialiseInterceptorInstances();
      
      // invoke post construct lifecycle on the bean/interceptor instances
      this.aopBasedSingletonContainer.invokePostConstruct(legacyBeanContext);

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
      org.jboss.ejb3.BeanContext<?> legacyBeanContext = new LegacySingletonBeanContext(this.aopBasedSingletonContainer,
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

}
