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
package org.jboss.ejb3.singleton.integration.test.nointerface;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * AccountManagerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote(AccountManager.class)
@RemoteBinding(jndiBinding = AccountManagerBean.JNDI_NAME)
public class AccountManagerBean implements AccountManager
{

   public static final String JNDI_NAME = "AccountManagerSingletonBean";

   private int balance = 0;

   @EJB
   private Calculator calculator;

   /**
    * @see org.jboss.ejb3.singleton.integration.test.nointerface.AccountManager#balance()
    */
   @Override
   public int balance()
   {
      return this.balance;
   }

   /**
    * @see org.jboss.ejb3.singleton.integration.test.nointerface.AccountManager#credit(int)
    */
   @Override
   public void credit(int amount)
   {
      this.balance = this.calculator.add(this.balance, amount);

   }

   /**
    * @see org.jboss.ejb3.singleton.integration.test.nointerface.AccountManager#debit(int)
    */
   @Override
   public void debit(int amount)
   {
      this.balance = this.calculator.subtract(this.balance, amount);

   }

}
