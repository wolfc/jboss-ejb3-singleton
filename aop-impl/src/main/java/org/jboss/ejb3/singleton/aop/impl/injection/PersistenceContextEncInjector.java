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
package org.jboss.ejb3.singleton.aop.impl.injection;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;

import org.jboss.ejb3.container.spi.EJBContainer;
import org.jboss.ejb3.container.spi.injection.EJBContainerENCInjector;
import org.jboss.jpa.deployment.ManagedEntityManagerFactory;
import org.jboss.jpa.spi.PersistenceUnitRegistry;
import org.jboss.jpa.tx.TransactionScopedEntityManager;
import org.jboss.util.naming.Util;

/**
 * PersistenceContextInjector
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class PersistenceContextEncInjector implements EJBContainerENCInjector
{

   private String puSupplierName;

   private String encName;

   private PersistenceContextType type = PersistenceContextType.TRANSACTION;
   
   private List<String> dependencies = new ArrayList<String>();

   public PersistenceContextEncInjector(String jndiname, String unitSupplierName, PersistenceContextType pcType)
   {
      this.encName = jndiname;
      this.puSupplierName = unitSupplierName;
      this.type = pcType;
   }

   public PersistenceContextEncInjector(String jndiname, String unitSupplierName)
   {
      this.encName = jndiname;
      this.puSupplierName = unitSupplierName;

   }

   @Override
   public void inject(EJBContainer container)
   {

      ManagedEntityManagerFactory memf = ((org.jboss.jpa.deployment.PersistenceUnitDeployment) PersistenceUnitRegistry
            .getPersistenceUnit(this.puSupplierName)).getManagedFactory();
      if (this.type == PersistenceContextType.EXTENDED)
      {
         // TODO
         throw new RuntimeException("Injection of PersistenceContextType.EXTENDED not yet implemented");
      }
      else
      {
         EntityManager entityManager = new TransactionScopedEntityManager(memf);
         try
         {

            Util.rebind(container.getENC(), encName, entityManager);
         }
         catch (NamingException ne)
         {
            throw new RuntimeException(ne);
         }
      }
   }
   
   public void addDependency(String dep)
   {
      this.dependencies.add(dep);
   }
   
   public List<String> getDependencies()
   {
      return this.dependencies;
   }
}
