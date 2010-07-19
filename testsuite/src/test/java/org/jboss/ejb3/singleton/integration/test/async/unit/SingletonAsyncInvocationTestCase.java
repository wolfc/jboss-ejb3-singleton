/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.singleton.integration.test.async.unit;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * SingletonAsyncInvocationTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonAsyncInvocationTestCase extends AbstractSingletonTestCase
{

   private static Logger logger = Logger.getLogger(SingletonAsyncInvocationTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "async-singleton-test.jar";
      File jar = buildSimpleJar(jarName, AsyncSingleton.class.getPackage());
      this.deployment = jar.toURI().toURL();
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
   
   @Test
   public void testSimpleAsyncInvocationOnSingletonBean() throws Throwable
   {
      AsyncOps asyncOps = (AsyncOps) this.getInitialContext().lookup(AsyncSingleton.JNDI_NAME);
      String message = "Hello, it's urgent! Please reply";
      Future<String> futureReply = asyncOps.delayedEcho(message);
      
      try
      {
         // wait for a few seconds to get the reply
         String reply = futureReply.get(5000, TimeUnit.SECONDS);
         Assert.assertEquals("Unexpected echo message", message, reply);
      }
      catch (ExecutionException ee)
      {
         // getCause contains the real cause why the operation failed
         throw ee.getCause();
      }
      catch (TimeoutException te)
      {
         throw new RuntimeException("Timed-out waiting for a reply from async method on singleton bean", te);
      }
      
   }
   
   @Test
   public void testTimerServiceLookupInAsyncMethodOfSingletonBean() throws Throwable
   {
      AsyncOps asyncOps = (AsyncOps) this.getInitialContext().lookup(AsyncSingleton.JNDI_NAME);
      
      Future<Boolean> futureReply = asyncOps.lookupTimerService();
      
      try
      {
         // wait for a few seconds to get the reply
         Boolean timerServiceAvailable = futureReply.get(5000, TimeUnit.SECONDS);
         Assert.assertTrue("TimerService wasn't available in async method of singleton bean", timerServiceAvailable);
      }
      catch (ExecutionException ee)
      {
         // getCause contains the real cause why the operation failed
         throw ee.getCause();
      }
      catch (TimeoutException te)
      {
         throw new RuntimeException("Timed-out waiting for a reply from async method on singleton bean", te);
      }
   }
}
