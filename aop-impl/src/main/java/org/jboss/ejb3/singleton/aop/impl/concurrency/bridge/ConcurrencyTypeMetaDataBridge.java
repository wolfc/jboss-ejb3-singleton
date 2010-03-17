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
package org.jboss.ejb3.singleton.aop.impl.concurrency.bridge;

import java.lang.annotation.Annotation;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;

import org.jboss.ejb3.metadata.MetaDataBridge;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.spi.signature.DeclaredMethodSignature;

/**
 * An implementation of {@link MetaDataBridge} which is responsible for resolving
 * the {@link ConcurrencyManagement} annotation from EJB metadata.
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ConcurrencyTypeMetaDataBridge implements MetaDataBridge<JBossEnterpriseBeanMetaData>
{

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader)
   {
      if (annotationClass == null || annotationClass.equals(ConcurrencyManagement.class) == false)
      {
         return null;
      }
      // only session beans and that too of type JBossSessionBean31MetaData
      if (metaData.isSession() == false || (metaData instanceof JBossSessionBean31MetaData) == false)
      {
         return null;
      }
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) metaData;
      ConcurrencyManagementType concurrencyManagementType = sessionBean.getConcurrencyManagementType();
      if (concurrencyManagementType == null)
      {
         return null;
      }
      ConcurrencyManagement conManagement = new ConcurrencyManagementImpl(concurrencyManagementType);
      return annotationClass.cast(conManagement);
   }

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader, org.jboss.metadata.spi.signature.DeclaredMethodSignature)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader, DeclaredMethodSignature method)
   {
      // @ConcurrencyManagement applies only to class
      return null;
   }

   /**
    * 
    * Implementation of {@link ConcurrencyManagement} annotation
    *
    * @author Jaikiran Pai
    * @version $Revision: $
    */
   private class ConcurrencyManagementImpl implements ConcurrencyManagement
   {
      private ConcurrencyManagementType concurrencyManagementType;

      /**
       * Creates a {@link ConcurrencyManagementImpl} for a {@link ConcurrencyManagementType}
       * @param concurrencyType
       */
      public ConcurrencyManagementImpl(ConcurrencyManagementType concurrencyType)
      {
         this.concurrencyManagementType = concurrencyType;
      }

      /**
       * @see javax.ejb.ConcurrencyManagement#value()
       */
      @Override
      public ConcurrencyManagementType value()
      {
         return this.concurrencyManagementType;
      }

      /**
       * @see java.lang.annotation.Annotation#annotationType()
       */
      @Override
      public Class<? extends Annotation> annotationType()
      {
         return ConcurrencyManagement.class;
      }
   }
}
