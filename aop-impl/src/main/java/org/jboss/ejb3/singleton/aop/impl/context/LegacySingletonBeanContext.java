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
package org.jboss.ejb3.singleton.aop.impl.context;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.spi.EJBContext;
import org.jboss.ejb3.context.spi.SessionInvocationContext;
import org.jboss.ejb3.session.SessionSpecBeanContext;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.timerservice.spi.TimerServiceInvocationContext;
import org.jboss.logging.Logger;

import java.io.Serializable;

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
   private static Logger logger = Logger.getLogger(LegacySingletonBeanContext.class);
   
   private AOPBasedSingletonContainer aopBasedSingletonContainer;


   public LegacySingletonBeanContext(AOPBasedSingletonContainer aopBasedSingletonContainer, Object beanInstance)
   {
      super(aopBasedSingletonContainer, beanInstance);
      this.aopBasedSingletonContainer = aopBasedSingletonContainer;
   }

   /**
    * @see org.jboss.ejb3.session.SessionBeanContext#getEJBContext()
    */
   @Override
   public EJBContext getEJBContext()
   {
      if (this.ejbContext == null)
      {
         this.ejbContext = new SingletonSessionContext(this);
      }
      return this.ejbContext;
   }

   /**
    * @see org.jboss.ejb3.BeanContext#remove()
    */
   @Override
   public void remove()
   {
      logger.warn(this.getClass().getName() + ".remove() not yet implemented");
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
   public AOPBasedSingletonContainer getEJBContainer()
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
   
   @Override
   public SessionInvocationContext createLifecycleInvocation()
   {
      return new SingletonBeanLifecylceInvocationContext();
   }
   
   private class SingletonBeanLifecylceInvocationContext extends BaseSessionInvocationContext implements TimerServiceInvocationContext
   {

      public SingletonBeanLifecylceInvocationContext()
      {
         super(null, null, null);
         // TODO Auto-generated constructor stub
      }

      @Override
      public Object proceed() throws Exception
      {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public boolean isSingleton()
      {
         return true;
      }
      
   }
}
