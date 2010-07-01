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
package org.jboss.ejb3.singleton.integration.test.timerservice;

import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.ejb3.singleton.integration.test.timerservice.unit.SingletonBeanTimerServiceTestCase;
import org.jboss.logging.Logger;

/**
 * Used in {@link SingletonBeanTimerServiceTestCase}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (Scheduler.class)
@RemoteBinding (jndiBinding = SingletonScheduler.JNDI_NAME)
public class SingletonScheduler implements Scheduler
{

   private static Logger logger = Logger.getLogger(SingletonScheduler.class);
   
   public static final String JNDI_NAME = "TimerTestSingletonBean";
   
   private ScheduleTracker scheduleTracker = new ScheduleTracker();
   
   private int maxTimeouts;
   
   @Resource
   private TimerService timerService;
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void schedule(Date initialExpiry, long interval, int maxTimes)
   {
      this.maxTimeouts = maxTimes;
      // create the timer
      this.timerService.createTimer(initialExpiry, interval, null);
   }
   
   /**
    * Handle the timeout
    * @param timer
    */
   @Timeout
   @Lock (LockType.WRITE)
   public void timeout(Timer timer)
   {
      logger.info("Received timeout at: " + new Date() + " for timer " + timer);
      // record the timeout
      this.scheduleTracker.trackTimeout(timer);
      // see if the max number of timeouts has reached. If yes, then
      // cancel any further timeouts
      int timeoutCount = this.scheduleTracker.getTimeoutCount();
      logger.info("Total timeouts so far: " + timeoutCount + " Max allowed " + maxTimeouts);
      if (timeoutCount == maxTimeouts)
      {
         timer.cancel();
         logger.info("Cancelled timer: " + timer);
      }
   }
   
   @Override
   @Lock (LockType.READ)
   public ScheduleTracker getScheduleTracker()
   {
      return this.scheduleTracker;
   }

}
