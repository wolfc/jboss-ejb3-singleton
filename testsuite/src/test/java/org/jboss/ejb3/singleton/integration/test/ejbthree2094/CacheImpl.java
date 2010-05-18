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
package org.jboss.ejb3.singleton.integration.test.ejbthree2094;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * CacheImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote(Cache.class)
@RemoteBinding(jndiBinding = CacheImpl.JNDI_NAME)
public class CacheImpl<K, V> implements Cache<K, V>
{

   public static final String JNDI_NAME = "SimpleSingletonCache";

   private Map<K, V> cache = new HashMap<K, V>();

   @Override
   @Lock(LockType.READ)
   public V get(K key)
   {
      return this.cache.get(key);
   }

   @Override
   @Lock(LockType.WRITE)
   public void put(K key, V value)
   {
      this.cache.put(key, value);

   }

}
