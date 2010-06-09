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
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.Handle;
import javax.ejb.Timer;
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
import org.jboss.ejb3.Container;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.aop.BeanContainer;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.common.resolvers.spi.EjbReference;
import org.jboss.ejb3.common.resolvers.spi.EjbReferenceResolver;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.deployers.JBoss5DependencyPolicy;
import org.jboss.ejb3.metadata.annotation.AnnotationRepositoryToMetaData;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.impl.remoting.SessionSpecRemotingMetadata;
import org.jboss.ejb3.resolvers.MessageDestinationReferenceResolver;
import org.jboss.ejb3.session.SessionSpecContainer;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.AccessTimeoutMetaDataBridge;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.ConcurrencyTypeMetaDataBridge;
import org.jboss.ejb3.singleton.aop.impl.concurrency.bridge.LockMetaDataBridge;
import org.jboss.ejb3.singleton.impl.container.SingletonContainer;
import org.jboss.ejb3.singleton.spi.SingletonEJBInstanceManager;
import org.jboss.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.reloaded.naming.spi.JavaEEComponent;

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
public class AOPBasedSingletonContainer extends SessionSpecContainer implements EJBContainer, EJBLifecycleHandler, MultiTimeoutMethodTimedObjectInvoker
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(AOPBasedSingletonContainer.class);

   /**
    * This is the container to which the {@link AOPBasedSingletonContainer} will
    * delegate the calls to
    */
   protected SingletonContainer delegate;

   protected DeploymentUnit deploymentUnit;
   
   protected PersistenceUnitDependencyResolver puResolver;
   
   protected EjbReferenceResolver ejbRefResolver;
   
   protected MessageDestinationReferenceResolver messageDestinationResolver;

   
   protected DependencyPolicy dependencyPolicy;
   
   protected JavaEEComponent javaComp;
   
   protected static final String LIFECYCLE_CALLBACK_STACK_NAME = "SingletonBeanLifecycleCallBackStack";
   
   protected Method timeoutMethod;

   
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
      // HACK
      this.dependencyPolicy = new JBoss5DependencyPolicy(this);
      // create a AOP based interceptor registry which will be used by the container
      InterceptorRegistry interceptorRegistry = new AOPBasedInterceptorRegistry(this);
      // create the new jboss-ejb3-container-spi based singleton container
      this.delegate = new SingletonContainer(this.getBeanClass(), beanMetaData, interceptorRegistry);
      SingletonEJBInstanceManager instanceManager  = new AOPBasedSingletonInstanceManager(this);
      this.delegate.setBeanInstanceManager(instanceManager);

      // init the timeout method
      this.initTimeout();
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
   
//   /**
//    * @see org.jboss.ejb3.EJBContainer#invokeCallback(org.jboss.ejb3.BeanContext, java.lang.Class)
//    */
//   @Override
//   protected void invokeCallback(BeanContext<?> beanContext, Class<? extends Annotation> callbackAnnotationClass)
//   {
//      // it's the BeanContainer's responsibility to invoke the callback
//      // through the correct interceptors. So let's pass the call to the beanContainer
//      this.getBeanContainer().invokeCallback(beanContext, callbackAnnotationClass,LIFECYCLE_CALLBACK_STACK_NAME);
//   }
//   /**
//    * @see org.jboss.ejb3.EJBContainer#pushEnc()
//    */
//   @Override
//   protected void pushEnc()
//   {
//      // noop (we now rely on the new JavaEEComponent based naming)
//   }
//   
//   /**
//    * @see org.jboss.ejb3.EJBContainer#popEnc()
//    */
//   @Override
//   protected void popEnc()
//   {
//      // noop (we now rely on the new JavaEEComponent based naming)
//
//   }
   
   public void setJavaComp(JavaEEComponent javaeeComp)
   {
      this.javaComp = javaeeComp;
   }
   
   /**
    * @see org.jboss.ejb3.EJBContainer#getEnc()
    */
//   @Override
//   public Context getEnc()
//   {
//      if (this.javaComp != null)
//      {
//         return this.javaComp.getContext();
//      }
//      ClassLoader cl = Thread.currentThread().getContextClassLoader();
//      Class<?> interfaces[] = { Context.class };
//      InvocationHandler handler = new InvocationHandler() {
//         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
//         {
//            try
//            {
//               if(javaComp == null)
//                  throw new IllegalStateException("java:comp is not allowed before CREATE of " + AOPBasedSingletonContainer.class.getName());
//               return method.invoke(javaComp.getContext(), args);
//            }
//            catch(InvocationTargetException e)
//            {
//               throw e.getTargetException();
//            }
//         }
//      };
//      return (Context) Proxy.newProxyInstance(cl, interfaces, handler);
//   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#lockedStop()
    */
   @Override
   protected void lockedStop() throws Exception
   {
      this.delegate.stop();
      super.lockedStop();
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

      // Set the TCCL to the container's CL during, required to unmarshalling methods/params from the bean impl class
      ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

      // Set the Container's CL as TCL, required to unmarshall methods from the bean impl class
      Thread.currentThread().setContextClassLoader(this.getClassloader());
      try
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
      }
      finally
      {
         // reset the TCCL to the original CL
         Thread.currentThread().setContextClassLoader(originalLoader);         
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
    * @see org.jboss.ejb3.session.SessionSpecContainer#invoke(java.lang.Object, org.jboss.ejb3.common.lang.SerializableMethod, java.lang.Object[])
    */
   @Override
   public Object invoke(Object proxy, SerializableMethod serializableMethod, Object[] args) throws Throwable
   {
      Class<?> invokedBusinessInterface = this.classloader.loadClass(serializableMethod.getActualClassName());
      Method invokedMethod = serializableMethod.toMethod(this.classloader);
      return this.invoke((Serializable) null, invokedBusinessInterface, invokedMethod, args);
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
      return this.xml.getContainerName();
   }


   /**
    * @see org.jboss.ejb3.container.spi.EJBContainer#getENC()
    */
   @Override
   public Context getENC()
   {
      return this.getEnc();
   }
   
   public PersistenceUnitDependencyResolver getPersistenceUnitResolver()
   {
      return this.puResolver;
   }
   
   public void setPersistenceUnitResolver(PersistenceUnitDependencyResolver puResolver)
   {
      this.puResolver = puResolver;
   }
   
   public void setEjbReferenceResolver(EjbReferenceResolver ejbRefResolver)
   {
      this.ejbRefResolver = ejbRefResolver;
   }
   
   public EjbReferenceResolver getEjbReferenceResolver()
   {
      return this.ejbRefResolver;
   }
   
   public MessageDestinationReferenceResolver getMessageDestinationResolver()
   {
      return messageDestinationResolver;
   }

   public void setMessageDestinationResolver(MessageDestinationReferenceResolver messageDestinationResolver)
   {
      this.messageDestinationResolver = messageDestinationResolver;
   }
   
   /**
    * @see org.jboss.ejb3.EJBContainer#canResolveEJB()
    */
   @Override
   public boolean canResolveEJB()
   {
      // TODO: Revisit this after we have a implementation for 
      // nointerface ejbrefresolver
      return false;
      //return this.ejbRefResolver != null;
   }
   
   @Override
   public String resolveEJB(String link, Class<?> beanInterface, String mappedName)
   {
      if (this.ejbRefResolver == null)
      {
         return null;
      }
      EjbReference reference = new EjbReference(link, beanInterface.getName(), mappedName);
      return this.ejbRefResolver.resolveEjb(this.deploymentUnit, reference);
      
   }
   
   @Override
   public Container resolveEjbContainer(String link, Class businessIntf)
   {
      return null;
   }

   @Override
   public Container resolveEjbContainer(Class businessIntf) 
   {
      return null;
   }
   
   @Override
   public String resolvePersistenceUnitSupplier(String unitName)
   {
      if (this.puResolver == null)
      {
         return null;
      }
      return this.puResolver.resolvePersistenceUnitSupplier(this.deploymentUnit, unitName);
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#resolveMessageDestination(java.lang.String)
    */
   @Override
   public String resolveMessageDestination(String link)
   {
      if (this.messageDestinationResolver == null)
      {
         return null;
      }
      return this.messageDestinationResolver.resolveMessageDestinationJndiName(this.deploymentUnit, link);
   }

   /**
    * @see org.jboss.ejb3.EJBContainer#getDependencyPolicy()
    */
   @Override
   public DependencyPolicy getDependencyPolicy()
   {
      return this.dependencyPolicy;
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#postConstruct(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void postConstruct(org.jboss.ejb3.container.spi.BeanContext beanContext) throws Exception
   {
      if (!(beanContext instanceof org.jboss.ejb3.BeanContext))
      {
         throw new IllegalArgumentException(this.getClass() + " can only handle "
               + org.jboss.ejb3.BeanContext.class + " , was passed " + beanContext.getClass());
      }
      try
      {
         this.pushEnc();
         this.pushContext((BeanContext<?>) beanContext);
  
         this.injectBeanContext((BeanContext<?>) beanContext);
         this.invokePostConstruct((BeanContext<?>) beanContext);
         this.delegate.getInterceptorRegistry().invokePostConstruct(beanContext);

      }
      finally
      {
         this.popContext();
         this.popEnc();
      }
   }

   /**
    * @see org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler#preDestroy(org.jboss.ejb3.container.spi.BeanContext)
    */
   @Override
   public void preDestroy(org.jboss.ejb3.container.spi.BeanContext beanContext) throws Exception
   { 
      if (!(beanContext instanceof org.jboss.ejb3.BeanContext))
      {
         throw new IllegalArgumentException(this.getClass() + " can only handle "
               + org.jboss.ejb3.BeanContext.class + " , was passed " + beanContext.getClass());
      }
      try
      {
         this.pushEnc();
         this.pushContext((BeanContext<?>) beanContext);
         this.invokePreDestroy((BeanContext<?>) beanContext);
         this.delegate.getInterceptorRegistry().invokePreDestroy(beanContext);
      }
      finally
      {
         this.popContext();
         this.popEnc();
      }
   }
   
   /**
    * Init the timeout method (if any) on the bean 
    */
   private void initTimeout()
   {
      NamedMethodMetaData timeoutMethodMetaData = this.getMetaData().getTimeoutMethod();
      if (timeoutMethodMetaData != null)
      {
         this.timeoutMethod = this.getTimeoutCallback(timeoutMethodMetaData, this.getBeanClass());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTimedObjectId()
   {
      return this.getObjectName().getCanonicalName();
   }

   /**
    * Call the timeout method on the bean.
    * 
    * <p>
    *   Internally, this method invokes the timeout method just like
    *   any other bean method invocation (i.e. passes it through the necessary interceptors)
    * </p>
    * {@inheritDoc}
    */
   @Override
   public void callTimeout(Timer timer) throws Exception
   {
      // the method annotated with @Timeout or it's xml equivalent
      if (this.timeoutMethod == null)
      {
         throw new EJBException("No timeout method found for bean " + this.beanClassName);
      }
      // invoke the timeout  
      this.callTimeout(timer, this.timeoutMethod);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected TimedObjectInvoker getTimedObjectInvoker()
   {
      return this;
   }

   /**
    * {@inheritDoc}
    * 
    * @throws IllegalArgumentException If the passed <code>timeoutMethodName</code> or <code>timer</code> is null
    */
   @Override
   public void callTimeout(Timer timer, String timeoutMethodName, String[] timeoutMethodParams) throws Exception
   {
      if (timer == null)
      {
         throw new IllegalArgumentException("Timer instance is null on callTimeout");
      }
      if (timeoutMethodName == null)
      {
         throw new IllegalArgumentException("Timeout method name is null on callTimeout");
      }
      // load the method param classes
      Class<?>[] methodParams = null;
      if (timeoutMethodParams != null)
      {
         methodParams = new Class<?>[timeoutMethodParams.length];
         int i = 0;
         for (String param : timeoutMethodParams)
         {
            Class<?> methodParam = this.classloader.loadClass(param);
            methodParams[i++] = methodParam;
         }
      }
      Method autoTimeoutMethod = null;
      try
      {
         // NOTE: We do *not* do any semantic validations on the timeout method. 
         // Any relevant validations should be done outside the container during metadata
         // creation stage.
         autoTimeoutMethod = this.getBeanClass().getMethod(timeoutMethodName, methodParams);
      }
      catch (NoSuchMethodException nsme)
      {
         logger.error("Timeout method not found for bean " + this.getEjbName() + " for timer " + timer);
         throw nsme;
      }

      this.callTimeout(timer, autoTimeoutMethod);
      
   }
   
   /**
    * Invokes the passed timeout method for the passed {@link Timer}, on a bean instance.
    * 
    * @param timer The {@link Timer} for which the timeout has occurred 
    * @param tMethod The timeout method
    * @throws Exception If any exception occurs during invocation of timeout method on the target bean
    * @throws {@link NullPointerException} If the passed <code>tMethod</code> is null
    */
   private void callTimeout(Timer timer, Method tMethod) throws Exception
   {
      Object[] args =
      {timer};
      if (tMethod.getParameterTypes().length == 0)
         args = null;
      ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
      try
      {
         // invoke
         this.invoke((Serializable) null, (Class<?>) null, tMethod, args);
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldLoader);
      }
   }
}
