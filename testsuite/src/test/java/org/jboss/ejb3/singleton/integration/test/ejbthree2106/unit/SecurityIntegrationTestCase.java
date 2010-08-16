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
package org.jboss.ejb3.singleton.integration.test.ejbthree2106.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.ejbthree2106.Cache;
import org.jboss.ejb3.singleton.integration.test.ejbthree2106.WrapperSLSB;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the bug fix for EJBTHREE-2106 https://jira.jboss.org/browse/EJBTHREE-2106
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SecurityIntegrationTestCase extends AbstractSingletonTestCase
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
      // build jar
      String ejbJarName = "ejbthree-2106.jar";
      File ejbJar = buildSimpleJar(ejbJarName, WrapperSLSB.class.getPackage());

      this.deployment = ejbJar.toURI().toURL();

      // deploy the ear
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
    * Tests that an invocation on a secured singleton bean works without any errors
    * 
    * @throws Exception
    */
   @Test
   public void testSecuredSingletonBeanAccess() throws Exception
   {
      Cache slsb = (Cache) this.getInitialContext().lookup(WrapperSLSB.JNDI_NAME);

      // setup security credentials
      SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
      securityClient.setSimple("user", "password");
      securityClient.login();

      // invoke the bean
      String val = "Hello world!";
      slsb.setValue(val);

      slsb = (Cache) this.getInitialContext().lookup(WrapperSLSB.JNDI_NAME);

      // invoke again
      String returnedVal = slsb.getValue();

      Assert.assertEquals("Unexpected return value from bean", val, returnedVal);

      // logout
      securityClient.logout();
   }
}
