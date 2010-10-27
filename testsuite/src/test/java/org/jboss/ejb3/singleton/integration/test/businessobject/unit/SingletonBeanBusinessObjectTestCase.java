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
package org.jboss.ejb3.singleton.integration.test.businessobject.unit;

import java.io.File;
import java.net.URL;

import javax.ejb.SessionContext;

import org.jboss.ejb3.singleton.integration.test.businessobject.BusinessObject;
import org.jboss.ejb3.singleton.integration.test.businessobject.BusinessObjectBean;
import org.jboss.ejb3.singleton.integration.test.businessobject.Delegate;
import org.jboss.ejb3.singleton.integration.test.businessobject.DelegateBean;
import org.jboss.ejb3.singleton.integration.test.common.AbstractSingletonTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link SessionContext#getBusinessObject(Class)} on Singleton beans
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanBusinessObjectTestCase extends AbstractSingletonTestCase
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
      String jarName = "businessobject-singleton-bean.jar";
      File jar = buildSimpleJar(jarName, BusinessObjectBean.class.getPackage());
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
    * Tests that the business object returned for a no-interface view of a singleton bean,
    * is equal to the original no-interface view of that bean, on which the {@link SessionContext#getBusinessObject(Class)}
    * was invoked.
    * 
    * @throws Exception
    */
   @Test
   public void testNoInterfaceViewBusinessObjectEquality() throws Exception
   {
      Delegate bean = (Delegate) this.getInitialContext().lookup(DelegateBean.JNDI_NAME);
      bean.checkNoInterfaceViewBusinessObjectEquality();
   }
   
   /**
    * Tests that the business object returned for a singleton bean, through a call to
    * {@link SessionContext#getBusinessObject(Class)} returns an instance of the correct type.
    *  
    * @throws Exception
    */
   @Test
   public void testGetBusinessObject() throws Exception
   {
      Delegate bean = (Delegate) this.getInitialContext().lookup(DelegateBean.JNDI_NAME);
      // test the remote business interface
      bean.testBusinessObjectValidity(BusinessObject.class);
      // test the no-interface view
      bean.testBusinessObjectValidity(BusinessObjectBean.class);
   }

}
