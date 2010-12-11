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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.beans.metadata.api.annotations.Start;
import org.jboss.beans.metadata.api.annotations.Stop;
import org.jboss.logging.Logger;
import org.jboss.util.naming.Util;

/**
 * Binds/Un-binds jndi bindings for a Singleton EJB  
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SingletonBeanJndiBinder
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(SingletonBeanJndiBinder.class);
   
   /**
    * The context to bind to
    */
   private Context context;
   
   /**
    * The jndi bindings
    */
   private List<Binding> bindings = new ArrayList<Binding>();
   
   /**
    * 
    * @param context JNDI context
    */
   public SingletonBeanJndiBinder(Context context)
   {
      this(context, null);
   }
   
   /**
    * 
    * @param context The jndi context to bind to
    * @param bindings The jndi bindings
    */
   public SingletonBeanJndiBinder(Context context, Collection<Binding> bindings)
   {
      this.context = context;
      if (bindings != null)
      {
         this.bindings.addAll(bindings);
      }
   }
   
   /**
    * Returns the jndi bindings which this binder is responsible for binding/un-binding.
    * Returns an empty collection if there are no bindings
    * @return 
    */
   public Collection<Binding> getBindings()
   {
      return this.bindings;
   }
   
   /**
    * Binds the {@link Binding jndi bindings} to jndi context
    * 
    * @throws Exception
    */
   @Start
   public void start() throws NamingException
   {
      for (Binding binding : this.bindings)
      {
         Util.rebind(this.context, binding.getJndiName(), binding.getObject());
      }
   }

   /**
    * Un-binds the {@link Binding jndi bindings} from jndi context
    * 
    * @throws Exception
    */
   @Stop
   public void stop() throws NamingException
   {
      for (Binding binding : this.bindings)
      {
         try
         {
            Util.unbind(this.context, binding.getJndiName());
         }
         catch (NameNotFoundException nnfe)
         {
            logger.debug("***Ignoring*** NameNotFoundException during unbind in singleton bean jndi binder", nnfe);
         }
      }
   }

}
