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

import org.jboss.ejb3.metadata.MetaDataBridge;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.spi.signature.DeclaredMethodSignature;

import javax.ejb.AccessTimeout;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link MetaDataBridge} which is responsible for
 * resolving the {@link AccessTimeout} annotation from EJB metadata
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AccessTimeoutMetaDataBridge implements MetaDataBridge<JBossEnterpriseBeanMetaData>
{

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader)
   {
      if (annotationClass == null || annotationClass.equals(AccessTimeout.class) == false)
      {
         return null;
      }
      // only session beans and that too of type JBossSessionBean31MetaData
      if (metaData.isSession() == false || (metaData instanceof JBossSessionBean31MetaData) == false)
      {
         return null;
      }
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) metaData;
      AccessTimeoutMetaData accessTimeoutMetaData = sessionBean.getAccessTimeout();
      if (accessTimeoutMetaData == null)
      {
         return null;
      }
      AccessTimeout accessTimeout = new AccessTimeoutImpl(accessTimeoutMetaData.getTimeout(), accessTimeoutMetaData
            .getUnit());
      return annotationClass.cast(accessTimeout);
   }

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader, org.jboss.metadata.spi.signature.DeclaredMethodSignature)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader, DeclaredMethodSignature method)
   {
      if (annotationClass == null || annotationClass.equals(AccessTimeout.class) == false)
      {
         return null;
      }
      // only session beans and that too of type JBossSessionBean31MetaData
      if (metaData.isSession() == false || (metaData instanceof JBossSessionBean31MetaData) == false)
      {
         return null;
      }
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) metaData;
      // create  a named method metadata to represent the method being queried
      NamedMethodMetaData namedMethod = new NamedMethodMetaData();
      namedMethod.setName(method.getName());
      if (method.getParameters() != null)
      {
         MethodParametersMetaData methodParams = new MethodParametersMetaData();
         methodParams.addAll(Arrays.asList(method.getParameters()));
         // set the method params on the named method metadata
         namedMethod.setMethodParams(methodParams);
      }
      ConcurrentMethodsMetaData concurrentMethods = sessionBean.getConcurrentMethods();
      if(concurrentMethods == null)
         return null;
      // get the concurrency method metadata for this named method
      ConcurrentMethodMetaData concurrentMethodMetaData = concurrentMethods.find(namedMethod);
      AccessTimeoutMetaData accessTimeoutMetaData = null;
      // if this named method did not have concurrency metadata or access timeout metadata, then
      // check for the method named "*" and see if that has the access timeout set
      if (concurrentMethodMetaData == null || concurrentMethodMetaData.getAccessTimeout() == null)
      {
         // get access timeout for method "*"
         accessTimeoutMetaData = getAccessTimeoutApplicableForAllMethods(sessionBean);
      }
      // access timeout was not specified for this method nor for the 
      // method "*"
      if (accessTimeoutMetaData == null)
      {
         return null;
      }
      AccessTimeout accessTimeout = new AccessTimeoutImpl(accessTimeoutMetaData.getTimeout(), accessTimeoutMetaData
            .getUnit());
      return annotationClass.cast(accessTimeout);
   }

   /**
    * Returns the {@link AccessTimeoutMetaData} specified for the method "*". Returns null
    * if there is no {@link AccessTimeoutMetaData} applies to that method.
    * 
    * @param sessionBean Session bean metadata
    * @return
    */
   private AccessTimeoutMetaData getAccessTimeoutApplicableForAllMethods(JBossSessionBean31MetaData sessionBean)
   {
      NamedMethodMetaData allMethods = new NamedMethodMetaData();
      allMethods.setName("*");
      ConcurrentMethodMetaData concurrentMethod = sessionBean.getConcurrentMethods().get(allMethods);
      if (concurrentMethod == null)
      {
         return null;
      }
      return concurrentMethod.getAccessTimeout();
   }

   /**
    * Implementation of {@link AccessTimeout} annotation
    * 
    *
    * @author Jaikiran Pai
    * @version $Revision: $
    */
   private class AccessTimeoutImpl implements AccessTimeout
   {

      private TimeUnit unit;

      private long timeout;

      private final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

      /**
       * Create a {@link AccessTimeoutImpl} from timeout value and unit
       * 
       * @param timeout Timeout value
       * @param unit Can be null, in which case {@link #unit()} return {@link TimeUnit#MILLISECONDS}
       */
      public AccessTimeoutImpl(long timeout, TimeUnit unit)
      {
         this.timeout = timeout;
         this.unit = unit;
      }

      /**
       * @see javax.ejb.AccessTimeout#unit()
       */
      @Override
      public TimeUnit unit()
      {
         return this.unit == null ? DEFAULT_TIME_UNIT : this.unit;
      }

      /**
       * @see javax.ejb.AccessTimeout#value()
       */
      @Override
      public long value()
      {
         return this.timeout;
      }

      /**
       * @see java.lang.annotation.Annotation#annotationType()
       */
      @Override
      public Class<? extends Annotation> annotationType()
      {
         return AccessTimeout.class;
      }

   }
}
