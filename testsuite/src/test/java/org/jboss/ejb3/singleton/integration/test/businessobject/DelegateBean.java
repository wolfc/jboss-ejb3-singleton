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
package org.jboss.ejb3.singleton.integration.test.businessobject;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * DelegateBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote(Delegate.class)
@RemoteBinding (jndiBinding = DelegateBean.JNDI_NAME)
public class DelegateBean implements Delegate
{

   public static final String JNDI_NAME = "BusinessObjectDelegateBean";
   
   @EJB
   private BusinessObjectBean businessObjNoInterfaceView;

   @Override
   public void checkNoInterfaceViewBusinessObjectEquality()
   {
      BusinessObjectBean anotherNoInterfaceViewInstance = this.businessObjNoInterfaceView.getBusinessObject(BusinessObjectBean.class);
      if(!this.businessObjNoInterfaceView.equals(anotherNoInterfaceViewInstance))
      {
         throw new RuntimeException("Business object: " + anotherNoInterfaceViewInstance + " is of unexpected type: "
               + anotherNoInterfaceViewInstance.getClass() + " expected type was: " + BusinessObjectBean.class);
      }
      
   }
   

   @Override
   public void testBusinessObjectValidity(Class<?> businessInterface)
   {
       Object businessObject = this.businessObjNoInterfaceView.getBusinessObject(businessInterface);
       if (businessObject == null)
       {
          throw new RuntimeException("Business object for business interface: " + businessInterface + " is null");
       }
       if (!businessInterface.isInstance(businessObject))
       {
         throw new RuntimeException("Business object: " + businessObject + " is of unexpected type: "
               + businessObject.getClass() + " expected type was: " + businessInterface);
       }
   }
}
