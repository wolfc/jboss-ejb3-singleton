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
import org.jboss.aop.advice.Interceptor;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.session.SessionContainerInvocation;
import org.jboss.ejb3.singleton.aop.impl.context.LegacySingletonBeanContext;

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
      if (!(containerInvocation instanceof AOPBasedContainerInvocation))
      {
         throw new IllegalArgumentException(AOPBasedInterceptorRegistry.class + " can only handle "
               + AOPBasedContainerInvocation.class + " , was passed " + containerInvocation.getClass());
      }
      
      if (!(targetBeanContext instanceof org.jboss.ejb3.BeanContext))
      {
         throw new IllegalArgumentException(AOPBasedInterceptorRegistry.class + " can only handle "
               + org.jboss.ejb3.BeanContext.class + " , was passed " + targetBeanContext.getClass());
      }
      AOPBasedContainerInvocation aopInvocationContext = (AOPBasedContainerInvocation) containerInvocation;
      // form a AOP invocation
      MethodInfo methodInfo = aopInvocationContext.getMethodInfo();
      Interceptor[] aopInterceptors = aopInvocationContext.getInterceptors();

      EJBContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext> invocation = new SessionContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext>(
            containerInvocation.getInvokedBusinessInterface(), aopInvocationContext.getMethodInfo(), aopInterceptors,
            aopBasedSingletonContainer);
      invocation.setAdvisor(methodInfo.getAdvisor());
      invocation.setArguments(containerInvocation.getArgs());
      // set the target bean context of the AOP invocation
      invocation.setBeanContext((org.jboss.ejb3.interceptors.container.BeanContext<?>) targetBeanContext);

      
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
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePostConstruct(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePostConstruct(BeanContext targetBeanContext) throws Exception
   {

   }

   /**
    * 
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePreDestroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void invokePreDestroy(BeanContext targetBeanContext) throws Exception
   {
      
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
