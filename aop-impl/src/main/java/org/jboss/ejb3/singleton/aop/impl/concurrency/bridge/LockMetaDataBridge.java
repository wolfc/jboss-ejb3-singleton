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
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.spi.signature.DeclaredMethodSignature;

import javax.ejb.Lock;
import javax.ejb.LockType;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * An implementation of {@link MetaDataBridge} which is responsible for
 * resolving the {@link Lock} annotation from EJB metadata
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class LockMetaDataBridge implements MetaDataBridge<JBossEnterpriseBeanMetaData>
{

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader)
   {
      if (annotationClass == null || annotationClass.equals(Lock.class) == false)
      {
         return null;
      }
      // only session beans and that too of type JBossSessionBean31MetaData
      if (metaData.isSession() == false || (metaData instanceof JBossSessionBean31MetaData) == false)
      {
         return null;
      }
      JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) metaData;
      // get the bean level lock type
      LockType lockType = sessionBean.getLockType();
      // if no bean level lock type specified, then just return null
      if (lockType == null)
      {
         return null;
      }
      Lock lock = new LockImpl(lockType);
      return annotationClass.cast(lock);

   }

   /**
    * @see org.jboss.ejb3.metadata.MetaDataBridge#retrieveAnnotation(java.lang.Class, java.lang.Object, java.lang.ClassLoader, org.jboss.metadata.spi.signature.DeclaredMethodSignature)
    */
   @Override
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, JBossEnterpriseBeanMetaData metaData,
         ClassLoader classLoader, DeclaredMethodSignature method)
   {
      if (annotationClass == null || annotationClass.equals(Lock.class) == false)
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
      {
         // EJB3.1 spec, section 4.8.5.5 specifies special meaning for @Lock annotation
         // on superclass(es) of bean.
         // If the method invoked belongs to the superclass of the bean, then pick up
         // the @Lock annotation from the superclass. If it's absent on the superclass
         // then by default it's LockType.WRITE
         String declaringClass = method.getDeclaringClass();
         // the check here for @Singleton bean is for optimization, so as to avoid
         // doing additional checks for non @Singleton beans since only @Singleton beans have explicit @Lock
         if (sessionBean.isSingleton() && declaringClass != null && !sessionBean.getEjbClass().equals(declaringClass))
         {
            LockType lockType = sessionBean.getLockType(declaringClass);
            if (lockType == null)
            {
               lockType = LockType.WRITE;
            }
            Lock lock = new LockImpl(lockType);
            return annotationClass.cast(lock);
         }
         // the method was invoked on the bean class (and not on super class of bean).
         // So return null. Later resolveClassAnnotation will pick up the correct bean level
         // lock semantics (either via the annotation or via metadata)
         return null;
      }
      // get the concurrency method metadata for this named method
      ConcurrentMethodMetaData concurrentMethodMetaData = concurrentMethods.find(namedMethod);
      LockType lockType = null;
      // if this named method did not have concurrency metadata or lock metadata, then
      // check for the method named "*" and see if that has the lock type set
      if (concurrentMethodMetaData == null || concurrentMethodMetaData.getLockType() == null)
      {
         // get lock type for method "*"
         lockType = getLockTypeApplicableForAllMethods(sessionBean);
      }
      // lock type was not specified for this method nor for the 
      // method "*"
      if (lockType == null)
      {
         // EJB3.1 spec, section 4.8.5.5 specifies special meaning for @Lock annotation
         // on superclass(es) of bean.
         // If the method invoked belongs to the superclass of the bean, then pick up
         // the @Lock annotation from the superclass. If it's absent on the superclass
         // then by default it's LockType.WRITE
         String declaringClass = method.getDeclaringClass();
         // the check here for @Singleton bean is for optimization, so as to avoid
         // doing additional checks for non @Singleton beans since only @Singleton beans have explicit @Lock
         if (sessionBean.isSingleton() && declaringClass != null && !sessionBean.getEjbClass().equals(declaringClass))
         {
            lockType = sessionBean.getLockType(declaringClass);
            if (lockType == null)
            {
               lockType = LockType.WRITE;
            }
            Lock lock = new LockImpl(lockType);
            return annotationClass.cast(lock);
         }
         // the method was invoked on the bean class (and not on super class of bean).
         // So return null. Later resolveClassAnnotation will pick up the correct bean level
         // lock semantics (either via the annotation or via metadata)
         return null;
      }
      Lock lock = new LockImpl(lockType);
      return annotationClass.cast(lock);
   }

   /**
    * Returns the {@link LockType} specified for the method "*". Returns null
    * if there is no lock type specified.
    * 
    * @param sessionBean Session bean metadata
    * @return
    */
   private LockType getLockTypeApplicableForAllMethods(JBossSessionBean31MetaData sessionBean)
   {
      NamedMethodMetaData allMethods = new NamedMethodMetaData();
      allMethods.setName("*");
      ConcurrentMethodMetaData concurrentMethod = sessionBean.getConcurrentMethods().get(allMethods);
      if (concurrentMethod == null)
      {
         return null;
      }
      return concurrentMethod.getLockType();
   }
   
   /**
    * 
    * Implementation of {@link Lock} annotation
    *
    * @author Jaikiran Pai
    * @version $Revision: $
    */
   private class LockImpl implements Lock
   {

      private LockType lockType;

      /**
       * Create a {@link LockImpl} from a {@link LockType}
       * 
       * @param lockType
       */
      public LockImpl(LockType lockType)
      {
         this.lockType = lockType;
      }

      /**
       * @see javax.ejb.Lock#value()
       */
      @Override
      public LockType value()
      {
         return this.lockType;
      }

      /**
       * @see java.lang.annotation.Annotation#annotationType()
       */
      @Override
      public Class<? extends Annotation> annotationType()
      {
         return Lock.class;
      }

   }
}
