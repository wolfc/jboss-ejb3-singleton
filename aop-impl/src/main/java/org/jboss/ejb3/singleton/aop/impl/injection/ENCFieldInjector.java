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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.ejb3.container.spi.BeanContext;
import org.jboss.ejb3.container.spi.injection.InstanceInjector;

/**
 * ENCMemberInjector
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ENCFieldInjector implements InstanceInjector
{

   private Field field;
   
   private String jndiName;
   
   private List<String> dependencies = new ArrayList<String>();
   
   public ENCFieldInjector(Field field, String jndiName)
   {
      this.field = field;
      this.jndiName = jndiName;
   }
   
   public void inject(BeanContext beanContext, Object instanceToBeInjected)
   {
      Context beanENC = beanContext.getEJBContainer().getENC();
      try
      {
         Object obj = beanENC.lookup(this.jndiName);
         field.set(instanceToBeInjected, obj);
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }
      catch (IllegalArgumentException iae)
      {
         throw new RuntimeException(iae);
      }
      catch (IllegalAccessException iae)
      {
         throw new RuntimeException(iae);
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
