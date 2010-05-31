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

/**
 * Schedule
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public interface Scheduler
{

   /**
    * Creates a timer scheduled to timeout first on <code>initialExpiry</code>
    * and then every <code>interval</code> milliseconds. The timer is going to fire
    * for a maximum of <code>maxTimes</code>
    *  
    * @param initialExpiry Initial timeout
    * @param interval Interval in milliseconds for the timeout
    * @param maxTimes The maximum number of times the timeout has to occur
    */
   void schedule(Date initialExpiry, long interval, int maxTimes);
   
   /**
    * Returns the {@link ScheduleTracker} which can be used to checked timeout status
    * @return
    */
   public ScheduleTracker getScheduleTracker();
}
