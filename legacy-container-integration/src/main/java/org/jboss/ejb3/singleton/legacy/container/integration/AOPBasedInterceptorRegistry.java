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
package org.jboss.ejb3.singleton.legacy.container.integration;

import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocationContext;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;

/**
 * AOPBasedInterceptorRegistry
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedInterceptorRegistry implements InterceptorRegistry
{

   private AOPBasedSingletonContainer aopBasedSingletonContainer;

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
      return this.aopBasedSingletonContainer.getSingletonContainer();
   }

   /**
    * @see org.jboss.ejb3.container.spi.InterceptorRegistry#intercept(ContainerInvocationContext, BeanContext)
    */
   @Override
   public Object intercept(ContainerInvocationContext containerInvocation, BeanContext targetBeanContext) throws Exception
   {
      // we handle only AOP based invocation
      if (!(containerInvocation instanceof AOPBasedContainerInvocationContext))
      {
         throw new IllegalArgumentException(AOPBasedInterceptorRegistry.class + " can only handle "
               + AOPBasedContainerInvocationContext.class + " , was passed " + containerInvocation.getClass());
      }
      AOPBasedContainerInvocationContext aopInvocationContext = (AOPBasedContainerInvocationContext) containerInvocation;

      MethodInfo methodInfo = aopInvocationContext.getMethodInfo();
      EJBContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext> invocation = new EJBContainerInvocation<AOPBasedSingletonContainer, LegacySingletonBeanContext>(
            aopInvocationContext.getMethodInfo());
      invocation.setAdvisor(methodInfo.getAdvisor());
      invocation.setArguments(containerInvocation.getArgs());
      // get bean context (we could have used the passed one, but we need org.jboss.ejb3.BeanContext for the 
      // AOP based invocation. Hence get it from the AOP based container)
      org.jboss.ejb3.BeanContext<?> singletonBeanContext = this.aopBasedSingletonContainer.getSingletonBeanContext();
      invocation.setBeanContext(singletonBeanContext);
      
      try
      {
         return invocation.invokeNext();
      }
      catch (Throwable t)
      {
         throw new RuntimeException(t);
      }
   }

}
