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
package org.jboss.ejb3.singleton.aop.impl.deployment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.ejb3.DeploymentUnit;
import org.jboss.ejb3.interceptor.InterceptorInfoRepository;
import org.jboss.ejb3.vfs.impl.vfs2.VirtualFileWrapper;
import org.jboss.ejb3.vfs.spi.VirtualFile;
import org.jboss.ejb3.vfs.spi.VirtualFileFilter;

/**
 * MCBasedEJB3DeploymentUnit
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class MCBasedEJB3DeploymentUnit implements DeploymentUnit
{

   private VFSDeploymentUnit vfsDeploymentUnit;

   public MCBasedEJB3DeploymentUnit(VFSDeploymentUnit vfsDeploymentUnit)
   {
      this.vfsDeploymentUnit = vfsDeploymentUnit;
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#addAttachment(java.lang.String, java.lang.Object)
    */
   @Override
   public Object addAttachment(String name, Object attachment)
   {
      return this.vfsDeploymentUnit.addAttachment(name, attachment);
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getAttachment(java.lang.String)
    */
   @Override
   public Object getAttachment(String name)
   {
      return this.vfsDeploymentUnit.getAttachment(name);
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getClassLoader()
    */
   @Override
   public ClassLoader getClassLoader()
   {
      return this.vfsDeploymentUnit.getClassLoader();
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getClasses()
    */
   @Override
   public List<Class> getClasses()
   {
      return Collections.EMPTY_LIST;
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getDefaultEntityManagerName()
    */
   @Override
   public String getDefaultEntityManagerName()
   {
      throw new UnsupportedOperationException("NYI - getDefaultEntityManagerName");
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getDefaultPersistenceProperties()
    */
   @Override
   public Map getDefaultPersistenceProperties()
   {
      throw new UnsupportedOperationException("NYI - getDefaultPersistenceProperties");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getEjbJarXml()
    */
   @Override
   public URL getEjbJarXml()
   {
      return this.extractDescriptorUrl("ejb-jar.xml");
   }

   /**
    * @see org.jboss.ejb3.DeploymentUnit#getInterceptorInfoRepository()
    */
   @Override
   public InterceptorInfoRepository getInterceptorInfoRepository()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getJbossXml()
    */
   @Override
   public URL getJbossXml()
   {
      return this.extractDescriptorUrl("jboss.xml");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getJndiProperties()
    */
   @Override
   public Hashtable getJndiProperties()
   {
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getMetaDataFile(java.lang.String)
    */
   @Override
   public VirtualFile getMetaDataFile(String filename)
   {
      // TODO: revisit this
      org.jboss.virtual.VirtualFile virtualFile =  this.vfsDeploymentUnit.getMetaDataFile(filename);
      VirtualFileWrapper virtualFileWrapper = new VirtualFileWrapper(virtualFile);
      return virtualFileWrapper;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getPersistenceXml()
    */
   @Override
   public URL getPersistenceXml()
   {
      return this.extractDescriptorUrl("persistence.xml");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getRelativePath()
    */
   @Override
   public String getRelativePath()
   {
      return this.vfsDeploymentUnit.getRelativePath();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getRelativeURL(java.lang.String)
    */
   @Override
   public URL getRelativeURL(String path)
   {
      try
      {
         return new URL(path);
      }
      catch (MalformedURLException e)
      {
         try
         {
            if (getUrl() == null)
               throw new RuntimeException("relative <jar-file> not allowed when standalone deployment unit is used");
            return new URL(getUrl(), path);
         }
         catch (Exception e1)
         {
            throw new RuntimeException("could not find relative path: " + path, e1);
         }
      }
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getResourceLoader()
    */
   @Override
   public ClassLoader getResourceLoader()
   {
      return this.getClassLoader();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getResources(org.jboss.virtual.VirtualFileFilter)
    */
   @Override
   public List<VirtualFile> getResources(VirtualFileFilter filter)
   {
      throw new UnsupportedOperationException("NYI");
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getRootFile()
    */
   @Override
   public VirtualFile getRootFile()
   {
      org.jboss.virtual.VirtualFile virtualFile = this.vfsDeploymentUnit.getFile("");
      return new VirtualFileWrapper(virtualFile);
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getShortName()
    */
   @Override
   public String getShortName()
   {
      return this.vfsDeploymentUnit.getFile("").getName();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#getUrl()
    */
   @Override
   public URL getUrl()
   {
      try
      {
         return this.vfsDeploymentUnit.getFile("").toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.DeploymentUnit#removeAttachment(java.lang.String)
    */
   @Override
   public Object removeAttachment(String name)
   {
      return this.vfsDeploymentUnit.removeAttachment(name);
   }

   private URL extractDescriptorUrl(String resource)
   {
      try
      {
         org.jboss.virtual.VirtualFile vf = this.vfsDeploymentUnit.getMetaDataFile(resource);
         if (vf == null) return null;
         return vf.toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
}
