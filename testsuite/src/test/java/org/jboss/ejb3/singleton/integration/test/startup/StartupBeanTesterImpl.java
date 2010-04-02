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
package org.jboss.ejb3.singleton.integration.test.startup;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * StartupBeanTesterImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (StartupBeanTester.class)
@RemoteBinding (jndiBinding = StartupBeanTesterImpl.JNDI_NAME)
public class StartupBeanTesterImpl implements StartupBeanTester
{
   public static final String JNDI_NAME = "StartupBeanTesterRemoteBean";
   
   @EJB
   private CallTracker callTracker;
   
   private static Logger logger = Logger.getLogger(StartupBeanTesterImpl.class);

   /**
    * @see org.jboss.ejb3.singleton.integration.test.startup.StartupBeanTester#wasSingletonLoadedOnStartup()
    */
   @Override
   public boolean wasSingletonLoadedOnStartup()
   {
      List<String> whoCalled = this.callTracker.whoCalled();
      if (whoCalled == null || whoCalled.isEmpty())
      {
         return false;
      }
      logger.info("Number of callers " + whoCalled.size());
      if (whoCalled.size() > 1)
      {
         throw new IllegalStateException("Unexpected number of calls = " + whoCalled.size() + " to " + this.callTracker);
      }
      String caller = whoCalled.get(0);
      logger.info("Caller was " + caller);
      if (StartupBean.class.getName().equals(caller))
      {
         return true;
      }
      return false;
   }
   
}
