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
package org.jboss.ejb3.singleton.integration.test.tx.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.tx.TxAwareSingletonBean;
import org.jboss.ejb3.singleton.integration.test.tx.User;
import org.jboss.ejb3.singleton.integration.test.tx.UserManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * SingletonBeanTxSemanticsTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanTxSemanticsTestCase extends AbstractSingletonTestCase
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
      String jarName = "singleton-tx-test.jar";
      File jar = buildSimpleJar(jarName, TxAwareSingletonBean.class.getPackage());
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

   /**
    * 
    * @throws Exception
    */
   @Test
   public void testSingletonBeanTx() throws Exception
   {
      UserManager userManager = (UserManager) this.getInitialContext().lookup(TxAwareSingletonBean.JNDI_NAME);
      String userOneName = "User123";
      long userOneId = userManager.createUser(userOneName);

      User userOne = userManager.getUser(userOneId);
      assertNotNull("Singleton user manager bean returned null user for id " + userOneId, userOne);
      assertEquals("Unexpected user returned by singleton user manager bean", userOneName, userOne.getName());
   }
}
