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
package org.jboss.ejb3.singleton.legacy.container.integration;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.container.spi.ContainerInvocation;

/**
 * AOPBasedContainerInvocationContext
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AOPBasedContainerInvocationContext implements ContainerInvocation
{

   private Method unadvisedMethod;

   private Object[] args;

   private MethodInfo aopMethodInfo;
   
   private Class<?> businessInterface;

   private Map<Object, Object> responseContextInfo = new HashMap<Object, Object>();

   /**
    * @param method
    * @param args
    */
   public AOPBasedContainerInvocationContext(MethodInfo aopMethodInfo, Object[] args)
   {
      this.aopMethodInfo = aopMethodInfo;
      this.args = args;

      // set the unadvised method
      this.unadvisedMethod = this.aopMethodInfo.getUnadvisedMethod();

   }
   
   /**
    * @param method
    * @param args
    */
   public AOPBasedContainerInvocationContext(MethodInfo aopMethodInfo, Object[] args, Class<?> businessInterface)
   {
      this(aopMethodInfo, args);
      this.businessInterface = businessInterface;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getArgs()
    */
   @Override
   public Object[] getArgs()
   {
      return this.args;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getMethod()
    */
   @Override
   public Method getMethod()
   {
      return this.unadvisedMethod;
   }

   
   public MethodInfo getMethodInfo()
   {
      return this.aopMethodInfo;
   }

   public Map<Object, Object> getResponseContextInfo()
   {
      return this.responseContextInfo;
   }
   
   public void setResponseContextInfo(Map<Object, Object> responseContextInfo)
   {
      this.responseContextInfo = responseContextInfo;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getSessionId()
    */
   @Override
   public Serializable getSessionId()
   {
      // singleton beans don't have a session id
      return null;
   }

   /**
    * @see org.jboss.ejb3.container.spi.ContainerInvocation#getInvokedBusinessInterface()
    */
   @Override
   public Class<?> getInvokedBusinessInterface()
   {
      // TODO Auto-generated method stub
      return null;
   }
}
