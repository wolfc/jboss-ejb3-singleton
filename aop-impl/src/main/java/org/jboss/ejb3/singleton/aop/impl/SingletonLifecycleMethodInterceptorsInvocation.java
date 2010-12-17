/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.aop.advice.Interceptor;
import org.jboss.ejb3.interceptors.container.LifecycleMethodInterceptorsInvocation;
import org.jboss.ejb3.singleton.aop.impl.context.LegacySingletonBeanContext;
import org.jboss.ejb3.tx2.aop.EJBInvocation;
import org.jboss.ejb3.tx2.spi.TransactionalInvocationContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class SingletonLifecycleMethodInterceptorsInvocation extends LifecycleMethodInterceptorsInvocation
   implements EJBInvocation
{
   private TransactionalInvocationContext invocationContext;

   /**
    * Constructor
    *
    * @param beanContext  The bean context. Cannot be null
    * @param interceptors The interceptors
    * @throws IllegalArgumentException if <code>beanContext</code> is null
    */
   public SingletonLifecycleMethodInterceptorsInvocation(LegacySingletonBeanContext beanContext, Interceptor[] interceptors)
   {
      super(beanContext, interceptors);

      this.invocationContext = new ConstructionInvocationContextAdapter(this, beanContext.getEJBContainer());
   }

   @Override
   public TransactionalInvocationContext getInvocationContext()
   {
      return invocationContext;
   }
}
