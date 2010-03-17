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
package org.jboss.ejb3.singleton.integration.test.concurrency.unit;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.ejb.ConcurrentAccessTimeoutException;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.concurrency.CallRegistry;
import org.jboss.ejb3.singleton.integration.test.concurrency.CallRegistryBean;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * SingletonBeanConcurrencyTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanConcurrencyTestCase extends AbstractSingletonTestCase
{

   private static Logger logger = Logger.getLogger(SingletonBeanConcurrencyTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "singleton-bean-concurrency.jar";
      File jar = buildSimpleJar(jarName, CallRegistryBean.class.getPackage());
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
   public void testReadsInMultipleThreads() throws Exception
   {
      CallRegistry callRegistry = (CallRegistry) this.getInitialContext().lookup(CallRegistryBean.JNDI_NAME);
      String calleeOne = "callee1";
      String calleeTwo = "callee2";
      callRegistry.call(calleeOne, 1);
      callRegistry.call(calleeTwo, 1);

      CountDownLatch latch = new CountDownLatch(2);
      CallLoggingTask callLogger = new CallLoggingTask(callRegistry, latch, 10);
      BackupTask backupTask = new BackupTask(callRegistry, latch, 3);

      backupTask.start();
      callLogger.start();

      latch.await();

      logger.info("Report from " + callLogger + " : ", callLogger.getError());
      Assert.assertNull("Call logging task ran into an error while working with @Read operation on singleton",
            callLogger.getError());

      logger.info("Report from " + backupTask + " : ", backupTask.getError());
      Assert.assertNull("Backup task ran into an error while working with @Read operation on singleton", backupTask
            .getError());

   }

   @Test
   public void testTimeoutWhenWriteInProgress() throws Exception
   {
      CallRegistry callRegistry = (CallRegistry) this.getInitialContext().lookup(CallRegistryBean.JNDI_NAME);
      String calleeOne = "callee1";
      CountDownLatch latch = new CountDownLatch(2);
      CallLoggingTask callLogger = new CallLoggingTask(callRegistry, latch, 1);
      CallerTask caller = new CallerTask(callRegistry, latch, calleeOne, 7000);

      caller.start();
      Thread.sleep(500);
      callLogger.start();

      latch.await();

      Throwable readOperationError = callLogger.getError();
      logger.info("Report from " + callLogger + " : ", readOperationError);
      Assert.assertNotNull("Call logging task performed a Read when a Write operation was on", readOperationError);
      Assert.assertTrue("Unexpected error type from read operation: " + readOperationError,
            (readOperationError instanceof ConcurrentAccessTimeoutException));

      logger.info("Report from " + caller + " : ", caller.getError());
      Assert.assertNull("Caller task failed a write operation", caller.getError());

      List<String> callSequence = callRegistry.getCallSequence();
      Assert.assertNotNull("Call sequence was null, perhaps write operation failed", callSequence);
      Assert.assertEquals("Unexpected number of calls", 1, callSequence.size());
      String callRecepient = callSequence.get(0);
      Assert.assertEquals("Unexpected call made to " + callRecepient, calleeOne, callRecepient);

   }

   private class CallerTask extends Thread
   {
      private CallRegistry callRegistry;

      private CountDownLatch latch;

      private String callee;

      private long durationInMilli;

      private Throwable error;

      public CallerTask(CallRegistry callRegistry, CountDownLatch latch, String callee, long durationInMilliSec)
      {
         this.callRegistry = callRegistry;
         this.latch = latch;
         this.callee = callee;
         this.durationInMilli = durationInMilliSec;
      }

      /**
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         try
         {
            this.callRegistry.call(this.callee, durationInMilli);
         }
         catch (Throwable t)
         {
            this.error = t;
         }
         finally
         {
            this.latch.countDown();
         }
      }

      public Throwable getError()
      {
         return this.error;
      }
   }

   private class BackupTask extends Thread
   {
      private CallRegistry callRegistry;

      private Throwable error;

      private CountDownLatch latch;

      private int numBackups;

      public BackupTask(CallRegistry callregistry, CountDownLatch latch, int numBackups)
      {
         this.callRegistry = callregistry;
         this.latch = latch;
         this.numBackups = numBackups;
      }

      /**
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         try
         {
            for (int i = 0; i < numBackups; i++)
            {
               this.callRegistry.backupCallSequence();
            }
         }
         catch (Throwable t)
         {
            this.error = t;
         }
         finally
         {
            this.latch.countDown();
         }
      }

      public Throwable getError()
      {
         return this.error;
      }
   }

   private class CallLoggingTask extends Thread
   {
      private CallRegistry callRegistry;

      private Throwable error;

      private CountDownLatch latch;

      private int numTimes;

      public CallLoggingTask(CallRegistry callRegistry, CountDownLatch latch, int numTimes)
      {
         this.callRegistry = callRegistry;
         this.latch = latch;
         this.numTimes = numTimes;
      }

      /** 
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         for (int i = 0; i < numTimes; i++)
         {
            try
            {
               List<String> sequence = this.callRegistry.getCallSequence();
               if (sequence != null)
               {
                  for (String callee : sequence)
                  {
                     logger.debug("Call made to : " + callee);
                  }
               }
            }
            catch (Throwable t)
            {
               this.error = t;
            }
            finally
            {
               latch.countDown();
            }
         }

      }

      public Throwable getError()
      {
         return this.error;
      }
   }

}
