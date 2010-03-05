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
package org.jboss.ejb3.singleton.integration.test.deployment.unit;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.deployment.Counter;
import org.jboss.ejb3.singleton.integration.test.deployment.SimpleSingletonBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * SingletonDeploymentUnitTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonDeploymentUnitTestCase extends AbstractSingletonTestCase
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
      String jarName = "simple-singleton-bean.jar";
      File jar = buildSimpleJar(jarName, SimpleSingletonBean.class.getPackage());
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
   public void testSingletonBeanAccess() throws Exception
   {
      Counter counter = (Counter) this.getInitialContext().lookup(SimpleSingletonBean.JNDI_NAME);
      int initialCount = counter.getCount();
      assertEquals("Unexpected initial counter value - @PostConstruct on singleton bean not called?", 1, initialCount);
      // increment 10 times
      final int NUM_TIMES = 10;
      for (int i = 0; i < NUM_TIMES; i++)
      {
         counter.incrementCount();
      }
      int currentCount = counter.getCount();
      assertEquals("Unexpected counter count after increment", initialCount + NUM_TIMES, currentCount);
   }
}
