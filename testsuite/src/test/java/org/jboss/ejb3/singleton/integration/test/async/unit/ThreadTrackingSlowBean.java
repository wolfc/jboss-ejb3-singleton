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

import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.logging.Logger;

/**
 * SlowEchoBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Local(ThreadTracker.class)
public class ThreadTrackingSlowBean implements ThreadTracker
{

   private static Logger logger = Logger.getLogger(ThreadTrackingSlowBean.class);

   /**
    * A {@link Asynchronous} method which returns the current thread of execution. Sleep for a few seconds, before
    * returning the result
    */
   @Asynchronous
   public Future<Thread> getThreadOfExecution()
   {
      this.sleepFor5Sec();

      return new AsyncResult<Thread>(Thread.currentThread());
   }

   private void sleepFor5Sec()
   {
      // let's go to sleep
      long sleepTime = 5000;
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
