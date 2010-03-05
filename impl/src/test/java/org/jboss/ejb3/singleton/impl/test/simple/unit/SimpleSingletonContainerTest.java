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
package org.jboss.ejb3.singleton.impl.test.simple.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.container.spi.injection.InstanceInjector;
import org.jboss.ejb3.singleton.impl.container.InVMContainerInvocationImpl;
import org.jboss.ejb3.singleton.impl.container.SingletonContainer;
import org.jboss.ejb3.singleton.impl.test.simple.SimpleSingletonBean;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionType;
import org.junit.Before;
import org.junit.Test;

/**
 * SimpleSingletonContainerTest
 * 
 * Simple tests for the {@link SingletonContainer}
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SimpleSingletonContainerTest
{
   /**
    * Singleton container
    */
   private SingletonContainer singletonContainer;

   /**
    * Setup
    */
   @Before
   public void beforeTest()
   {
      JBossSessionBean31MetaData singletonBeanMetadata = new JBossSessionBean31MetaData();
      singletonBeanMetadata.setEjbClass(SimpleSingletonBean.class.getName());
      singletonBeanMetadata.setEjbName(SimpleSingletonBean.class.getSimpleName());
      singletonBeanMetadata.setSessionType(SessionType.Singleton);

      // create a singleton container
      this.singletonContainer = new SingletonContainer(SimpleSingletonBean.class, singletonBeanMetadata,
            new EmptyInterceptorRegistry());
   }

   /**
    * Tests a simple method invocation on a singleton bean
    * 
    * @throws Exception
    */
   @Test
   public void testSimpleInvocation() throws Exception
   {
      // invoke the "echo" method
      final Method echoMethod = SimpleSingletonBean.class.getMethod("echo", new Class<?>[]
      {String.class});
      String message = "Good morning!";
      final Object params[] = new Object[]
      {message};

      ContainerInvocation containerInvocation = new InVMContainerInvocationImpl(echoMethod, params);

      Object result = this.singletonContainer.invoke(containerInvocation);

      assertNotNull("Result returned by singleton container was null", result);
      assertEquals("Unexpected return type from singleton container", String.class, result.getClass());

      String returnedMessage = (String) result;
      assertEquals("Unexpected message returned from singleton container", message, returnedMessage);
   }

   /**
    * Tests that a singleton container always uses the same single bean instance across multiple calls
    * 
    * @throws Exception
    */
   @Test
   public void testInstanceIdentity() throws Exception
   {
      // multiple calls through the container, should be directed to the 
      // same bean instance
      final Method getMeMethod = SimpleSingletonBean.class.getMethod("getMe", new Class<?>[]
      {});
      final Object params[] = new Object[]
      {};

      ContainerInvocation containerInvocation = new InVMContainerInvocationImpl(getMeMethod, params);
      Object result = this.singletonContainer.invoke(containerInvocation);

      assertNotNull("Result returned by singleton container was null", result);
      assertEquals("Unexpected return type from singleton container", SimpleSingletonBean.class, result.getClass());

      SimpleSingletonBean firstInstance = (SimpleSingletonBean) result;

      // more invocations on the bean and check that the same instance is returned
      for (int i = 0; i < 5; i++)
      {
         containerInvocation = new InVMContainerInvocationImpl(getMeMethod, params);
         result = this.singletonContainer.invoke(containerInvocation);

         assertNotNull("Result returned by singleton container was null", result);
         assertEquals("Unexpected return type from singleton container", SimpleSingletonBean.class, result.getClass());

         SimpleSingletonBean instance = (SimpleSingletonBean) result;

         assertSame("Unexpected bean instance used by singleton container - singleton semantics broken?",
               firstInstance, instance);
      }

   }

   /**
    * Tests that the bean instance maintains the right state after each call
    * @throws Exception
    */
   @Test
   public void testState() throws Exception
   {
      final Method getCountMethod = SimpleSingletonBean.class.getMethod("getCount", new Class<?>[]
      {});
      final Object params[] = new Object[]
      {};
      final Method incrementCountMethod = SimpleSingletonBean.class.getMethod("incrementCount", new Class<?>[]
      {});

      ContainerInvocation containerInvocation = new InVMContainerInvocationImpl(getCountMethod, params);
      Object result = this.singletonContainer.invoke(containerInvocation);

      assertNotNull("Result returned by singleton container was null", result);
      assertEquals("Unexpected return type from singleton container", Integer.class, result.getClass());

      Integer count = (Integer) result;
      assertEquals("Unexpected initial count returned by singleton container", (Integer) 0, count);

      for (int i = 1; i < 10; i++)
      {
         // first increment
         containerInvocation = new InVMContainerInvocationImpl(incrementCountMethod, params);
         this.singletonContainer.invoke(containerInvocation);
         // get the count
         containerInvocation = new InVMContainerInvocationImpl(getCountMethod, params);
         result = this.singletonContainer.invoke(containerInvocation);

         assertNotNull("Result returned by singleton container was null", result);
         assertEquals("Unexpected return type from singleton container", Integer.class, result.getClass());

         count = (Integer) result;
         assertEquals("Unexpected count returned by singleton container", (Integer) i, count);

      }
   }

   private class EmptyInterceptorRegistry implements InterceptorRegistry
   {

      /** (non-Javadoc)
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getEJBContainer()
       */
      @Override
      public EJBContainer getEJBContainer()
      {
         return SimpleSingletonContainerTest.this.singletonContainer;
      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#intercept(org.jboss.ejb3.container.spi.ContainerInvocation, org.jboss.ejb3.container.spi.BeanContext)
       */
      @Override
      public Object intercept(ContainerInvocation containerInvocation, BeanContext targetBeanContext) throws Exception
      {
         Object target = targetBeanContext.getBeanInstance();
         Method methodToInvoke = containerInvocation.getMethod();
         Object[] params = containerInvocation.getArgs();
         return methodToInvoke.invoke(target, params);

      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePostActivate(org.jboss.ejb3.container.spi.BeanContext)
       */
      @Override
      public void invokePostActivate(BeanContext targetBeanContext) throws Exception
      {
         // TODO Auto-generated method stub

      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePostConstruct(org.jboss.ejb3.container.spi.BeanContext)
       */
      @Override
      public void invokePostConstruct(BeanContext targetBeanContext) throws Exception
      {
         // TODO Auto-generated method stub

      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePreDestroy(org.jboss.ejb3.container.spi.BeanContext)
       */
      @Override
      public void invokePreDestroy(BeanContext targetBeanContext) throws Exception
      {
         // TODO Auto-generated method stub

      }

      /**
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#invokePrePassivate(org.jboss.ejb3.container.spi.BeanContext)
       */
      @Override
      public void invokePrePassivate(BeanContext targetBeanContext) throws Exception
      {
         // TODO Auto-generated method stub

      }

      /* (non-Javadoc)
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getInterceptorClasses()
       */
      @Override
      public List<Class<?>> getInterceptorClasses()
      {
         return Collections.EMPTY_LIST;
      }

      /* (non-Javadoc)
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#getInterceptorInjectors()
       */
      @Override
      public Map<Class<?>, List<InstanceInjector>> getInterceptorInjectors()
      {
         // TODO Auto-generated method stub
         return null;
      }

      /* (non-Javadoc)
       * @see org.jboss.ejb3.container.spi.InterceptorRegistry#setInterceptorInjectors(java.util.List)
       */
      @Override
      public void setInterceptorInjectors(Map<Class<?>, List<InstanceInjector>> interceptorInjectors)
      {
         // TODO Auto-generated method stub
         
      }

   }
}
