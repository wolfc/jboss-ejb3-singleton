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
package org.jboss.ejb3.singleton.aop.impl.test.container.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.aop.AspectManager;
import org.jboss.aop.AspectXmlLoader;
import org.jboss.aop.Domain;
import org.jboss.aop.DomainDefinition;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;
import org.jboss.ejb3.singleton.aop.impl.test.container.InVMContainerInvocationImpl;
import org.jboss.ejb3.singleton.aop.impl.test.container.SimpleSingletonBean;
import org.jboss.metadata.annotation.creator.ejb.jboss.JBoss50Creator;
import org.jboss.metadata.annotation.finder.AnnotationFinder;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.naming.JavaCompInitializer;
import org.jboss.reloaded.naming.spi.JavaEEComponent;
import org.jnp.server.SingletonNamingServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * AOPBasedSingletonContainerTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedSingletonContainerTestCase
{

   private Domain singletonAOPDomain;

   private JavaCompInitializer javaCompInitializer;
   
   @Before
   public void beforeTest() throws Exception
   {
      this.bootupNamingServer();
      // load the aop-interceptors
      String interceptorsFilePath = "org/jboss/ejb3/singleton/aop/impl/test/container/singleton-interceptors-aop.xml";
      URL url = Thread.currentThread().getContextClassLoader().getResource(interceptorsFilePath);
      if (url == null)
         throw new IllegalStateException("Can't find " + interceptorsFilePath + " on class loader "
               + Thread.currentThread().getContextClassLoader());
      AspectXmlLoader.deployXML(url);

      DomainDefinition domainDef = AspectManager.instance().getContainer("Singleton Bean");
      if (domainDef == null)
         throw new IllegalArgumentException("Singleton Bean domain not found");
      this.singletonAOPDomain = (Domain) domainDef.getManager();
   }

   private void bootupNamingServer() throws Exception
   {
      SingletonNamingServer namingServer = new SingletonNamingServer();

      this.javaCompInitializer = new JavaCompInitializer();
      javaCompInitializer.start();
   }

   @Test
   public void testSimpleInvocation() throws Exception
   {
      AnnotationFinder<AnnotatedElement> finder = new DefaultAnnotationFinder<AnnotatedElement>();
      JBoss50Creator metadataCreator = new JBoss50Creator(finder);
      Set<Class<?>> classes = new HashSet<Class<?>>();
      classes.add(SimpleSingletonBean.class);
      JBossMetaData metadata = metadataCreator.create(classes);

      Assert.assertNotNull("Metadata created out of class is null", metadata);

      JBossEnterpriseBeanMetaData enterpriseBean = metadata
            .getEnterpriseBean(SimpleSingletonBean.class.getSimpleName());

      Assert.assertNotNull("Metadata was not created for " + SimpleSingletonBean.class, enterpriseBean);
      Assert.assertTrue(SimpleSingletonBean.class + " wasn't considered a session bean ", enterpriseBean.isSession());

      // somewhat OK to cast
      JBossSessionBean31MetaData sessionBeanMetaData = (JBossSessionBean31MetaData) enterpriseBean;
      Assert.assertTrue(SimpleSingletonBean.class + " wasn't considered a singleton bean ", sessionBeanMetaData
            .isSingleton());


      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      String beanClassName = SimpleSingletonBean.class.getName();
      String beanName = SimpleSingletonBean.class.getSimpleName();
      String containerName = "jboss.j2ee:service=EJB3,name=" + beanName;
      sessionBeanMetaData.setContainerName(containerName);
      Hashtable props = new Hashtable();
      AOPBasedSingletonContainer singletonContainer = new AOPBasedSingletonContainer(cl, beanClassName, beanName,
            this.singletonAOPDomain, props, sessionBeanMetaData, Executors.newCachedThreadPool());
      // setup dummy java:/comp
      JavaEEComponent mockJavaEEComponent = mock(JavaEEComponent.class);
      when(mockJavaEEComponent.getContext()).thenReturn(this.javaCompInitializer.getIniCtx());
      singletonContainer.setJavaComp(mockJavaEEComponent);
      
      Method getCountMethod = SimpleSingletonBean.class.getDeclaredMethod("getCount", new Class<?>[]
      {});
      Object[] args = new Object[]
      {};
      // first check that the initial count is 1 (@PostConstruct calls increments it from 0 to 1)
      ContainerInvocation invocation = new InVMContainerInvocationImpl(getCountMethod, args);
      Object result = singletonContainer.invoke(invocation);

      Assert.assertNotNull("Result was null", result);

      int count = (Integer) result;
      Assert.assertEquals("Incorrect count - @PostConstruct was not called", 1, count);

      // now increment the count
      Method method = SimpleSingletonBean.class.getDeclaredMethod("incrementCount", new Class<?>[]
      {});

      invocation = new InVMContainerInvocationImpl(method, args);
      singletonContainer.invoke(invocation);

      // and now again check the count (should now be 2)
      invocation = new InVMContainerInvocationImpl(getCountMethod, args);
      result = singletonContainer.invoke(invocation);

      Assert.assertNotNull("Result was null", result);

      count = (Integer) result;
      Assert.assertEquals("Incorrect count after incrementing", 2, count);
   }
   
   
}
