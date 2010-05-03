/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.singleton.integration.test.dependson.unit;

import java.io.File;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.dependson.jarone.SingletonInJarOne;
import org.jboss.ejb3.singleton.integration.test.dependson.jartwo.SingletonInJarTwo;
import org.jboss.ejb3.singleton.integration.test.dependson.somejar.CallTracker;
import org.jboss.ejb3.singleton.integration.test.dependson.somejar.CallTrackerBean;
import org.jboss.ejb3.singleton.integration.test.dependson.somejar.Echo;
import org.jboss.ejb3.singleton.integration.test.dependson.somejar.SingletonBeanA;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the depends-on functionality of singleton beans
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DependsOnTestCase extends AbstractSingletonTestCase
{

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String earName = "singleton-depends-on-test.ear";

      // build jarOne
      String jarOneName = "jarOne.jar";
      File jarOne = buildSimpleJar(jarOneName, SingletonInJarOne.class.getPackage());

      // build jarTwo
      String jarTwoName = "jarTwo.jar";
      File jarTwo = buildSimpleJar(jarTwoName, SingletonInJarTwo.class.getPackage());

      // build the third jar
      String oneMoreJarName = "somejar.jar";
      File oneMoreJar = buildSimpleJar(oneMoreJarName, SingletonBeanA.class.getPackage());

      // create the ear out of these jars
      File ear = buildSimpleEAR(earName, jarOne, jarTwo, oneMoreJar);
      this.deployment = ear.toURI().toURL();

      // deploy the ear
      this.redeploy(deployment);
   }

   @After
   public void after() throws Exception
   {
      if (this.deployment != null)
      {
         this.undeploy(deployment);
      }
   }

   /**
    * Test that the depends-on works as expected for singleton beans belonging to the same jar file.
    * 
    * @throws Exception
    */
   @Test
   public void testSimpleDependsOn() throws Exception
   {
      Echo echoBean = (Echo) this.getInitialContext().lookup(SingletonBeanA.JNDI_NAME);
      String msg = "Hello";
      String returnMsg = echoBean.echo(msg);

      Assert.assertEquals(msg, returnMsg);
   }

   /**
    * Test that the depends-on works as expected for singleton beans across jar files,  but
    * in the same .ear file.
    * <p>
    *  {@link CallTrackerBean} tracks the load sequence of two startup singleton beans {@link SingletonInJarOne}
    *  and {@link SingletonInJarTwo}. {@link SingletonInJarTwo} is expected to be loaded first because
    *  {@link SingletonInJarOne} depends-on {@link SingletonInJarTwo} 
    * </p>
    * @throws Exception
    */
   @Test
   public void testDependsOnAcrossJars() throws Exception
   {
      CallTracker callTracker = (CallTracker) this.getInitialContext().lookup(CallTrackerBean.JNDI_NAME);
      List<String> callSequence = callTracker.getCallSequence();

      Assert.assertNotNull("No call sequence found", callSequence);
      Assert.assertEquals("Unexpected number of calls to call tracker", 2, callSequence.size());

      String firstCaller = callSequence.get(0);
      Assert.assertNotNull("First caller was null", firstCaller);
      Assert.assertEquals("Unexpected order of call to call tracker", SingletonInJarTwo.class.getName(), firstCaller);

      String secondCaller = callSequence.get(1);
      Assert.assertNotNull("Second caller was null", secondCaller);
      Assert.assertEquals("Unexpected order of call to call tracker", SingletonInJarOne.class.getName(), secondCaller);

   }
}
