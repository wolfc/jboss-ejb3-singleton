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
import org.jboss.aop.AspectManager;
import org.jboss.aop.Domain;
import org.jboss.aop.advice.AdviceStack;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.advice.PerVmAdvice;
import org.jboss.aspects.currentinvocation.CurrentInvocationInterceptor;
import org.jboss.ejb3.interceptors.aop.ExtendedAdvisor;
import org.jboss.ejb3.interceptors.aop.ExtendedAdvisorHelper;
import org.jboss.ejb3.interceptors.aop.InvocationContextInterceptor;
import org.jboss.ejb3.interceptors.aop.LifecycleCallbackBeanMethodInterceptor;
import org.jboss.ejb3.interceptors.aop.LifecycleCallbackInterceptorMethodLazyInterceptor;
import org.jboss.ejb3.interceptors.lang.ClassHelper;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A copy of org.jboss.ejb3.interceptors.aop.LifecycleCallback which does not support a custom lifecycle callback stack.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Deprecated
class LifecycleCallbacks
{
   private static final Logger log = Logger.getLogger(LifecycleCallbacks.class);

   private static HashSet<Class<?>> checkClass(HashSet<Class<?>> classes, Method m, Advisor advisor, Class<? extends Annotation> lifecycleAnnotationType)
   {
      if (classes != null)
      {
         if (classes.contains(m.getDeclaringClass()))
         {
            String type = null;
            if (lifecycleAnnotationType == PostConstruct.class)
            {
               type = "post-construct";
            }
            else if (lifecycleAnnotationType == PreDestroy.class)
            {
               type = "pre-destroy";
            }
            else if (lifecycleAnnotationType == PostActivate.class)
            {
               type = "post-activate";
            }
            else if (lifecycleAnnotationType == PrePassivate.class)
            {
               type = "pre-passivate";
            }
            throw new RuntimeException("More than one '" + type + "' method in " + advisor.getName());
         }
      }
      else
      {
         classes = new HashSet<Class<?>>();
      }
      classes.add(m.getDeclaringClass());
      return classes;
   }

   /**
    * Creates an AOP interceptor chain for the lifecycle represented by
    * the <code>lifecycleAnnotationType</code>.
    *
    * Internally, the AOP interceptor chain consists of the LifecycleCallback AOP stack
    * interceptors, the javax.interceptor.Interceptor(s) and the lifecycle methods on the bean
    * implementation class
    *
    * @param advisor The bean class advisor
    * @param lifecycleInterceptorClasses The lifecycle interceptor classes associated with the bean
    * @param lifecycleAnnotationType The lifecycle annotation (ex: @PostConstruct, @PrePassivate and
    *                               other similar lifecycle types).
    * @return Returns an empty array if there are no interceptors corresponding to the bean for
    *       the <code>lifecycleAnnotationType</code>. Else returns the applicable interceptors
    */
   public static Interceptor[] createLifecycleCallbackInterceptors(Advisor advisor, List<Class<?>> lifecycleInterceptorClasses, Class<? extends Annotation> lifecycleAnnotationType, String stackName)
   {
      List<Interceptor> interceptors = new ArrayList<Interceptor>();

      AdviceStack stack = advisor.getManager().getAdviceStack(stackName);
      if(stack == null)
      {
         log.warn("EJBTHREE-1480: " + stackName + " has not been defined for " + toString(advisor.getManager()));
         interceptors.add(new CurrentInvocationInterceptor());
         Interceptor invocationContextInterceptor;
         try
         {
            invocationContextInterceptor = PerVmAdvice.generateInterceptor(null, new InvocationContextInterceptor(), "setup");
         }
         catch (Exception e)
         {
            throw new RuntimeException("Could not generate invocation context interceptor", e);
         }
         interceptors.add(invocationContextInterceptor);
      }
      else
      {
         interceptors.addAll(Arrays.asList(stack.createInterceptors(advisor, null)));
      }

      // 12.7 footnote 57: ignore method level interceptors
      // The lifecycle callbacks on the interceptors must be invoked in order
      for(Class<?> interceptorClass : lifecycleInterceptorClasses)
      {
         ExtendedAdvisor interceptorAdvisor = ExtendedAdvisorHelper.getExtendedAdvisor(advisor);
         HashSet<Class<?>> classes = null;
         // Get all public/private/protected/package access methods of signature:
         // void <MethodName> (InvocationContext)
         Method[] possibleLifecycleInterceptorMethods = ClassHelper.getMethods(interceptorClass, void.class, new Class<?>[]{InvocationContext.class});
         for(Method interceptorMethod : possibleLifecycleInterceptorMethods)
         {
            // Now check if the lifecycle annotation is present
            if(interceptorAdvisor.isAnnotationPresent(interceptorClass, interceptorMethod, lifecycleAnnotationType)) //For Xml this returns true sometimes
            {
               // And finally consider it only if it's not overriden
               if (!ClassHelper.isOverridden(interceptorClass, interceptorMethod))
               {
                  classes = checkClass(classes, interceptorMethod, advisor, lifecycleAnnotationType);
                  interceptors.add(new LifecycleCallbackInterceptorMethodLazyInterceptor(interceptorClass, interceptorMethod));
               }
            }

         }

      }

      // Bean lifecycle callbacks
      Class<?> beanClass = advisor.getClazz();
      HashSet<Class<?>> classes = null;
      // Get all public/private/protected/package access methods of signature:
      // void <MethodName> ()
      Method[] possibleLifecycleMethods = ClassHelper.getMethods(beanClass, void.class, new Class<?>[] {});
      for(Method beanMethod : possibleLifecycleMethods)
      {
         // Now check if the method is marked with a lifecycle annotation
         if(advisor.hasAnnotation(beanMethod, lifecycleAnnotationType))
         {
            // And finally consider it only if it's not overriden
            if (!ClassHelper.isOverridden(beanClass, beanMethod))
            {
               classes = checkClass(classes, beanMethod, advisor, lifecycleAnnotationType);
               interceptors.add(new LifecycleCallbackBeanMethodInterceptor(beanMethod));

            }
         }

      }

      return interceptors.toArray(new Interceptor[0]);
   }

   private static String toString(AspectManager manager)
   {
      if(manager instanceof Domain)
         return "domain '" + ((Domain) manager).getDomainName() + "'";
      return manager.toString();
   }   
}
