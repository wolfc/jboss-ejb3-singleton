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
package org.jboss.ejb3.singleton.integration.test.ejbthree2094.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.jboss.ejb3.singleton.integration.test.ejbthree2094.Cache;
import org.jboss.ejb3.singleton.integration.test.ejbthree2094.CacheImpl;
import org.jboss.ejb3.singleton.integration.test.ejbthree2094.CachedObject;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the fix for EJBTHREE-2094 bug https://jira.jboss.org/browse/EJBTHREE-2094
 * <p>
 *  {@link AOPBasedSingletonContainer#dynamicInvoke(org.jboss.aop.joinpoint.Invocation)} wasn't
 *  setting the container's classloader as TCCL, which was resulting in a {@link ClassNotFoundException}
 *  during unmarshalling of params.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CustomParamUnitTestCase extends AbstractSingletonTestCase
{

   private static Logger logger = Logger.getLogger(CustomParamUnitTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      // Build and deploy an .ear
      // By default in AS-6, .ears have isolated classloading
      // (so we don't need any custom classloading configuration files)

      String earName = "customparam-test-singletonbean.ear";

      // build jar
      String ejbJarName = "ejb.jar";
      File ejbJar = buildSimpleJar(ejbJarName, CacheImpl.class.getPackage());

      // create the ear 
      File ear = buildSimpleEAR(earName, ejbJar);
      this.deployment = ear.toURI().toURL();

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
    * Tests that invocation on a singleton bean whose method accepts a custom (user application specific)
    * param type, doesn't run into {@link ClassNotFoundException}. 
    * See https://jira.jboss.org/browse/EJBTHREE-2094 for more details.
    * 
    * @throws Exception
    */
   @Test
   public void testSingletonBeanInvocation() throws Exception
   {
      // get the bean
      Cache<String, CachedObject> cache = (Cache<String, CachedObject>) this.getInitialContext().lookup(CacheImpl.JNDI_NAME);
      String key = "Somekey";
      String someName = "Kilroy";
      CachedObject value = new CachedObject(someName);
      // invoke on the bean, by passing an instance of, user app specific class type,
      // as the method param
      cache.put(key, value);
      
      // one more invocation to get back the cached result
      CachedObject returnedVal = cache.get(key);
      
      // test that the returned value is correct
      Assert.assertNotNull("Could not find entry in singleton cache, for key" + key, returnedVal);
      Assert.assertEquals("Unexpected cache value for key " + key, someName, returnedVal.getValue());
   }
}
