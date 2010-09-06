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
package org.jboss.ejb3.singleton.deployer;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.beans.metadata.api.annotations.Start;
import org.jboss.beans.metadata.api.annotations.Stop;
import org.jboss.util.naming.Util;

/**
 * JNDIBinderImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class JNDIBinderImpl
{

   private Context context;

   private String jndiName;

   private Object jndiObject;
   
   private String description;

   public JNDIBinderImpl(Context ctx, String jndiName, Object objectToBind)
   {
      this.context = ctx;
      this.jndiName = jndiName;
      this.jndiObject = objectToBind;
   }
   
   /**
    * 
    * @param ctx
    * @param jndiName
    * @param objectToBind
    * @param description A brief description of what this JNDI entry is for (for example: "EJB3.x Default Local Business Interface" for
    *                   the jndi binding of default local business interface of a EJB3.x bean) 
    */
   public JNDIBinderImpl(Context ctx, String jndiName, Object objectToBind, String description)
   {
      this(ctx, jndiName, objectToBind);
      this.description = description;
   }

   @Start
   public void start() throws Exception
   {
      this.bind();
   }

   @Stop
   public void stop() throws Exception
   {
      this.unbind();
   }

   public void bind() throws NamingException
   {
      Util.rebind(this.context, this.jndiName, this.jndiObject);
   }

   public void unbind() throws NamingException
   {
      Util.unbind(this.context, this.jndiName);
   }
   
   public String getJNDIName()
   {
      return this.jndiName;
   }
   
   public String getDescription()
   {
      return this.description;
   }
   
}
