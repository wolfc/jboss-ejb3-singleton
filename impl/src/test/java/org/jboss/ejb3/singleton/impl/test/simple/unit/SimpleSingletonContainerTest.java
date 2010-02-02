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

import org.jboss.ejb3.container.spi.ContainerInvocationContext;
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
      this.singletonContainer = new SingletonContainer(SimpleSingletonBean.class, singletonBeanMetadata);
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

      ContainerInvocationContext containerInvocation = new DummyContainerInvocation(echoMethod, params);

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

      ContainerInvocationContext containerInvocation = new DummyContainerInvocation(getMeMethod, params);
      Object result = this.singletonContainer.invoke(containerInvocation);

      assertNotNull("Result returned by singleton container was null", result);
      assertEquals("Unexpected return type from singleton container", SimpleSingletonBean.class, result.getClass());

      SimpleSingletonBean firstInstance = (SimpleSingletonBean) result;

      // more invocations on the bean and check that the same instance is returned
      for (int i = 0; i < 5; i++)
      {
         containerInvocation = new DummyContainerInvocation(getMeMethod, params);
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

      ContainerInvocationContext containerInvocation = new DummyContainerInvocation(getCountMethod, params);
      Object result = this.singletonContainer.invoke(containerInvocation);

      assertNotNull("Result returned by singleton container was null", result);
      assertEquals("Unexpected return type from singleton container", Integer.class, result.getClass());

      Integer count = (Integer) result;
      assertEquals("Unexpected initial count returned by singleton container", (Integer) 0, count);

      for (int i = 1; i < 10; i++)
      {
         // first increment
         containerInvocation = new DummyContainerInvocation(incrementCountMethod, params);
         this.singletonContainer.invoke(containerInvocation);
         // get the count
         containerInvocation = new DummyContainerInvocation(getCountMethod, params);
         result = this.singletonContainer.invoke(containerInvocation);

         assertNotNull("Result returned by singleton container was null", result);
         assertEquals("Unexpected return type from singleton container", Integer.class, result.getClass());

         count = (Integer) result;
         assertEquals("Unexpected count returned by singleton container", (Integer) i, count);

      }
   }

   /**
    * 
    * DummyContainerInvocation
    *
    * A dummy {@link ContainerInvocationContext} used in tests
    * @author Jaikiran Pai
    * @version $Revision: $
    */
   private class DummyContainerInvocation implements ContainerInvocationContext
   {

      private Method method;

      private Object[] args;

      private boolean stateful;

      public DummyContainerInvocation(Method method, Object[] args)
      {
         this.method = method;
         this.args = args;
      }

      public DummyContainerInvocation(Method method, Object[] args, boolean isStateful)
      {
         this(method, args);
         this.stateful = isStateful;
      }

      /**
       * @see org.jboss.ejb3.container.spi.ContainerInvocationContext#getArgs()
       */
      @Override
      public Object[] getArgs()
      {

         return this.args;
      }

      /**
       * @see org.jboss.ejb3.container.spi.ContainerInvocationContext#getMethod()
       */
      @Override
      public Method getMethod()
      {
         return this.method;
      }

      
   }
}
