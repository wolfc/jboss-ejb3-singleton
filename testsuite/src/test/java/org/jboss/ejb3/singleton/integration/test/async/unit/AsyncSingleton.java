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

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
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

   @Override
   public Future<String> delayedEcho(String msg)
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

      return new AsyncResult<String>(msg);
   }

   @Override
   public Future<Boolean> lookupTimerService()
   {
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
}
