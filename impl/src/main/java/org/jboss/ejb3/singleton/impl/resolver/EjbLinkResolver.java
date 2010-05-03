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
 * A resolver which is responsible for resolving an EJB metadata from an
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
    * Returns the {@link JBossEnterpriseBeanMetaData} for the passed <code>ejbLink</code>. The {@link JBossEnterpriseBeanMetaData}
    * is resolved using the {@link JBossMetaData} found in the <code>deploymentUnit</code>.
    * <p>
    *   This method first obtains the top level deployment unit for the passed <code>deploymentUnit</code>
    *   through a call to {@link DeploymentUnit#getTopLevel()}. It then starts searching for an appropriate
    *   EJB, within the top level deployment unit and its child deployment units, until it finds one.
    * </p>
    * <p>
    *   This method will return null, if an appropriate {@link JBossEnterpriseBeanMetaData} cannot be found.
    * </p>
    * @param ejbLink The ejbLink which has to be resolved
    * @param deploymentUnit The deployment unit within which the EJB has to be scanned for
    * 
    * @return
    */
   public JBossEnterpriseBeanMetaData resolveEJB(String ejbLink, DeploymentUnit deploymentUnit)
   {
      // get the top level DU
      DeploymentUnit topLevelDeploymentUnit = deploymentUnit.getTopLevel();
      // resolve from top level DU and its child DUs
      return this.resolveEjbWithinChildDeploymentUnits(ejbLink, topLevelDeploymentUnit);
   }
   
   /**
    * Returns the {@link JBossEnterpriseBeanMetaData} for the passed <code>ejbLink</code>. The {@link JBossEnterpriseBeanMetaData}
    * is resolved using the {@link JBossMetaData} found in the <code>deploymentUnit</code>.
    * <p>
    *   This method will return null, if an appropriate {@link JBossEnterpriseBeanMetaData} cannot be found.
    * </p>
    * 
    * @param The ejbLink which has to be resolved
    * @param deploymentUnit The deployment unit within which the container has to be scanned for
    * @return
    */
   private JBossEnterpriseBeanMetaData resolveEjbWithinChildDeploymentUnits(String ejbLink, DeploymentUnit deploymentUnit)
   {
      // get metadata from the DU
      JBossMetaData jbossMetaData = this.getMetaData(deploymentUnit);
      // first try to resolve in this DU
      JBossEnterpriseBeanMetaData bean = this.getEjbMetaData(ejbLink, jbossMetaData, deploymentUnit);
      
      // not yet resolved, so check in child deployment units
      if (bean == null)
      {
         List<DeploymentUnit> childUnits = deploymentUnit.getChildren();
         if (childUnits == null)
         {
            // no resolution
            return null;
         }
         
         for (DeploymentUnit childUnit : childUnits)
         {
            // try to resolve in child DU
            bean = this.resolveEjbWithinChildDeploymentUnits(ejbLink, childUnit);
            // resolved the ejb-link
            if (bean != null)
            {
               logger.debug("Resolved container name: " + bean.getContainerName() + " for ejb-link: " + ejbLink + " in unit " + childUnit);
               return bean;
            }
         }
      }
      return bean;
   }
   
   /**
    * Returns the {@link JBossEnterpriseBeanMetaData} corresponding to the passed <code>ejbLink</code>. 
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
    * @param ejbLink The ejbLink for which the container name has to be found
    * @param jbossMetaData The metadata which will be scanned for any potential matches for the ejbLink
    * @param unit The deployment unit within which the {@link JBossEnterpriseBeanMetaData} is to be looked for
    * @return
    */
   protected JBossEnterpriseBeanMetaData getEjbMetaData(String ejbLink, JBossMetaData jbossMetaData, DeploymentUnit unit)
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
               return enterpriseBean;
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
