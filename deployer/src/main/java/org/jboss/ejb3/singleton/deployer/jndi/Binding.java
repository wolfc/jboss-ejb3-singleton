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
package org.jboss.ejb3.singleton.deployer.jndi;

/**
 * Represents a jndi binding for a Singleton EJB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Binding
{

   /**
    * The jndi name of the binding
    */
   private String jndiName;
   
   /**
    * The object to bind to jndi 
    */
   private Object object;
   
   /**
    * Optional description of jndi object being represented by this {@link Binding binding} 
    */
   private String description;
   
   /**
    * 
    * @param jndiName The jndi name to which the object will be bound
    * @param jndiObject The object to be bound to jndi
    */
   public Binding(String jndiName, Object jndiObject)
   {
      this(jndiName, jndiObject, null);
   }
   
   /**
    * 
    * @param jndiName The jndi name to which the object will be bound
    * @param jndiObject The object to be bound to jndi
    * @param desc Optional description of the jndi object which this {@link Binding binding} represents
    */
   public Binding(String jndiName, Object jndiObject, String desc)
   {
      this.jndiName = jndiName;
      this.object = jndiObject;
      this.description = desc;
   }
   
   /**
    * Returns the jndi name
    * @return
    */
   public String getJndiName()
   {
      return this.jndiName;
   }
   
   /**
    * Returns the object to be bound to jndi
    * @return
    */
   public Object getObject()
   {
      return this.object;
   }
   
   /**
    * Returns the description of the object being bound in jndi
    * @return
    */
   public String getDescription()
   {
      return this.description;
   }
   
   @Override
   public String toString()
   {
      return this.jndiName + "\t\t->" + this.description;
   }
}
