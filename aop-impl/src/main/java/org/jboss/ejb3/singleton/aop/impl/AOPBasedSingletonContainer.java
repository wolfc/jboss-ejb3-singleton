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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.ejb.DependsOn;
import javax.ejb.EJBException;
import javax.ejb.Handle;
import javax.ejb.Timer;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.aop.Advisor;
import org.jboss.aop.Domain;
import org.jboss.aop.MethodInfo;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.InvocationResponse;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.aop.util.MethodHashing;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.Ejb3Registry;
import org.jboss.ejb3.aop.BeanContainer;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.container.spi.ContainerInvocation;
import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.EJBInstanceManager;
import org.jboss.ejb3.container.spi.InterceptorRegistry;
import org.jboss.ejb3.container.spi.lifecycle.EJBLifecycleHandler;
import org.jboss.ejb3.deployers.JBoss5DependencyPolicy;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReference;
import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver;
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
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.reloaded.naming.CurrentComponent;
import org.jboss.reloaded.naming.spi.JavaEEComponent;
import org.jboss.wsf.spi.invocation.integration.InvocationContextCallback;
import org.jboss.wsf.spi.invocation.integration.ServiceEndpointContainer;

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
public class AOPBasedSingletonContainer extends SessionSpecContainer implements EJBContainer, EJBLifecycleHandler, MultiTimeoutMethodTimedObjectInvoker, ServiceEndpointContainer
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(AOPBasedSingletonContainer.class);
   
   private static final String SINGLETON_BEAN_TIMEOUT_METHOD_AOP_INTERCEPTOR_STACK_NAME = "SingletonBeanTimeoutMethodStack";

   /**
    * This is the container to which the {@link AOPBasedSingletonContainer} will
    * delegate the calls to
    */
   protected SingletonContainer delegate;

   protected DeploymentUnit deploymentUnit;
   
   protected PersistenceUnitDependencyResolver puResolver;
   
   private EjbReferenceResolver ejbRefResolver;
   
   protected MessageDestinationReferenceResolver messageDestinationResolver;

   protected JBossSessionBean31MetaData sessionBean31MetaData;
   
   protected DependencyPolicy dependencyPolicy;
   
   protected JavaEEComponent javaComp;
   
   protected static final String LIFECYCLE_CALLBACK_STACK_NAME = "SingletonBeanLifecycleCallBackStack";
   
   protected Method timeoutMethod;

   /**
    * The list of {@link EJBContainer EJBContainers} on which the singleton bean
    * managed by this container, {@link DependsOn @DependsOn}
    */
   private List<EJBContainer> iDependOnSingletonBeanContainers;
   
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
         Hashtable ctxProperties, JBossSessionBean31MetaData beanMetaData, DeploymentUnit du, ExecutorService asyncExecutorService)
         throws ClassNotFoundException
   {
      this(cl, beanClassName, ejbName, domain, ctxProperties, beanMetaData, asyncExecutorService);
      this.deploymentUnit = du;

   }

   public AOPBasedSingletonContainer(ClassLoader cl, String beanClassName, String ejbName, Domain domain,
         Hashtable ctxProperties, JBossSessionBean31MetaData beanMetaData, ExecutorService asyncExecutorService) throws ClassNotFoundException
   {
      super(cl, beanClassName, ejbName, domain, ctxProperties, beanMetaData, asyncExecutorService);
      this.sessionBean31MetaData = beanMetaData;
      // HACK
      this.dependencyPolicy = new JBoss5DependencyPolicy(this);
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

      // create a AOP based interceptor registry which will be used by the container
      InterceptorRegistry interceptorRegistry = new AOPBasedInterceptorRegistry(this);
      // create the new jboss-ejb3-container-spi based singleton container
      this.delegate = new SingletonContainer(this.getBeanClass(), this.sessionBean31MetaData, interceptorRegistry);
      
      // Obtain the SingletonEJBInstanceManager(s) for the @DependsOn
      List<SingletonEJBInstanceManager> dependsOn = null;
      if (this.iDependOnSingletonBeanContainers != null && !this.iDependOnSingletonBeanContainers.isEmpty())
      {
         dependsOn = new ArrayList<SingletonEJBInstanceManager>();
         for (EJBContainer container : this.iDependOnSingletonBeanContainers)
         {
            EJBInstanceManager instanceManager = container.getBeanInstanceManager();
            if (instanceManager instanceof SingletonEJBInstanceManager)
            {
               dependsOn.add((SingletonEJBInstanceManager) instanceManager);
            }
         }
      }
      // create the instance manager
      SingletonEJBInstanceManager instanceManager  = new AOPBasedSingletonInstanceManager(this, this.getBeanInstantiator(), dependsOn);
      this.delegate.setBeanInstanceManager(instanceManager);

      // init the timeout method
      this.initTimeout();

      // let the delegate any of its create work
      this.delegate.create();
      
   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#lockedStart()
    */
   @Override
   protected void lockedStart() throws Exception
   {
      super.lockedStart();

      // org.jboss.ejb3.remoting.IsLocalInterceptor requires the container to be registered with Ejb3Registry
      Ejb3Registry.register(this);

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
    * @see org.jboss.ejb3.EJBContainer#pushEnc()
    */
   @Override
   protected void pushEnc()
   {
      CurrentComponent.push(this.javaComp);
   }
   
   /**
    * @see org.jboss.ejb3.EJBContainer#popEnc()
    */
   @Override
   protected void popEnc()
   {
      JavaEEComponent previousComponent = CurrentComponent.pop();
      if (previousComponent != this.javaComp)
      {
         throw new IllegalStateException("Unexpected ENC context " + previousComponent
               + " popped by EJB container of bean " + this.getBeanClassName());
      }
   }
   
   public void setJavaComp(JavaEEComponent javaeeComp)
   {
      this.javaComp = javaeeComp;
   }
   
   /**
    * @see org.jboss.ejb3.EJBContainer#getEnc()
    */
   @Override
   public Context getEnc()
   {
      // if the java:comp is not yet setup, we return a proxy to javax.naming.Context
      // on a call to getEnc(). This is a really brittle and tricky piece of thing.
      // Any code which calls this method, is *not* expected to invoke 
      // any methods on the returned proxy context, until the java:comp for this container
      // is setup (i.e. the context is unusable until the create() of the container is called).
      // TODO: This hack (and a similar on in org.jboss.ejb3.EJBContainer)
      // MUST be removed once we have a better integration with naming/switchboard 
      if (this.javaComp == null)
      {
         // postpone the inevitable
         ClassLoader tccl = Thread.currentThread().getContextClassLoader();
         Class<?> interfaces[] =
         {Context.class};
         InvocationHandler handler = new InvocationHandler()
         {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
               try
               {
                  if (AOPBasedSingletonContainer.this.javaComp == null)
                  {
                     throw new IllegalStateException(
                           "java:comp is not expected to be used before CREATE of EJB container. Failing bean: "
                                 + AOPBasedSingletonContainer.this.getBeanClassName());
                  }
                  return method.invoke(AOPBasedSingletonContainer.this.javaComp.getContext(), args);
               }
               catch (InvocationTargetException e)
               {
                  throw e.getTargetException();
               }
            }
         };
         return (Context) Proxy.newProxyInstance(tccl, interfaces, handler);
      }
      return this.javaComp.getContext();
   }

   /**
    * @see org.jboss.ejb3.session.SessionSpecContainer#lockedStop()
    */
   @Override
   protected void lockedStop() throws Exception
   {
      this.delegate.stop();
      if (Ejb3Registry.hasContainer(this))
      {
         Ejb3Registry.unregister(this);
      }

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

      // pass the control to the simple singleton container
      return this.delegate.invoke(containerInvocation);

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
   @Deprecated
   public boolean canResolveEJB()
   {
      return this.ejbRefResolver != null;
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
      // pass-on the control to getTimeoutCallback (even if the timeoutMethodMetaData is null).
      // the getTimeoutCallback method can look for a timeout method (like the legacy ejbTimeout(Timer timer) 
      // method) even in the absence of the timeout method metadata.
      this.timeoutMethod = this.getTimeoutCallback(timeoutMethodMetaData, this.getBeanClass());
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
    * Invokes the passed timeout method for the passed {@link Timer}, on a bean instance.
    * 
    * @param timer The {@link Timer} for which the timeout has occurred 
    * @param tMethod The timeout method
    * @throws Exception If any exception occurs during invocation of timeout method on the target bean
    * @throws {@link NullPointerException} If the passed <code>tMethod</code> is null
    */
   @Override
   public void callTimeout(Timer timer, Method tMethod) throws Exception
   {
      Object[] args =
      {timer};
      if (tMethod.getParameterTypes().length == 0)
         args = null;
      ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
      try
      {
         // create a (AOP) MethodInfo first so that a AOP based container invocation can be created out of it
         long hash = MethodHashing.calculateHash(tMethod);
         MethodInfo methodInfo = getAdvisor().getMethodInfo(hash);
         if (methodInfo == null)
         {
            throw new RuntimeException("MethodInfo not found for timeout method " + tMethod.toString()
                  + ", probable error in virtual method registration w/ Advisor for the Container of bean " + this.getEjbName());
         }
         // get hold of the unadvised method, so that we can mark it accessible
         // for the duration of this call (Remember, timeout methods can be with private, protected, package access modifier)
         Method unadvisedMethod = methodInfo.getUnadvisedMethod();
         // mark as accessible before invoking
         unadvisedMethod.setAccessible(true);
         // the timeout method (even if private, protected etc...) should pass through the AOP interceptor
         // chain. Hence we have a specific AOP interceptor stack for timeout method. Get hold of those interceptors
         Interceptor[] timeoutMethodAOPInterceptors = this.getInterceptors(methodInfo.getJoinpoint(),SINGLETON_BEAN_TIMEOUT_METHOD_AOP_INTERCEPTOR_STACK_NAME);
         // create a container invocation
         ContainerInvocation containerInvocation = new AOPBasedContainerInvocation(methodInfo, args, null, timeoutMethodAOPInterceptors);

         // pass the control to the simple singleton container
         this.delegate.invoke(containerInvocation);

      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldLoader);
      }
   }
   
   // TODO: We don't do anything special here, except for making it
   // package protected for use in AOPBasedInterceptorRegistry. This
   // needs a revisit.
   @Override
   public ExecutorService getAsynchronousExecutor()
   {
      return super.getAsynchronousExecutor();
   }
   
   @Override
   public <T> T getBusinessObject(BeanContext<?> beanContext, Class<T> businessInterface) throws IllegalStateException
   {
      if (businessInterface == null)
      {
         throw new IllegalArgumentException("Business interface type cannot be null, for getBusinessObject method call");
      }
      // check the validity of the business interface
      if (!this.isValidBusinessInterface(businessInterface))
      {
         // Ideally, throwing a IllegalArgumentException would have been more appropriate, but the
         // EJB3.1 spec, section 4.8.6 says:
         // "If a session bean instance attempts to invoke a method of the SessionContext interface, and the
         // access is not allowed in Table 3, the container must throw the java.lang.IllegalStateException."
         // and TCK tests expect IllegalStateException. 
         throw new IllegalStateException(businessInterface.getName() + " is not a valid business interface for bean named: " + this.ejbName);
      }
      
      String jndiName = this.resolveEJB(this.ejbName, businessInterface, null);
      if (logger.isTraceEnabled())
      {
         logger.trace("getBusinessObject resolved jndi name: " + jndiName + " for business interface "
               + businessInterface + " for bean named: " + this.ejbName);
      }
      try
      {
         return businessInterface.cast(getInitialContext().lookup(jndiName));
      }
      catch (NamingException ne)
      {
         throw new RuntimeException("Could not get business object for interface type: " + businessInterface + " and bean named : "
               + this.ejbName, ne);
      }
   }
   
   /**
    * Returns true if the passed {@link Class} is a business interface of the 
    * bean, managed by this container. Else returns false.
    * 
    * @param businessInterface The {@link Class} being checked
    * @return
    */
   private boolean isValidBusinessInterface(Class<?> businessInterface)
   {
      if (businessInterface == null)
      {
         throw new IllegalArgumentException("Business interface cannot be null");
      }
      
      boolean isValidBusinessInterface = false;
      // first check business locals
      BusinessLocalsMetaData businessLocals = this.sessionBean31MetaData.getBusinessLocals();
      if (businessLocals != null)
      {
         isValidBusinessInterface = businessLocals.contains(businessInterface.getName());
      }
      // if it's not a valid business local, then check business remotes
      if (!isValidBusinessInterface)
      {
         BusinessRemotesMetaData businessRemotes = this.sessionBean31MetaData.getBusinessRemotes();
         if (businessRemotes != null)
         {
            isValidBusinessInterface = businessRemotes.contains(businessInterface.getName());
         }
      }
      // if it's not a valid business local and neither a valid business remote, then
      // check for the no-interface view
      if (!isValidBusinessInterface)
      {
         isValidBusinessInterface = this.sessionBean31MetaData.isNoInterfaceBean() && this.beanClassName.equals(businessInterface.getName());
      }
      
      return isValidBusinessInterface;
   }
   
   /**
    * Sets the list of {@link EJBContainer EJBContainers} on which the singleton bean 
    * {@link DependsOn @DependsOn}
    * @param containers
    */
   public void setSingletonDependsOn(List<EJBContainer> containers)
   {
      this.iDependOnSingletonBeanContainers = containers;
   }
   
   /**
    * {@inheritDoc}
    * @return
    */
   @Override
   public String getContainerName()
   {
      String containerName = this.getObjectName() != null ? this.getObjectName().getCanonicalName() : null;
      return containerName;
   }
   
   /**
    * {@inheritDoc}
    * @return
    */
   @Override
   public Class getServiceImplementationClass()
   {
      return this.getBeanClass();
   }
   
   /**
    * {@inheritDoc}
    * 
    * @param method
    * @param args
    * @param callback
    * @return
    * @throws Throwable
    */
   @Override
   public Object invokeEndpoint(Method method, Object[] args, InvocationContextCallback callback) throws Throwable
   {
      return this.invoke((Serializable) null, (Class<?>) null, method, args);
   }
}
