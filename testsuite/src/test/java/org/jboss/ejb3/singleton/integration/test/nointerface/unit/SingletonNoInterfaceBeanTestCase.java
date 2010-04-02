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
package org.jboss.ejb3.singleton.integration.test.nointerface.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.nointerface.AccountManager;
import org.jboss.ejb3.singleton.integration.test.nointerface.AccountManagerBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * SingletonNoInterfaceBeanTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonNoInterfaceBeanTestCase extends AbstractSingletonTestCase
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
      String jarName = "singleton-bean-nointerface-test.jar";
      File jar = buildSimpleJar(jarName, AccountManagerBean.class.getPackage());
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
   public void testNoInterfaceAccess() throws Exception
   {
      AccountManager accountManager =  (AccountManager) this.getInitialContext().lookup(AccountManagerBean.JNDI_NAME);
      
      Assert.assertEquals("Incorrect initial balance", 0, accountManager.balance());
      
      accountManager.credit(100);
      Assert.assertEquals("Incorrect balance after credit", 100, accountManager.balance());
      
      accountManager.debit(50);
      Assert.assertEquals("Incorrect balance after debit", 50, accountManager.balance());
   }

}
