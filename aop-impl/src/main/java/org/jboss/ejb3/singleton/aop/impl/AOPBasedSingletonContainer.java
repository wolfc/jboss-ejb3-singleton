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
package org.jboss.ejb3.singleton.aop.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.ejb.Handle;
import javax.ejb.TimerService;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;

import org.jboss.aop.Advisor;
import org.jboss.aop.Domain;
import org.jboss.aop.MethodInfo;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.InvocationResponse;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.aop.util.MethodHashing;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Ejb3Module;
import org.jboss.ejb3.aop.BeanContainer;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.metadata.annotation.AnnotationRepositoryToMetaData;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.impl.remoting.SessionSpecRemotingMetadata;
import org.jboss.ejb3.session.SessionSpecContainer;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.AccessTimeoutMetaDataBridge;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.ConcurrencyTypeMetaDataBridge;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.LockMetaDataBridge;
import org.jboss.ejb3.singleton.impl.container.SingletonContainer;
import org.jboss.injection.inject.spi.Injector;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

/**
 * <p>
 * A singleton container based on AOP. This container is used to integrate the {@link EJBContainer} with the
 * existing AOP based container framework (mainly the AOP interceptors). Most of the work in this container
 * is delegated to the {@link SingletonContainer}. 
 * </p>
 * <p>
 * The main purpose of this container is to act as an entry point to invocations and plug-in a 
 * AOP based interceptor registry {@link AOPBasedInterceptorRegistry} into the {@link SingletonContainer}
 * </p>
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedSingletonContainer extends SessionSpecContainer implements EJBContainer
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(AOPBasedSingletonContainer.class);

   /**
    * This is the container to which the {@link AOPBasedSingletonContainer} will
    * delegate the calls to
    */
   private SingletonContainer delegate;

   private DeploymentUnit deploymentUnit;

   /**
    * Returns the AOP domain name which this container uses
    * for AOP based processing
    * @return
    */
   public static String getAOPDomainName()
   {
      return "Singleton Bean";
   }

   /**
    * @param cl
    * @param beanClassName
    * @param ejbName
    * @param domain
    * @param ctxProperties
    * @param deployment
    * @param beanMetaData
    * @throws ClassNotFoundException
    */
   public AOPBasedSingletonContainer(ClassLoader cl, String beanClassName, String ejbName, Domain domain,
         Hashtable ctxProperties, JBossSessionBean31MetaData beanMetaData, DeploymentUnit du)
         throws ClassNotFoundException
   {
      this(cl, beanClassName, ejbName, domain, ctxProperties, beanMetaData);
      this.deploymentUnit = du;

   }

   public AOPBasedSingletonContainer(ClassLoader cl, String beanClassName, String ejbName, Domain domain,
         Hashtable ctxProperties, JBossSessionBean31MetaData beanMetaData) throws ClassNotFoundException
   {
      super(cl, beanClassName, ejbName, domain, ctxProperties, beanMetaData);
      // create a AOP based interceptor registry which will be used by the container
      InterceptorRegistry interceptorRegistry = new AOPBasedInterceptorRegistry(this);
      // create the new jboss-ejb3-container-spi based singleton container
      this.delegate = new SingletonContainer(this.getBeanClass(), beanMetaData, interceptorRegistry);

   }

   /**
    * This method first setups the common metadata bridges, by calling super.initMetaDataBasedAnnotationRepository().
    * It then adds the following metadata bridges to the {@link AnnotationRepositoryToMetaData}:
    * <ul>
    *   <li> {@link ConcurrencyTypeMetaDataBridge} </li>
    *   <li> {@link LockMetaDataBridge} </li>
    *   <li> {@link AccessTimeoutMetaDataBridge} </li>
    * </ul>
    * @see org.jboss.ejb3.EJBContainer#initMetaDataBasedAnnotationRepository()
    * 
    */
   @Override
   protected void initMetaDataBasedAnnotationRepository()
   {
      super.initMetaDataBasedAnnotationRepository();
      // setup singleton container specific metadata bridges
      this.metadataBasedAnnotationRepo.addMetaDataBridge(new ConcurrencyTypeMetaDataBridge());
      this.metadataBasedAnnotationRepo.addMetaDataBridge(new LockMetaDataBridge());
      this.metadataBasedAnnotationRepo.addMetaDataBridge(new AccessTimeoutMetaDataBridge());
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#create()
    */
   @Override
   public void create() throws Exception
   {
      super.create();
      this.delegate.create();
   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#lockedStart()
    */
   @Override
   protected void lockedStart() throws Exception
   {
      super.lockedStart();

      // pass on the control to our simple singleton container
      this.delegate.start();
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#initializePool()
    */
   @Override
   protected void initializePool() throws Exception
   {
      // do nothing (we don't have a pool)
   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#lockedStop()
    */
   @Override
   protected void lockedStop() throws Exception
   {
      super.lockedStop();
      this.delegate.stop();
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#destroy()
    */
   @Override
   public void destroy() throws Exception
   {
      this.delegate.destroy();
      // let the super do the rest
      super.destroy();
   }

   /**
    * @see org.jboss.ejb3.session.SessionContainer#createSession(java.lang.Class<?>[], java.lang.Object[])
    */
   @Override
   public Serializable createSession(Class<?>[] initParameterTypes, Object[] initParameterValues)
   {
      // no sessions for @Singleton
      return null;
   }

   /**
    * @see org.jboss.ejb3.session.SessionContainer#dynamicInvoke(org.jboss.aop.joinpoint.Invocation)
    */
   @Override
   public InvocationResponse dynamicInvoke(Invocation invocation) throws Throwable
   {
      /*
       * Obtain the target method (unmarshall from invocation)
       */

      // Cast
      assert invocation instanceof MethodInvocation : AOPBasedSingletonContainer.class.getName()
            + ".dynamicInoke supports only " + MethodInvocation.class.getSimpleName() + ", but has been passed: "
            + invocation.getClass();

      MethodInvocation methodInvocation = (MethodInvocation) invocation;

      // Get the method hash
      long methodHash = methodInvocation.getMethodHash();
      if (logger.isTraceEnabled())
      {
         logger.trace("Received dynamic invocation for method with hash: " + methodHash);
      }

      // Get the Method via MethodInfo from the Advisor
      Advisor advisor = this.getAdvisor();
      MethodInfo methodInfo = advisor.getMethodInfo(methodHash);

      // create a container invocation
      AOPBasedContainerInvocation containerInvocation = new AOPBasedContainerInvocation(methodInfo,
            methodInvocation.getArguments());
      try
      {
         // TODO: This is legacy code copied from StatelessContainer/SessionSpecContainer of ejb3-core
         // Get the invoked method from invocation metadata
         Object objInvokedMethod = invocation.getMetaData(SessionSpecRemotingMetadata.TAG_SESSION_INVOCATION,
               SessionSpecRemotingMetadata.KEY_INVOKED_METHOD);
         assert objInvokedMethod != null : "Invoked Method must be set on invocation metadata";
         assert objInvokedMethod instanceof SerializableMethod : "Invoked Method set on invocation metadata is not of type "
               + SerializableMethod.class.getName() + ", instead: " + objInvokedMethod;
         SerializableMethod invokedMethod = (SerializableMethod) objInvokedMethod;

         // push onto stack
//         SessionSpecContainer.invokedMethod.push(invokedMethod);

         // pass the control to the simple singleton container
         Object result = this.delegate.invoke(containerInvocation);

         // create an InvocationResponse out of the result 
         Map<Object, Object> responseContextInfo = containerInvocation.getResponseContextInfo();
         InvocationResponse invocationResponse = marshallResponse(invocation, result, responseContextInfo);
         return invocationResponse;

      }
      catch (Throwable throwable)
      {
         Map<Object, Object> responseContextInfo = containerInvocation.getResponseContextInfo();
         return marshallException(invocation, throwable, responseContextInfo);
      }
      finally
      {
  //       SessionSpecContainer.invokedMethod.pop();
      }

   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#invoke(java.io.Serializable, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
    */
   @Override
   public Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object[] args)
         throws Exception
   {
      // create a (AOP) MethodInfo first so that a AOP based container invocation can be created out of it
      long hash = MethodHashing.calculateHash(method);
      MethodInfo methodInfo = getAdvisor().getMethodInfo(hash);
      if (methodInfo == null)
      {
         throw new RuntimeException("Method invocation via Proxy could not be found handled for EJB "
               + this.getEjbName() + " : " + method.toString()
               + ", probable error in virtual method registration w/ Advisor for the Container");
      }
      SerializableMethod serializableMethod = new SerializableMethod(method, invokedBusinessInterface);
      // create a container invocation
      ContainerInvocation containerInvocation = new AOPBasedContainerInvocation(methodInfo, args);

      try
      {
         // TODO: Legacy push/pop copied from StatelessContainer/SessionSpecContainer
      //   SessionSpecContainer.invokedMethod.push(serializableMethod);
         // pass the control to the simple singleton container
         return this.delegate.invoke(containerInvocation);

      }
      finally
      {
       //  SessionSpecContainer.invokedMethod.pop();
      }

   }

   /**
    * This method returns null, because binding of proxies into JNDI is done
    * by a separate module, outside of the singleton container implementation
    * 
    * @see org.jboss.ejb3.session.SessionContainer#getJndiRegistrarBindName()
    */
   @Override
   protected String getJndiRegistrarBindName()
   {
      return null;
   }

   /**
    * 
    * This method returns null, because binding of proxies into JNDI is done
    * by a separate module, outside of the singleton container implementation
    * 
    * @see org.jboss.ejb3.session.SessionContainer#getJndiRegistrar()
    */
   @Override
   protected JndiSessionRegistrarBase getJndiRegistrar()
   {
      return null;
   }

   /**
    * @see org.jboss.ejb3.session.SessionContainer#localHomeInvoke(java.lang.reflect.Method, java.lang.Object[])
    */
   @Override
   public Object localHomeInvoke(Method method, Object[] args) throws Throwable
   {
      throw new UnsupportedOperationException("NYI");
   }

   /**
    * @see org.jboss.ejb3.session.SessionContainer#localInvoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
    */
   @Override
   public Object localInvoke(Object id, Method method, Object[] args) throws Throwable
   {
      throw new UnsupportedOperationException("NYI");
   }

   /**
    * @see org.jboss.ejb3.session.SessionContainer#removeHandle(javax.ejb.Handle)
    */
   @Override
   protected void removeHandle(Handle handle) throws Exception
   {
      throw new UnsupportedOperationException("NYI");

   }

   /**
    * @see org.jboss.ejb3.EJBContainer#createBeanContext()
    */
   @Override
   public BeanContext<?> createBeanContext()
   {
      throw new UnsupportedOperationException("createBeanContext() is no longer supported");
   }

   /**
    * @see org.jboss.ejb3.Container#getMBean()
    */
   @Override
   public Object getMBean()
   {
      throw new UnsupportedOperationException("NYI");
   }

   /**
    * @see org.jboss.ejb3.Container#getTimerService()
    */
   @Override
   public TimerService getTimerService()
   {
      throw new UnsupportedOperationException("NYI");
   }

   /**
    * @see org.jboss.ejb3.Container#getTimerService(java.lang.Object)
    */
   @Override
   public TimerService getTimerService(Object key)
   {
      throw new UnsupportedOperationException("NYI");
   }

   /**
    * @see EJBContainer#getEJBName()
    */
   @Override
   public String getEJBName()
   {
      return this.ejbName;
   }

   /**
    * @see EJBContainer#getEJBClass()
    */
   @Override
   public String getEJBClass()
   {
      return this.beanClassName;
   }

   /**
    * @see EJBContainer#getBeanInstanceManager()
    */
   @Override
   public EJBInstanceManager getBeanInstanceManager()
   {
      return this.delegate.getBeanInstanceManager();
   }

   /**
    * @see EJBContainer#invoke(ContainerInvocation)
    */
   @Override
   public Object invoke(ContainerInvocation containerInvocation) throws Exception
   {
         return this.invoke((Serializable) null, containerInvocation.getInvokedBusinessInterface(), containerInvocation
            .getMethod(), containerInvocation.getArgs());
      
   }

   /**
    * @see EJBContainer#getInterceptorRegistry()
    */
   @Override
   public InterceptorRegistry getInterceptorRegistry()
   {
      return this.delegate.getInterceptorRegistry();
   }

   /**
    * Expose this as public so that the {@link AOPBasedInterceptorRegistry}
    * can get hold of the legacy AOP based {@link org.jboss.ejb3.interceptors.registry.InterceptorRegistry} 
    */
   public BeanContainer getBeanContainer()
   {
      return super.getBeanContainer();
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getClassLoader()
    */
   @Override
   public ClassLoader getClassLoader()
   {
      return this.classloader;
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#createObjectName(java.lang.String)
    */
   @Override
   public String createObjectName(String ejbName)
   {
      StringBuilder sb = new StringBuilder(Ejb3Module.BASE_EJB3_JMX_NAME + ",");
      DeploymentUnit ear = this.deploymentUnit == null ? null : this.deploymentUnit.getParent();
      if (ear != null)
      {
         sb.append("ear=");
         sb.append(ear.getSimpleName());
         sb.append(",");
      }
      String unitName = this.deploymentUnit == null ? null : this.deploymentUnit.getSimpleName();
      if (unitName == null)
      {
         sb.append("*");
      }
      else
      {
         sb.append("jar=");
         sb.append(unitName);
      }
      sb.append(",name=");
      sb.append(ejbName);
      try
      {
         ObjectName on = new ObjectName(sb.toString());
         return on.getCanonicalName();
      }
      catch (MalformedObjectNameException e)
      {
         throw new RuntimeException(e);
      }

   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#setEJBInjectors(java.util.List)
    */
   @Override
   public void setEJBInjectors(List<Injector<Object>> injectors)
   {
      this.delegate.setEJBInjectors(injectors);

   }

   /**
    * @see org.jboss.ejb3.EJBContainer#getInjectors()
    */
   public List<Injector<Object>> getEJBInjectors()
   {
      return this.delegate.getEJBInjectors();
   }

   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getENC()
    */
   @Override
   public Context getENC()
   {
      return this.delegate.getENC();
   }
   

}
