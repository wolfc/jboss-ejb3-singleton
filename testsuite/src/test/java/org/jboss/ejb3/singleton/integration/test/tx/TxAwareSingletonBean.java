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
package org.jboss.ejb3.singleton.integration.test.tx;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * TxAwareSingletonBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (UserManager.class)
@RemoteBinding (jndiBinding = TxAwareSingletonBean.JNDI_NAME)
public class TxAwareSingletonBean implements UserManager
{

   public static final String JNDI_NAME = "Tx-UserManagerBean";
   
   @PersistenceContext
   private EntityManager em;

   /* (non-Javadoc)
    * @see org.jboss.ejb3.singleton.integration.test.tx.UserManager#createUser(java.lang.String)
    */
   @Override
   public long createUser(String userName)
   {
      User user = new User(userName);
      this.em.persist(user);
      
      return user.getId();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.singleton.integration.test.tx.UserManager#getUser(long)
    */
   @Override
   public User getUser(long id)
   {
      return this.em.find(User.class, id);
   }
   
}
