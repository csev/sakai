/***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.archive.impl;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import lombok.Setter;

import org.apache.commons.codec.binary.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.sakaiproject.archive.api.ArchiveService;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.Xml;

@Slf4j
@Setter
public class SiteMerger {

	private AuthzGroupService authzGroupService;
	private UserDirectoryService userDirectoryService;
	private SiteService siteService;
	private SecurityService securityService;
	private EntityManager entityManager;

	// tool id updates
	// TODO: Do we need to swap these any more?
	private String old_toolId_prefix = "chef.";
	private String new_toolId_prefix = "sakai.";
	private String[] old_toolIds = {"sakai.noti.prefs", "sakai.presence", "sakai.siteinfogeneric", "sakai.sitesetupgeneric", "sakai.threadeddiscussion"};
	private String[] new_toolIds = {"sakai.preferences", "sakai.online", "sakai.siteinfo", "sakai.sitesetup", "sakai.discussion"};
	
	//SWG TODO I have a feeling this is a bug
	protected Set<String> usersListAllowImport = new HashSet<>(); 
	/**
	* Process a merge for the file, or if it's a directory, for all contained files (one level deep).
	* @param fileName The site name (for the archive file) to read from.
	* @param mergeId The id string to use to make ids in the merge consistent and unique.
	* @param creatorId The creator id
	* If null or blank, the date/time string of the merge is used.
	*/
	//TODO Javadoc this
	public String merge(String fileName, String siteId, String creatorId, String m_storagePath,
						boolean filterSakaiServices, String[] filteredSakaiServices, boolean filterSakaiRoles, String[] filteredSakaiRoles)
	{
		StringBuilder results = new StringBuilder();

		File[] files = null;

		// see if the name is a directory
		File archiveFile = new File(m_storagePath + fileName);
		if ((archiveFile == null) || (!archiveFile.exists()))
		{
			results.append("file: " + fileName + " not found.\n");
			log.warn("merge(): file not found: {}", archiveFile.getPath());
			return results.toString();
		} else {
			try {
				// Path outside archive location, discard !
				File baseLocation = new File(m_storagePath);
				if (!archiveFile.getCanonicalPath().startsWith(baseLocation.getCanonicalPath())) {
					throw new Exception();
				}
			} catch (Exception ex) {
				results.append("file: " + fileName + " not permitted.\n");
				log.warn("merge(): file not permitted: {}", archiveFile.getPath());
				return results.toString();
			}
		}

		if (archiveFile.isDirectory())
		{
			files = archiveFile.listFiles();
		}
		else
		{
			files = new File[1];
			files[0] = archiveFile;
		}

		// track old to new attachment names
		Map<String, String> attachmentNames = new HashMap<>();

		// firstly, merge the users
		for (File file : files) {
			if (file != null && file.getPath().indexOf("user.xml") != -1) {
				processMerge(file.getPath(), siteId, results, attachmentNames, null, filterSakaiServices, filteredSakaiServices, filterSakaiRoles, filteredSakaiRoles);
				file = null;
				break;
			}
		}

		// see if there's a site definition
		for (File file : files) {
			if (file != null && file.getPath().indexOf("site.xml") != -1) {
				processMerge(file.getPath(), siteId, results, attachmentNames, creatorId, filterSakaiServices, filteredSakaiServices, filterSakaiRoles, filteredSakaiRoles);
				file = null;
				break;
			}
		}

		// see if there's an attachments definition
		for (File file : files) {
			if (file != null && file.getPath().indexOf("attachment.xml") != -1) {
				processMerge(file.getPath(), siteId, results, attachmentNames, null, filterSakaiServices, filteredSakaiServices, filterSakaiRoles, filteredSakaiRoles);
				file = null;
				break;
			}
		}

		// process each remaining file that is an .xml file
		for (File file : files) {
			if (file != null && file.getPath().endsWith(".xml")) {
				processMerge(file.getPath(), siteId, results, attachmentNames, null, filterSakaiServices, filteredSakaiServices, filterSakaiRoles, filteredSakaiRoles);
			}
		}

		return results.toString();
	}	// merge

	/**
	* Read in an archive file and merge the entries into the specified site.
	* @param fileName The site name (for the archive file) to read from.
	* @param siteId The id of the site to merge the content into.
	* @param results A buffer to accumulate result messages.
	* @param attachmentNames A map of old to new attachment names.
	* @param useIdTrans A map of old WorkTools id to new Ctools id
	* @param creatorId The creator id
	*/
	protected void processMerge(String fileName, String siteId, StringBuilder results, Map<String, String> attachmentNames, String creatorId, boolean filterSakaiService, String[] filteredSakaiService, boolean filterSakaiRoles, String[] filteredSakaiRoles)
	{
		// correct for windows backslashes
		fileName = fileName.replace('\\', '/');

		log.debug("merge(): processing file: {}", fileName);

		Site theSite = null;
		try
		{
			theSite = siteService.getSite(siteId);
		}
		catch (IdUnusedException ignore) {
			log.info("Site not found for id: {}. New site will be created.");
		}

		// read the whole file into a DOM
		Document doc = Xml.readDocument(fileName);
		if (doc == null)
		{
			results.append("Error reading xml from: " + fileName + "\n");
			return;
		}

		// verify the root element
		Element root = doc.getDocumentElement();
		if (!root.getTagName().equals("archive"))
		{
			results.append("File: " + fileName + " does not contain archive xml.  Found this root tag: " + root.getTagName() + "\n");
			return;
		}

		// get the from site id
		String fromSite = root.getAttribute("source");
		String system = root.getAttribute("system");

		// the children
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for(int i = 0; i < length; i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element element = (Element)child;

			// look for site stuff
			if (element.getTagName().equals(SiteService.APPLICATION_ID))
			{	
				mergeSite(siteId, fromSite, element, Collections.EMPTY_MAP, creatorId, filterSakaiRoles, filteredSakaiRoles);
			}

			else
			{
				// we need a site now
				if (theSite == null)
				{
					results.append("Site: " + siteId + " not found.\n");
					return;
				}
				
				String serviceName;
				//Support for attachment.xml created prior to SAK-29456 
				if (element.getTagName().equals(ContentHostingService.APPLICATION_ID)) 
				{
				    serviceName = "org.sakaiproject.content.api.ContentHostingService";
				}
				else {
				// get the service name
				    serviceName = translateServiceName(element.getTagName());
				}

				// get the service
				try
				{
					EntityProducer service = (EntityProducer) ComponentManager.get(serviceName);
                    if (service == null) {
                        // find the service using the EntityManager
                        Collection<EntityProducer> entityProducers = entityManager.getEntityProducers();
                        for (EntityProducer entityProducer : entityProducers) {
                            if (serviceName.equals(entityProducer.getClass().getName())
                                    || serviceName.equals(entityProducer.getLabel())
                            ) {
                                service = entityProducer;
                                break;
                            }
                        }
                    }

                    try
					{
						String msg = "";
						if (service != null) {
						    if ((system.equalsIgnoreCase(ArchiveService.FROM_SAKAI) || system.equalsIgnoreCase(ArchiveService.FROM_SAKAI_2_8))) {
						        if (checkSakaiService(filterSakaiService, filteredSakaiService, serviceName)) {
						            // checks passed so now we attempt to do the merge
		                            log.debug("Merging archive data for {} ({}) to site {}", serviceName, fileName, siteId);
		                            msg = service.merge(siteId, element, fileName, fromSite, attachmentNames, Collections.EMPTY_MAP /* empty userIdTran map */, usersListAllowImport);
						        } else {
						            log.warn("Skipping merge archive data for {} ({}) to site {}, checked filter failed (filtersOn={}, filters={})", serviceName, fileName, siteId, filterSakaiService, Arrays.toString(filteredSakaiService));
						        }
						    } else {
						        log.warn("Skipping archive data for for {} ({}) to site {}, this does not appear to be a sakai archive", serviceName, fileName, siteId);
						    }
						} else {
                            log.warn("Skipping archive data for for {} ({}) to site {}, no service (EntityProducer) could be found to deal with this data", serviceName, fileName, siteId);
						}
						results.append(msg);
					}
					catch (Throwable t)
					{
						results.append("Error merging: " + serviceName + " in file: " + fileName + " : " + t.toString() + "\n");
						log.warn("Error merging: {} in file: {} : {}", serviceName, fileName, t.toString());
					}
				}
				catch (Throwable t)
				{
					results.append("Did not recognize the resource service: " + serviceName + " in file: " + fileName + "\n");
					log.warn("Did not recognize the resource service: {} in file: {} : {}", serviceName, fileName, t.toString());
				}
			}
		}

	}	// processMerge
	
	/**
	* Merge the site definition from the site part of the archive file into the site service.
	* Translate the id to the siteId.
	* @param siteId The id of the site getting imported into.
	* @param fromSiteId The id of the site the archive was made from.
	* @param element The XML DOM tree of messages to merge.
	* @param creatorId The creator id
	*/
	protected void mergeSite(String siteId, String fromSiteId, Element element, Map<String, String> useIdTrans, String creatorId, boolean filterSakaiRoles, String[] filteredSakaiRoles)
	{
		String source = "";
					
		Node parent = element.getParentNode();
		if (parent.getNodeType() == Node.ELEMENT_NODE) {
			source = ((Element) parent).getAttribute("system");
		}
					
		NodeList children = element.getChildNodes();
		for(int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element element2 = (Element) child;
			if (!element2.getTagName().equals("site")) continue;
			
			NodeList toolChildren = element2.getElementsByTagName("tool");
			for(int j = 0; j < toolChildren.getLength(); j++)
			{
				Element element3 = (Element) toolChildren.item(j);
				String toolId = element3.getAttribute("toolId");
				if (toolId != null)
				{
					// TODO: Do we need this?
					toolId = toolId.replaceAll(old_toolId_prefix, new_toolId_prefix);
					for (int k = 0; k < old_toolIds.length; k++)
					{
						toolId = toolId.replaceAll(old_toolIds[i], new_toolIds[i]);
					}
				}
				element3.setAttribute("toolId", toolId);
			}
				
			// merge the site info first
			try
			{
				siteService.merge(siteId, element2, creatorId);
				mergeSiteInfo(element2, siteId);
			}
			catch(Exception any)
			{
				log.warn(any.getMessage(), any);
			}
			
			Site site = null;
			try
			{
				site = siteService.getSite(siteId);
			}
			catch (IdUnusedException e) 
			{
				log.warn("The site with id {} doesn't exist : {}", e.toString());
				return;
			}
		
			if (site != null)
			{
				NodeList children2 = element2.getChildNodes();
				final int length2 = children2.getLength();
				for(int i2 = 0; i2 < length2; i2++)
				{
					Node child2 = children2.item(i2);
					if (child2.getNodeType() != Node.ELEMENT_NODE) continue;
					Element element3 = (Element)child2;
					if (!element3.getTagName().equals("roles")) continue;
	
					try  {	
						mergeSiteRoles(element3, siteId, useIdTrans, filterSakaiRoles, filteredSakaiRoles);
					} 
					catch (PermissionException e1) {
						log.warn(e1.getMessage(), e1);
					}
				}	
			}
		}
	}	// mergeSite
	
	/**
	* Merge the site info like description from the site part of the archive file into the site service.
	* @param element The XML DOM tree of messages to merge.
	* @param siteId The id of the site getting imported into.
	*/
	protected void mergeSiteInfo(Element el, String siteId)
		throws IdInvalidException, IdUsedException, PermissionException, IdUnusedException, InUseException 
	{
		// heck security (throws if not permitted)
		unlock(SiteService.SECURE_UPDATE_SITE, siteService.siteReference(siteId));
	
		Site edit = siteService.getSite(siteId);
		String desc = el.getAttribute("description-enc");
			
		try
		{
		byte[] decoded = Base64.decodeBase64(desc.getBytes("UTF-8"));
		byte[] filteredDecoded = decoded;
		for(int i=0; i<decoded.length;i++)
		{
			byte b = decoded[i];
			if (b == (byte) -109 || b == (byte) -108)
			{
				// smart quotes, open/close double quote
				filteredDecoded[i] = (byte) 34;
			}
			else if (b == (byte) -111 || b == (byte) -110)
			{
				// smart quotes, open/close double quote
				filteredDecoded[i] = (byte) 39;
			}
			else if (b == (byte) -106)
			{
				// dash
				filteredDecoded[i] = (byte) 45;
			}
		}
		desc = new String(decoded, "UTF-8");
		}
		catch(Exception any)
		{
			log.warn("mergeSiteInfo(): exception caught", any);	
		}							
		//edit.setTitle(title);
		edit.setDescription(desc);
		
		siteService.save(edit);
			 
		return;
		
	} // mergeSiteInfo	
	
	/**
	* Merge the the permission-roles settings into the site
	* @param element The XML DOM tree of messages to merge.
	* @param siteId The id of the site getting imported into.
	*/
	protected void mergeSiteRoles(Element el, String siteId, Map<String, String> useIdTrans, boolean filterSakaiRoles, String[] filteredSakaiRoles) throws PermissionException
	{
		// heck security (throws if not permitted)
		unlock(SiteService.SECURE_UPDATE_SITE, siteService.siteReference(siteId));
		
		String source = "";
		
		// el: <roles> node			
		Node parent0 = el.getParentNode(); // parent0: <site> node
		Node parent1 = parent0.getParentNode(); // parent1: <service> node
		Node parent = parent1.getParentNode(); // parent: <archive> node containing "system"
		
		if (parent.getNodeType() == Node.ELEMENT_NODE)
		{
			Element parentEl = (Element)parent;
			source = parentEl.getAttribute("system");
		}

		try
		{
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element2 = (Element)child;
                String roleId = null;

                if (ArchiveService.FROM_SAKAI_2_8.equals(source))
                {
                    if (!"role".equals(element2.getTagName())) continue;

                    roleId = element2.getAttribute("roleId");
                }
                else
                {
                    roleId = element2.getTagName();
                }

				//SWG Getting rid of WT part above, this was previously the else branch labeled "for both CT classic and Sakai CTools"
				// check is this roleId is a qualified one
				if (!checkSystemRole(source, roleId, filterSakaiRoles, filteredSakaiRoles)) continue;
					
				NodeList children2 = element2.getChildNodes();
				final int length2 = children2.getLength();
				for(int i2 = 0; i2 < length2; i2++)
				{
					Node child2 = children2.item(i2);
					if (child2.getNodeType() != Node.ELEMENT_NODE) continue;
					Element element3 = (Element)child2;
					if (!element3.getTagName().equals("ability")) continue;

					String userId = element3.getAttribute("userId");	
					// this user has a qualified role, his/her resource will be imported
					usersListAllowImport.add(userId);
				}
			} // for
		}
		catch(Exception err)
		{
			log.warn("()mergeSiteRoles site {} edit exception caught: {}", siteId, err.toString());
		}
		return;
	
	} // mergeSiteRoles
	
	/**
	* Merge the user list into the the system.
	* Translate the id to the siteId.
	* @param element The XML DOM tree of messages to merge.
	*/
	//SWG This seems to have been abandoned for anything for WorkTools.
	//    If we need the ability to import users again, see ArchiveServiceImpl.java
	//    for the implementation of this method.
	//protected String mergeUsers(Element element, HashMap useIdTrans) 
	//throws IdInvalidException, IdUsedException, PermissionException
	
	
	/**
	 * Old archives have the old CHEF 1.2 service names...
	 */
	protected String translateServiceName(String name)
	{
		if ("org.chefproject.service.GenericContentHostingService".equals(name))
		{
			return ContentHostingService.class.getName();
		}
		
		return name;
	}
	
	/*
	 * 
	 */
	protected boolean checkSakaiService (boolean m_filterSakaiServices, String[] m_filteredSakaiServices, String serviceName)
	{
		if (m_filterSakaiServices)
		{
			for (int i = 0; i < m_filteredSakaiServices.length; i ++)
			{
				if (serviceName.endsWith(m_filteredSakaiServices[i].toString()))
				{
					return true;
				}
			}
			return false;
		}
		else 
		{
			return true;
		}
	}
	 
	 /**
		* Check security permission.
		* @param lock The lock id string.
		* @param reference The resource's reference string, or null if no resource is involved.
		* @exception PermissionException thrown if the user does not have access
		*/
		protected void unlock(String lock, String reference) throws PermissionException
		{
			if (!securityService.unlock(lock, reference))
			{
				// needs to bring back: where is sessionService
				// throw new PermissionException(UsageSessionService.getSessionUserId(), lock, reference);
			}
		} // unlock
		
	/**
	* When Sakai is importing a role in site.xml, check if it is a qualified role.
	* @param roleId
	* @return boolean value - true: the role is accepted for importing; otherwise, not;
	*/
	protected boolean checkSystemRole(String system, String roleId, boolean filterSakaiRoles, String[] filteredSakaiRoles) {
		if (system.equalsIgnoreCase(ArchiveService.FROM_SAKAI) || system.equalsIgnoreCase(ArchiveService.FROM_SAKAI_2_8)) {
			if (filterSakaiRoles)
			{
				for (int i = 0; i <filteredSakaiRoles.length; i++)
				{
					if (!filteredSakaiRoles[i].equalsIgnoreCase(roleId))
					return true;
				}
			}
			else {
				return true;
			}
		}	
		return false;
	}
}
