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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.context.spi.EJBContext;
import org.jboss.ejb3.session.SessionSpecBeanContext;

/**
 * LegacySingletonBeanContext
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class LegacySingletonBeanContext extends SessionSpecBeanContext<AOPBasedSingletonContainer>
      implements
         BeanContext
{
   private AOPBasedSingletonContainer aopBasedSingletonContainer;

   private Map<Class<?>, Object> interceptorInstances = new HashMap<Class<?>, Object>();

   public LegacySingletonBeanContext(AOPBasedSingletonContainer aopBasedSingletonContainer, BeanContext context)
   {
      super(aopBasedSingletonContainer, context.getBeanInstance());
      this.aopBasedSingletonContainer = aopBasedSingletonContainer;
      this.initInterceptorInstances();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.session.SessionBeanContext#getEJBContext()
    */
   @Override
   public EJBContext getEJBContext()
   {
      throw new RuntimeException("Not yet implemented");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#remove()
    */
   @Override
   public void remove()
   {
      throw new RuntimeException("Not yet implemented");

   }

   private void initInterceptorInstances()
   {
      InterceptorRegistry interceptorRegistry = this.getEJBContainer().getInterceptorRegistry();
      List<Class<?>> interceptorClasses = interceptorRegistry.getInterceptorClasses();
      for (Class<?> interceptorClass : interceptorClasses)
      {
         try
         {
            Object interceptorInstance = interceptorClass.newInstance();
            this.interceptorInstances.put(interceptorClass, interceptorInstance);
         }
         catch (InstantiationException ie)
         {
            throw new RuntimeException("Could not create interceptor instance for interceptor class "
                  + interceptorClass, ie);
         }
         catch (IllegalAccessException iae)
         {
            throw new RuntimeException("Could not create interceptor instance for interceptor class "
                  + interceptorClass, iae);
         }

      }

   }

   /**
    * @see org.jboss.ejb3.container.spi.BeanContext#getBeanInstance()
    */
   @Override
   public Object getBeanInstance()
   {
      return this.getInstance();
   }

   /**
    * @see org.jboss.ejb3.container.spi.BeanContext#getEJBContainer()
    */
   @Override
   public EJBContainer getEJBContainer()
   {
      return this.aopBasedSingletonContainer;
   }

   /**
    * @see org.jboss.ejb3.container.spi.BeanContext#getSessionId()
    */
   @Override
   public Serializable getSessionId()
   {
      // singleton beans dont have sessions
      return null;
   }
   
   /**
    * @see org.jboss.ejb3.BaseContext#getInterceptor(java.lang.Class)
    */
   @Override
   public Object getInterceptor(Class<?> interceptorClass) throws IllegalArgumentException
   {
      Object interceptor = this.interceptorInstances.get(interceptorClass);
      if(interceptor == null)
         throw new IllegalArgumentException("No interceptor found for " + interceptorClass + " in " + this);
      return interceptor;
      
   }
}
