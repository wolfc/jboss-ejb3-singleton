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
package org.jboss.ejb3.singleton.aop.impl.context;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.jboss.ejb3.session.SessionBeanContext;
import org.jboss.ejb3.session.SessionContextDelegateBase;
import org.jboss.ejb3.singleton.aop.impl.AOPBasedSingletonContainer;

/**
 * SingletonSessionContext
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonSessionContext extends SessionContextDelegateBase<AOPBasedSingletonContainer>
{

   /**
    * @param beanContext
    */
   public SingletonSessionContext(SessionBeanContext<AOPBasedSingletonContainer> beanContext)
   {
      super(beanContext);
   }

   /**
    * @see org.jboss.ejb3.session.SessionContextDelegateBase#getEJBLocalObject()
    */
   @Override
   public EJBLocalObject getEJBLocalObject() throws IllegalStateException
   {
      throw new IllegalStateException(
            "EJB3.1 Spec violation: Section 4.8.6, Bullet point 4 - Invoking the getEJBLocalObject method is disallowed since a singleton session bean does not support the EJB 2.x Remote client view.");
   }

   /**
    * @see org.jboss.ejb3.session.SessionContextDelegateBase#getEJBObject()
    */
   @Override
   public EJBObject getEJBObject() throws IllegalStateException
   {
      throw new IllegalStateException(
            "EJB3.1 Spec violation: Section 4.8.6, Bullet point 3 - Invoking the getEJBObject method is disallowed since a singleton session bean does not support the EJB 2.x Remote client view.");
   }

   /**
    * @see org.jboss.ejb3.EJBContextImpl#getEJBHome()
    */
   @Override
   public EJBHome getEJBHome()
   {
      throw new IllegalStateException(
            "EJB3.1 Spec violation: Section 4.8.6, Bullet point 3 - Invoking the getEJBHome method is disallowed since a singleton session bean does not support the EJB 2.x Remote client view.");
   }

   /**
    * @see org.jboss.ejb3.EJBContextImpl#getEJBLocalHome()
    */
   @Override
   public EJBLocalHome getEJBLocalHome()
   {
      throw new IllegalStateException(
            "EJB3.1 Spec violation: Section 4.8.6, Bullet point 4 - Invoking the getEJBLocalHome method is disallowed since a singleton session bean does not support the EJB 2.x Remote client view.");
   }

}
