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
package org.jboss.ejb3.singleton.integration.test.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * ContainerManagedConcurrentBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@AccessTimeout(value = 2, unit = TimeUnit.SECONDS)
@Remote(CallRegistry.class)
@RemoteBinding(jndiBinding = CallRegistryBean.JNDI_NAME)
public class CallRegistryBean implements CallRegistry
{

   public static final String JNDI_NAME = "CallRegistrySingletonBean";

   private List<String> callSequence = new ArrayList<String>();

   public void call(String recipient, long durationInMilliSec)
   {
      try
      {
         Thread.sleep(durationInMilliSec);
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException(e);
      }
      this.callSequence.add(recipient);
   }

   @Lock(LockType.READ)
   public List<String> getCallSequence()
   {
      return this.callSequence;
   }

   @Lock(LockType.READ)
   public void backupCallSequence()
   {
      try
      {
         Thread.sleep(5000);
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException(e);
      }
      // do nothing
   }

}
