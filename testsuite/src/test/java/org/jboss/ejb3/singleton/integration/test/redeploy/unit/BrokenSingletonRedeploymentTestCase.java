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
package org.jboss.ejb3.singleton.integration.test.redeploy.unit;

import java.io.File;

import javax.naming.NameNotFoundException;

import junit.framework.Assert;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.redeploy.BrokenSingleton;
import org.jboss.ejb3.singleton.integration.test.redeploy.Cache;
import org.jboss.ejb3.singleton.integration.test.redeploy.GoodSingleton;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * BrokenSingletonRedeploymentTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class BrokenSingletonRedeploymentTestCase extends AbstractSingletonTestCase
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(BrokenSingletonRedeploymentTestCase.class);

   private final String DEPLOYMENT_FILE_NAME = "singleton-redeploy-test.jar";

   @Test
   public void testBrokenDeploymentRedeployment() throws Exception
   {
      // first deploy a broken deployment
      File jar = buildSimpleJar(DEPLOYMENT_FILE_NAME, Cache.class, BrokenSingleton.class);
      logger.info("Deploying broken singleton jar: " + jar.getAbsolutePath());
      try
      {
         this.deploy(jar.toURI().toURL());
      }
      catch (DeploymentException de)
      {
         // expected
         logger.debug("Got the expected deployment exception for broken singleton bean: ", de);
      }

      try
      {
         // now undeploy the broken singleton jar and deploy the good one (simulating
         // a usecase where the user fixes the problem and redeploys the jar)
         logger.info("Undeploying the broken singleton jar: " + jar.toURI().toURL());
         this.undeploy(jar.toURI().toURL());

         jar = buildSimpleJar(DEPLOYMENT_FILE_NAME, Cache.class, GoodSingleton.class);
         logger.info("Deploying singleton jar after replacing the broken singleton bean with a fixed singleton bean: "
               + jar.getAbsolutePath());
         this.deploy(jar.toURI().toURL());

         // just make sure that the bean was deployed successfully
         Cache goodSingleton = (Cache) this.getInitialContext().lookup(GoodSingleton.JNDI_NAME);
         String key = "key";
         String value = "value";
         // we are not testing any singleton semantics, so just making sure that a 
         // invocation on the bean completes successfully, is enough
         goodSingleton.put(key, value);

      }
      finally
      {
         if (jar != null)
         {
            this.undeploy(jar.toURI().toURL());
         }
      }

   }
}
