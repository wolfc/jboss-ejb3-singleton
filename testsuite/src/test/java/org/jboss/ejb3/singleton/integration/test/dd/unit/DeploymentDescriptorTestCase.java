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
package org.jboss.ejb3.singleton.integration.test.dd.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.dd.Counter;
import org.jboss.ejb3.singleton.integration.test.dd.DDBasedSingletonBean;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * DeploymentDescriptorTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DeploymentDescriptorTestCase extends AbstractSingletonTestCase
{
   private static Logger logger = Logger.getLogger(DeploymentDescriptorTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "ddbased-singleton-bean.jar";
      File jar = buildSimpleJar(jarName, DDBasedSingletonBean.class.getPackage());
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
   public void testSingletonBeanAccess() throws Exception
   {
      Counter counter = (Counter) this.getInitialContext().lookup("SingletonCounterBeanJNDIName");

      int initialCount = counter.getCount();

      Assert.assertEquals("Unexpected initial count", 0, initialCount);

      counter.incrementCount();

      Counter anotherCounter = (Counter) this.getInitialContext().lookup("SingletonCounterBeanJNDIName");
      int countAfterIncrement = anotherCounter.getCount();

      Assert.assertEquals("Unexpected count, after increment", 1, countAfterIncrement);

      anotherCounter.decremenetCount();
      int countAfterDecrement = counter.getCount();

      Assert.assertEquals("Unexpected count, after decrement", 0, countAfterDecrement);
   }
}
