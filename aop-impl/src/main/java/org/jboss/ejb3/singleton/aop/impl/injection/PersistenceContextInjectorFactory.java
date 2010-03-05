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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceContext;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.container.spi.injection.EJBContainerENCInjector;
import org.jboss.ejb3.container.spi.injection.InjectorFactory;
import org.jboss.ejb3.container.spi.injection.InstanceInjector;
import org.jboss.injection.InjectionUtil;
import org.jboss.jpa.resolvers.PersistenceUnitDependencyResolver;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;

/**
 * PersistenceContextInjector
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class PersistenceContextInjectorFactory implements InjectorFactory
{

   private DeploymentUnit deploymentUnit;

   private PersistenceUnitDependencyResolver persistenceUnitResolver;

   public PersistenceContextInjectorFactory(DeploymentUnit du, PersistenceUnitDependencyResolver puResolver)
   {
      this.deploymentUnit = du;
      this.persistenceUnitResolver = puResolver;
   }

   public List<InstanceInjector> createBeanInstanceInjectors(JBossEnterpriseBeanMetaData enterpriseBeanMetaData)
   {
      //      PersistenceContextReferencesMetaData pcRefs = enterpriseBeanMetaData.getPersistenceContextRefs();
      //      if (pcRefs == null || pcRefs.isEmpty())
      //      {
      //         return Collections.EMPTY_LIST;
      //      }
      //      List<EJBInjector> injectors = new ArrayList<EJBInjector>();
      //      for (PersistenceContextReferenceMetaData pcRef : pcRefs)
      //      {
      //         String unitName = pcRef.getPersistenceUnitName();
      //         String puSupplier = this.persistenceUnitResolver.resolvePersistenceUnitSupplier(this.deploymentUnit, unitName);
      //         injectors.add(new PersistenceContextEncInjector("env/" + pcRef.getPersistenceContextRefName(), puSupplier,
      //               pcRef.getPersistenceContextType()));
      //      }
      //      return injectors;
      return Collections.EMPTY_LIST;
   }

   @Override
   public InstanceInjector createInstanceInjector(Method method)
   {
      PersistenceContext pc = method.getAnnotation(PersistenceContext.class);
      if (pc == null)
      {
         return null;
      }
      String encName = pc.name();
      if (encName == null || encName.equals(""))
      {
         encName = InjectionUtil.getEncName(method);
      }
      else
      {
         encName = "env/" + pc.name();
      }
      return new ENCMethodInjector(method, encName);

   }

   /**
    * @see org.jboss.ejb3.container.spi.injection.InjectorFactory#createENCInjectors(org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData)
    */
   @Override
   public List<EJBContainerENCInjector> createENCInjectors(JBossEnterpriseBeanMetaData enterpriseBeanMetaData)
   {
      // todo
      return Collections.EMPTY_LIST;
   }

   

   /**
    * @see org.jboss.ejb3.container.spi.injection.InjectorFactory#createENCInjectors(java.lang.reflect.Method)
    */
   @Override
   public EJBContainerENCInjector createENCInjector(Method method)
   {
      PersistenceContext pc = method.getAnnotation(PersistenceContext.class);
      if (pc == null)
      {
         return null;
      }
      String unitName = pc.unitName();
      String puSupplier = this.persistenceUnitResolver.resolvePersistenceUnitSupplier(this.deploymentUnit, unitName);
      String encName = pc.name();
      if (encName == null || encName.equals(""))
      {
         encName = InjectionUtil.getEncName(method);
      }
      else
      {
         encName = "env/" + pc.name();
      }
      PersistenceContextEncInjector pcENCInjector = new PersistenceContextEncInjector(encName, puSupplier, pc.type());
      pcENCInjector.addDependency(puSupplier);
      
      return pcENCInjector;
   }

}
