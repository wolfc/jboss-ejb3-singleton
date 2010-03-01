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
package org.jboss.ejb3.singleton.integration.test.interceptor.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.interceptor.SingletonBeanWithInterceptor;
import org.junit.After;
import org.junit.Before;

/**
 * SingletonBeanInterceptorTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanInterceptorTestCase extends AbstractSingletonTestCase
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
      String jarName = "singleton-bean-interceptor-test.jar";
      File jar = buildSimpleJar(jarName, SingletonBeanWithInterceptor.class.getPackage());
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
   public void testInterception() throws Exception
   {
      SingletonBeanWithInterceptor singleton = (SingletonBeanWithInterceptor) this.getInitialContext().lookup(SingletonBeanWithInterceptor.JNDI_NAME);
      String message = "some message which will be intercepted";
      String expectedInterceptedMessage = message + "-intercepted";
      String result = singleton.echo(message);
      
      Assert.assertNotNull("Singleton bean method invocation returned null", result);
      Assert.assertEquals("Unexpected return message", expectedInterceptedMessage, result);
   }
}
