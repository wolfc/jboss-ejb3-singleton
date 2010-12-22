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

import org.jboss.aop.Advisor;
import org.jboss.aop.joinpoint.ConstructionInvocation;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.ejb3.context.base.BaseInvocationContext;
import org.jboss.ejb3.tx2.spi.TransactionalInvocationContext;

import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Note that this class is not fully functional.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class ConstructionInvocationContextAdapter extends BaseInvocationContext
   implements TransactionalInvocationContext
{
   private ConstructionInvocation delegate;
   private EJBContainer container;

   public ConstructionInvocationContextAdapter(ConstructionInvocation delegate, EJBContainer container)
   {
      super(null, null);

      this.delegate = delegate;
      this.container = container;
   }

   private Advisor getAdvisor()
   {
      return delegate.getAdvisor();
   }
   
   @Override
   public ApplicationException getApplicationException(Class<?> e)
   {
      return container.getApplicationException(e, null);
   }

   @Override
   public TransactionAttributeType getTransactionAttribute()
   {
      TransactionAttribute tx = (TransactionAttribute) getAdvisor().resolveAnnotation(TransactionAttribute.class);
      
      TransactionAttributeType value = TransactionAttributeType.REQUIRED;
      if (tx != null && tx.value() != null)
      {
         value = tx.value();
      }

      // Note that the container must start a new transaction if the REQUIRED (Required) transaction
      // attribute is used. This guarantees, for example, that the transactional behavior of the PostConstruct
      // method is the same regardless of whether it is initialized eagerly at container startup time or as a side
      // effect of a first client invocation on the Singleton. The REQUIRED transaction attribute value is
      // allowed so that specification of a transaction attribute for the Singleton PostConstruct/PreDestroy
      // methods can be defaulted.
      if(value == TransactionAttributeType.REQUIRED)
         value = TransactionAttributeType.REQUIRES_NEW;

      return value;
   }

   @Override
   public int getTransactionTimeout()
   {
      TransactionTimeout annotation = (TransactionTimeout) getAdvisor().resolveAnnotation(TransactionTimeout.class);

      if (annotation != null)
      {
         return annotation.value();
      }

      return -1;
   }

   @Override
   public Object proceed() throws Exception
   {
      try
      {
         return delegate.invokeNext();
      }
      catch(Exception e)
      {
         throw e;
      }
      catch(Error e)
      {
         throw (Error) e;
      }
      catch(Throwable t)
      {
         throw new RuntimeException(t);
      }
   }
}
