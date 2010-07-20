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

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * AsyncSingleton
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@RemoteBinding(jndiBinding = AsyncSingleton.JNDI_NAME)
@Remote(AsyncOps.class)
public class AsyncSingleton implements AsyncOps
{

   private static Logger logger = Logger.getLogger(AsyncSingleton.class);

   public static final String JNDI_NAME = "AnAsyncSingletonBean";

   @Resource
   private TimerService injectedTimerService;

   @Resource(name = "myTimerService")
   private TimerService timerServiceInjectedAtCustomENCName;

   @EJB
   private ThreadTracker threadTrackingSlowBean;

   @Override
   @Asynchronous
   public Future<String> delayedEcho(String msg)
   {
      // sleep for 2 sec      
      this.sleepFor2Sec();

      return new AsyncResult<String>(msg);
   }

   @Override
   @Asynchronous
   public Future<Boolean> lookupTimerService()
   {
      // sleep for 2 sec
      this.sleepFor2Sec();

      StringBuilder exceptionMessage = new StringBuilder("");
      if (this.injectedTimerService == null)
      {
         exceptionMessage
               .append("\"@Resource private TimerService ...\" was not injected in async method on singleton bean");
         exceptionMessage.append("\n");
      }

      if (this.timerServiceInjectedAtCustomENCName == null)
      {
         exceptionMessage
               .append("\"@Resource (name=\"myTimerService\") private TimerService ...\" was not injected in async method on singleton bean");
         exceptionMessage.append("\n");
      }

      Context ctx = this.getInitialContext();
      // lookup at java:comp/TimerService (the spec mandated ENC location)
      try
      {
         TimerService javaCompTimerService = (TimerService) ctx.lookup("java:comp/TimerService");
         // this shouldn't really happen
         if (javaCompTimerService == null)
         {
            exceptionMessage
                  .append("TimerService was NULL at java:comp/TimerService for async method on singleton bean");
            exceptionMessage.append("\n");
         }

      }
      catch (NameNotFoundException nnfe)
      {
         exceptionMessage.append("TimerService not bound at java:comp/TimerService for async method on singleton bean");
         exceptionMessage.append("\n");
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }

      // lookup at our custom ENC name
      try
      {
         TimerService javaCompEnvMyTimerService = (TimerService) ctx.lookup("java:comp/env/myTimerService");
         // this shouldn't really happen
         if (javaCompEnvMyTimerService == null)
         {
            exceptionMessage
                  .append("TimerService was NULL at java:comp/env/myTimerService for async method on singleton bean");
            exceptionMessage.append("\n");
         }
      }
      catch (NameNotFoundException nnfe)
      {
         exceptionMessage.append("TimerService not bound at java:comp/TimerService for async method on singleton bean");
         exceptionMessage.append("\n");
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }

      // if there was any exception or if timerservice wasn't available, then throw an exception 
      if (exceptionMessage.toString().trim().isEmpty() == false)
      {
         throw new RuntimeException(exceptionMessage.toString());
      }

      return new AsyncResult<Boolean>(true);
   }

   /**
    * Invokes a asynchronous method on a singleton bean, using the singleton bean's 
    * local business interface.
    * Note that this {@link #callAsynchronousMethodOnLocalBusinessInterfaceOfSingleton()} itself
    * is *synchronous* (it just calls an asynchronous method on some other bean)
    */
   @Override
   public void callAsynchronousMethodOnLocalBusinessInterfaceOfSingleton()
   {
      logger.info("Invoking a asynchronous method at: " + new Date());
      // call the asynchronous method on the other singleton bean
      Future<Thread> future = this.threadTrackingSlowBean.getThreadOfExecution();
      
      logger.info("Returned back from asynchronous method at: " + new Date());
      
      // the other singleton bean sleeps for a few seconds, so that returned
      // future shouldn't be DONE yet
      if (future.isDone())
      {
         throw new IllegalStateException(this.threadTrackingSlowBean.getClass().getSimpleName()
               + " bean unexpectedly returned a future which is already DONE");
      }
      Thread beanThread = null;
      try
      {
         // get the actual result
         beanThread = future.get(6, TimeUnit.SECONDS);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      Thread ourThread = Thread.currentThread();
      logger.info("Invocation Thread: " + beanThread);
      logger.info("Out Thread: " + Thread.currentThread());

      // compare our thread and the thread on which the asynchronous method execution
      // took place. They *shouldn't* be the same.
      if (beanThread.equals(ourThread))
      {
         throw new RuntimeException("Invocation of asynchronous method on the local interface of "
               + this.threadTrackingSlowBean.getClass().getSimpleName()
               + " bean, unexpectedly took place on the caller's thread");
      }

      // At this point, since the asynchronous invocation is complete, the future is 
      // expected to be DONE
      if (future.isDone() == false)
      {
         throw new IllegalStateException(
               "Future wasn't marked as DONE, even after completion of invocation on asynchronous method of "
                     + this.threadTrackingSlowBean.getClass().getSimpleName() + " bean");
      }

       
   }

   private Context getInitialContext()
   {
      try
      {
         Context ctx = new InitialContext();
         return ctx;
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
   }

   private void sleepFor2Sec()
   {
      // let's go to sleep, since we are too lazy to just send back an echo
      long sleepTime = 2000;
      logger.info("Sleeping for " + sleepTime + " milli. sec");
      try
      {
         Thread.sleep(sleepTime);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
   }
}
