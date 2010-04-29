/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.singleton.impl.resolver;

import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;

/**
 * A resolver which is responsible for resolving an EJB container name from an
 * ejb-link and a {@link DeploymentUnit}
 * <p>
 *  An ejb-link can have the following syntax:
 *  <ul>
 *      <li>abc.jar#XYZ : where abc.jar is the module name within which the bean is present, and XYZ is the bean name</li>
 *      <li>XYZ : Where XYZ is the bean name</li>
 *  </ul>    
 * </p>
 * @author Jaikiran Pai
 *
 */
public class EjbLinkResolver
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(EjbLinkResolver.class);
   
   /**
    * Returns the ejb container name for the passed <code>ejbLink</code>. The container name
    * is resolved using the {@link JBossMetaData} found in the <code>deploymentUnit</code>.
    * <p>
    *   This method first obtains the top level deployment unit for the passed <code>deploymentUnit</code>
    *   through a call to {@link DeploymentUnit#getTopLevel()}. It then starts searching for an appropriate
    *   EJB, within the top level deployment unit and its child deployment units, until it find one.
    * </p>
    * <p>
    *   This method will return null, if the container name cannot be resolved.
    * </p>
    * @param ejbLink The ejbLink which has to be resolved
    * @param deploymentUnit The deployment unit within which the container has to be scanned for
    * @return
    */
   public String resolveEjbContainerName(String ejbLink, DeploymentUnit deploymentUnit)
   {
      // get the top level DU
      DeploymentUnit topLevelDeploymentUnit = deploymentUnit.getTopLevel();
      
      // now start looking for the EJB corresponding to the ejb-link within the
      // top level deployment unit
      JBossMetaData jbossMetaData = this.getMetaData(topLevelDeploymentUnit);
      String ejbContainerName = this.getEjbContainerName(ejbLink, jbossMetaData, topLevelDeploymentUnit);
      // not yet resolved, so check in child deployment units
      if (ejbContainerName == null)
      {
         List<DeploymentUnit> childUnits = topLevelDeploymentUnit.getChildren();
         if (childUnits == null)
         {
            // no resolution
            return null;
         }
         
         for (DeploymentUnit childUnit : childUnits)
         {
            ejbContainerName = this.resolveEjbContainerName(ejbLink, childUnit);
            // resolved the ejb-link
            if (ejbContainerName != null)
            {
               logger.debug("Resolved container name: " + ejbContainerName + " for ejb-link: " + ejbLink + " in unit " + childUnit);
               return ejbContainerName;
            }
         }
      }
      return ejbContainerName;
   }

   /**
    * Returns the container name corresponding to the passed <code>ejbLink</code>. The container name
    * is obtained from an appropriate enterprise bean within the passed <code>jbossMetaData</code>, through
    * a call to {@link JBossEnterpriseBeanMetaData#getContainerName()}.
    * <p>
    *   An enterprise bean from the {@link JBossMetaData} is considered to be a match, if the name of the 
    *   bean is equal to the name part of the passed <code>ejbLink</code> and if the unit name is equal to the
    *   module name part of the <code>ejbLink</code>. If there is no module name part in the <code>ejbLink</code>,
    *   then only the ejb name is considered.
    *   <p>
    *       For example: If the passed ejbLink is abc.jar#XYZ, then if an enterprise bean's name is XYZ and
    *       the passed unit name is abc.jar, then it is considered to be a match.
    *   </p>    
    * </p>
    * <p>
    *   Note: It is expected that any matching {@link JBossEnterpriseBeanMetaData} will have the correct container name set.
    *   This method just gives a call to {@link JBossEnterpriseBeanMetaData#getContainerName()} on an appropriate instance
    *   of {@link JBossEnterpriseBeanMetaData}
    * </p>
    * @param ejbLink The ejbLink for which the container name has to be found
    * @param jbossMetaData The metadata which will be scanned for any potential matches for the ejbLink
    * @param unit The deployment unit within which the ejb container name is to be looked for
    * @return
    */
   protected String getEjbContainerName(String ejbLink, JBossMetaData jbossMetaData, DeploymentUnit unit)
   {
      if (jbossMetaData == null)
      {
         return null;
      }

      JBossEnterpriseBeansMetaData enterpriseBeans = jbossMetaData.getEnterpriseBeans();
      // no beans == no work!
      if (enterpriseBeans == null || enterpriseBeans.isEmpty())
      {
         return null;
      }
      // check for the presence of the # symbol. If found, then split it into
      // ejb name and module name
      int hashIndex = ejbLink.indexOf('#');
      String moduleName = null;
      String ejbName = null;
      // # found
      if (hashIndex != -1)
      {
         moduleName = ejbLink.substring(0, hashIndex);
         ejbName = ejbLink.substring(hashIndex + 1);
      }
      else
      {
         ejbName = ejbLink;
      }
      // now try and find a match 
      for (JBossEnterpriseBeanMetaData enterpriseBean : enterpriseBeans)
      {
         String potentialMatchEjbName = enterpriseBean.getName();
         if (ejbName.equals(potentialMatchEjbName))
         {
            // ejb-name matches, now let's compare the module name in the ejbLink and the
            // unit's name
            if (moduleName == null || moduleName.equals(unit.getSimpleName()))
            {
               // found a match, return the container name
               return enterpriseBean.getContainerName();
            }
         }
      }
      return null;
   }

   
   /**
    * Obtains the metadata attachment from the specified deployment unit, returning
    * null if not present
    * 
    * @param du
    * @return
    */
   protected JBossMetaData getMetaData(DeploymentUnit du)
   {
      return du.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class);
   }
}
