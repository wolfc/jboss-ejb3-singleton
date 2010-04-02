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

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * StartupSingletonBeanTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class StartupSingletonBeanTestCase extends AbstractSingletonTestCase
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
      String jarName = "startup-singleton-bean-test.jar";
      File jar = buildSimpleJar(jarName, StartupBeanTester.class.getPackage());
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
   public void testStartup() throws Exception
   {
      StartupBeanTester startupTester = (StartupBeanTester) this.getInitialContext().lookup(StartupBeanTesterImpl.JNDI_NAME);
      boolean wasStartupBeanInvoked = startupTester.wasSingletonLoadedOnStartup();
      
      Assert.assertTrue("Startup singleton bean was not invoked", wasStartupBeanInvoked);
   }
}
