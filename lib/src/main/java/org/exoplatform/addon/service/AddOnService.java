/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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
package org.exoplatform.addon.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;

public class AddOnService {
  
  /** The Constant PREFERENCE_RECEIVER. */
  public static final String PREFERENCE_RECEIVER = "adminEmail";

  /** The Constant PREFERENCE_RECEIVER. */
  public static final String PREFERENCE_FROM = "fromEmail";
  
  /** The Constant PREFERENCE_RECEIVER. */
  public static final String PREFERENCE_FROM_NAME = "fromName";
  
  /** The Constant PREFERENCE_EMAIL_SUBJECT. */
  public static final String PREFERENCE_EMAIL_SUBJECT = "emailsubject";
  
  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_ITEM_PATH  = "folderPath";
  /** The Constant PREFERENCE_ITEM_PATH. */
  public final static String PREFERENCE_SITE    = "siteName";
	
	private static final Log log = ExoLogger.getLogger(AddOnService.class);
	public static String imgPathBase = "/rest/jcr/repository/collaboration";
	
	public static Node getNode(String path) throws PathNotFoundException, RepositoryException{
		if(path == null || path.equals(""))
			return null;
		SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
		Session session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(), WCMCoreUtils.getRepository());
		return (Node) session.getItem(path);
	}
	public static Node getNodeById(String uuid) throws PathNotFoundException, RepositoryException{
		if(null == uuid || "".equals(uuid))
			return null;
		SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
		Session session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(), WCMCoreUtils.getRepository());
		return (Node) session.getNodeByUUID(uuid);
	}
	
	public static String getStrProperty(Node node,String propertyName) throws RepositoryException{

		if(node != null && node.hasProperty(propertyName)){
			Property prop = node.getProperty(propertyName);
			String res = prop.getString();
			return res;
		}
		
		return null;
	}	
	public static String getImageCover(Node node) throws PathNotFoundException, RepositoryException{
		
		String path ="/add-on-center-webapp/skin/css/images/addons-icon.jpg";
		if(node != null){
			Node mediaNode = node.getNode("medias/images");			   			   
			if(mediaNode != null){

				NodeIterator iterator = mediaNode.getNodes();	
				if (iterator.getSize() > 0) {
					Node firstNode= iterator.nextNode();
					path = "/rest/jcr/repository/collaboration" + firstNode.getPath();
				}
				
			}
			
		}
		return path;
		
	}
	public static List<String> getImagesNode(Node node) throws Exception, RepositoryException{
		

		List<String> images = new ArrayList<String>();
		if(node != null){
			Node mediaNode = node.getNode("medias/images"); 
			NodeIterator nodeIterator = mediaNode.getNodes();
			while (nodeIterator.hasNext()) {
				Node img = nodeIterator.nextNode();
				if(img.getPath() != null)
					images.add(imgPathBase+img.getPath());
			}
			
		}
		return images;
		

	}
		
	public static Node updateNode(String title,String name,Boolean hosted, Map<String,String> map,Boolean isNew) throws Exception {

		String nodeType = "exo:addon";

		//Node webRootNode = _livePortal.getLivePortal(sessionProvider, "website");
		Node homeNode = AddOnService.createCommunityFolder();
		Map<String, JcrInputProperty> inputProperties = new HashMap<String, JcrInputProperty>();
		String nodePath = "";
		JcrInputProperty h = new JcrInputProperty();

		h.setJcrPath("/node/default.html");
		h.setNodetype("nt:file");
		h.setValue("default.html");
		h.setMixintype("exo:htmlFile");
		inputProperties.put("/node/default.html", h);

	    /** loop through the property of node*/
		for (Map.Entry<String, String> entry : map.entrySet()) {
			JcrInputProperty jcrinputProperty = new JcrInputProperty();
			jcrinputProperty.setJcrPath("/node/" + entry.getKey());
			jcrinputProperty.setValue(entry.getValue());
			inputProperties.put("/node/" + entry.getKey(), jcrinputProperty);
		}
		JcrInputProperty hostedProp = new JcrInputProperty();
		hostedProp.setJcrPath("/node/exo:hosted");
		hostedProp.setValue(hosted);
		inputProperties.put("/node/exo:hosted", hostedProp);
		// nodeName
		JcrInputProperty nameNode = new JcrInputProperty();
		nameNode.setValue(name);
		inputProperties.put("/node", nameNode);

		CmsService cmsService = (CmsService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(CmsService.class);

		try {
			nodePath = cmsService.storeNode(nodeType, homeNode,inputProperties, isNew);
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		String[] temp = nodePath.split("/");
		String currentNodeName = temp[temp.length - 1];
		Node currentNode = homeNode.getNode(currentNodeName);
		homeNode.getSession().save();
		
		return currentNode;
	}
		
	public static Node storeNode(String title,String name,Boolean hosted, Map<String,String> map,Boolean isNew) throws Exception {
		String nodeType = "exo:addon";

		//Node webRootNode = _livePortal.getLivePortal(sessionProvider, "website");
		Node homeNode = AddOnService.createCommunityFolder();
		Map<String, JcrInputProperty> inputProperties = new HashMap<String, JcrInputProperty>();
		String nodePath = "";
		JcrInputProperty h = new JcrInputProperty();

		h.setJcrPath("/node/default.html");
		h.setNodetype("nt:file");
		h.setValue("default.html");
		h.setMixintype("exo:htmlFile");
		inputProperties.put("/node/default.html", h);

	    /** loop through the property of node*/
		for (Map.Entry<String, String> entry : map.entrySet()) {
			JcrInputProperty jcrinputProperty = new JcrInputProperty();
			jcrinputProperty.setJcrPath("/node/" + entry.getKey());
			jcrinputProperty.setValue(entry.getValue());
			inputProperties.put("/node/" + entry.getKey(), jcrinputProperty);
		}
		JcrInputProperty sendConfirmEmailProp = new JcrInputProperty();
		sendConfirmEmailProp.setJcrPath("/node/exo:sendConfirmEmail");
		sendConfirmEmailProp.setValue(false);
		inputProperties.put("/node/exo:sendConfirmEmail", sendConfirmEmailProp);
		
		JcrInputProperty hostedProp = new JcrInputProperty();
		hostedProp.setJcrPath("/node/exo:hosted");
		hostedProp.setValue(hosted);
		inputProperties.put("/node/exo:hosted", hostedProp);
		
		JcrInputProperty js = new JcrInputProperty();
		js.setJcrPath("/node/js/default.js");
		js.setNodetype("nt:file");
		js.setValue("default.js");

		inputProperties.put("/node/js/default.js", js);

		JcrInputProperty jsData = new JcrInputProperty();
		jsData.setJcrPath("/node/js/default.js/jcr:content/jcr:data");
		inputProperties.put("/node/js/default.js/jcr:content/jcr:data", jsData);

		JcrInputProperty contentJs = new JcrInputProperty();
		contentJs.setJcrPath("/node/js/default.js/jcr:content");
		contentJs.setNodetype("nt:resource");
		contentJs.setMixintype("dc:elementSet");
		inputProperties.put("/node/js/default.js/jcr:content", contentJs);
		// video
		JcrInputProperty video = new JcrInputProperty();
		video.setJcrPath("/node/medias/videos");
		video.setNodetype("nt:folder");
		video.setValue("videos");
		inputProperties.put("/node/medias/videos", video);
		// medias/ images
		JcrInputProperty image = new JcrInputProperty();
		image.setJcrPath("/node/medias/images");
		image.setNodetype("nt:folder");
		image.setValue("images");
		inputProperties.put("/node/medias/images", image);

		JcrInputProperty contentData = new JcrInputProperty();
		contentData.setJcrPath("/node/default.html/jcr:content");
		contentData.setMixintype("dc:elementSet");
		contentData.setNodetype("nt:resource");
		contentData.setValueType(0);
		contentData.setType(1);

		inputProperties.put("/node/default.html/jcr:content", contentData);

		JcrInputProperty mineTypeHtml = new JcrInputProperty();
		mineTypeHtml.setJcrPath("/node/default.html/jcr:content/jcr:mimeType");
		mineTypeHtml.setValue("text/html");
		inputProperties.put("/node/default.html/jcr:content/jcr:mimeType",
				mineTypeHtml);

		JcrInputProperty htmlEncoding = new JcrInputProperty();
		htmlEncoding.setJcrPath("/node/default.html/jcr:content/jcr:encoding");
		htmlEncoding.setValue("UTF-8");
		inputProperties.put("/node/default.html/jcr:content/jcr:encoding",
				htmlEncoding);

		JcrInputProperty exoTitle = new JcrInputProperty();
		exoTitle.setJcrPath("/node/exo:title");
		exoTitle.setValue(title);
		inputProperties.put("/node/exo:title", exoTitle);
		// document
		JcrInputProperty documentTitle = new JcrInputProperty();
		documentTitle.setJcrPath("/node/documents");// setMixintype
		documentTitle.setMixintype("exo:documentFolder");
		documentTitle.setNodetype("nt:unstructured");
		documentTitle.setValue("documents");
		inputProperties.put("/node/documents", documentTitle);
		// Node homeNode = "";
		JcrInputProperty css = new JcrInputProperty();
		css.setJcrPath("/node/css");
		css.setNodetype("exo:cssFolder");
		css.setValue("css");
		// nodeName
		JcrInputProperty nameNode = new JcrInputProperty();
		nameNode.setValue(name);
		inputProperties.put("/node", nameNode);

		CmsService cmsService = (CmsService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(CmsService.class);

		try {
			nodePath = cmsService.storeNode(nodeType, homeNode,inputProperties, isNew);
		} catch (Exception e) {
		  log.debug("Exceptions happen while storing data",e);
		}
		String[] temp = nodePath.split("/");
		String currentNodeName = temp[temp.length - 1];
		Node currentNode = homeNode.getNode(currentNodeName);
	
		/** store the default.htl file*/
		Node htmlNode = currentNode.addNode("default.html", "nt:file");
		htmlNode.addMixin("exo:htmlFile");
		Node contentNode = htmlNode.addNode("jcr:content", "nt:resource");
		contentNode.setProperty("jcr:data","");
		contentNode.setProperty("jcr:mimeType", "text/html");
		contentNode.setProperty("jcr:encoding", "UTF-8");
		contentNode.setProperty("jcr:lastModified", new Date().getTime());
		
		/**store the css default file*/
		Node cssNode = currentNode.addNode("css/default.css" , "nt:file");
		Node cssContent = cssNode.addNode("jcr:content", "nt:resource");
		cssContent.setProperty("jcr:data", "");
		cssContent.setProperty("jcr:mimeType", "text/plain");
		cssContent.setProperty("jcr:encoding", "UTF-8");
		cssContent.setProperty("jcr:lastModified", new Date().getTime());
		
		/**store the js default file */
		Node jsNode = currentNode.addNode("js/default.js" , "nt:file");
		Node jsContent = jsNode.addNode("jcr:content", "nt:resource");
		jsContent.setProperty("jcr:data", "");
		jsContent.setProperty("jcr:mimeType", "text/plain");
		jsContent.setProperty("jcr:encoding", "UTF-8");
		jsContent.setProperty("jcr:lastModified", new Date().getTime());
		homeNode.getSession().save();

		return currentNode;
	}

	public static String getPortalName() {
		ExoContainer container = ExoContainerContext.getCurrentContainer();
		PortalContainerInfo containerInfo = (PortalContainerInfo) container
				.getComponentInstanceOfType(PortalContainerInfo.class);
		return containerInfo.getContainerName();
	}

	public static String getRestName() {
		ExoContainer container = ExoContainerContext.getCurrentContainer();
		PortalContainerInfo containerInfo = (PortalContainerInfo) container .getComponentInstanceOfType(PortalContainerInfo.class);
		String portalName = containerInfo.getContainerName();
		PortalContainerConfig portalContainerConfig = WCMCoreUtils.getService(PortalContainerConfig.class);
		return portalContainerConfig.getRestContextName(portalName);
	}

	
	public static Node createCommunityFolder() throws Exception {
		String preference_item_path = Utils.getPortletPreference(PREFERENCE_ITEM_PATH);
		if(preference_item_path == null)
			preference_item_path = "web contents/Contributions";

		String folderName = null;
		if(preference_item_path != null){
		
			String[] strs = preference_item_path.split("/");
			folderName = strs[strs.length-1];
		}
		String siteName = Utils.getPortletPreference(PREFERENCE_SITE);
		if(siteName == null)
			siteName = Util.getPortalRequestContext().getSiteName();
		
		SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
		
		LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
		Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, siteName);
	
		if (!dummyNode.hasNode(preference_item_path)) {
			Session session = dummyNode.getSession();
			Node cmNode = dummyNode.addNode(preference_item_path,"nt:unstructured");
			cmNode.setProperty("exo:title", folderName);
			cmNode.addMixin("exo:privilegeable");
      Map<String, String[]> permissions = new HashMap<String, String[]>();
      permissions.put("*:/platform/administrators", PermissionType.ALL);
      permissions.put("*:/platform/users", new String[]{PermissionType.READ,PermissionType.ADD_NODE,PermissionType.SET_PROPERTY});
      ((ExtendedNode)cmNode).setPermissions(permissions);
      
			session.save();
			return cmNode;
		} else {
			return dummyNode.getNode(preference_item_path);
		}
	}
	
	/**
	 * 
	 */
	public static void sendRequestReceiveMail(String receiver, String fromEmail, String hostName)
			throws Exception {

		MailService mailService = WCMCoreUtils.getService(MailService.class);
		
		Message message = new Message();
		message.setTo(receiver);
		
		message.setFrom("eXo Resource Center" + "<" +fromEmail + ">");
		String bodyMessage = "<br> Thank you for submitting your add-on." + "<br>"
				             + "<br>"
		                     +  "=========================" + "<br>"
		     				 + hostName;
		
		message.setBody(bodyMessage);
		message.setMimeType("text/html");
		message.setSubject("Thank you for submitting your Add-on");
		mailService.sendMessage(message);

	}
	
	public static void sendNewAddonSubmisson(String receiver, String fromEmail, String subject,
			                                 String email,  String titleAddon, String description, String version,String license, String author, String compatibility,String  sourceUrl, String documentUrl ,String downloadUrl , Boolean hosted, String hostName)
			throws Exception {

		MailService mailService = WCMCoreUtils.getService(MailService.class);
	    

		Message message = new Message();
		message.setTo(receiver);
		message.setFrom("eXo Resource Center" + "<" +fromEmail + ">");
		
		String _version = version!= null ? "Version: " +  version : "Version: ";
		String _license = license!= null ? "License: " +  license : "License: ";
		
		String _author = author!= null ? "Author: " +  author : "Author: ";
		String _compatibility = compatibility!= null ? "Compatibility: " +  compatibility : "Compatibility: ";
		
		String _sourceUrl = sourceUrl!= null ? "Source Code: " +  sourceUrl : "Source Code: ";
		String _documentUrl= documentUrl!= null ? "Documentation: " +  documentUrl : "Documentation: ";
		String _downloadUrl= downloadUrl!= null ? "Download: " +  downloadUrl : "Download: ";
		String _hosted= hosted ? "I wish my add-on to be hosted on the eXo Add-on repository on Github: Yes" : "I wish my add-on to be hosted on the eXo Add-on repository on Github: No";
		String bodyMessage = "The following add-on is submitted on " + hostName + "<br><br>"
				
				+ "Add-on Name: " + titleAddon + "<br><br>"
				+ "Description: " + description + "<br><br>"
				+ _version + "<br><br>"
                + _license + "<br><br>"
                + _compatibility + "<br><br>"
                + _sourceUrl + "<br><br>"
                + _documentUrl + "<br><br>"
				+ _downloadUrl + "<br><br>"
				+ _hosted + "<br><br>"
				+ _author + "<br><br>"
				+ "Email (for internal use only): " + email + "<br><br>"
				+ "Please login to the website back-end to validate or refuse the add-on. (go to \"Web contents\" folder > \"Contributions\" folder, add categories and publish. " + "<br><br>"
			    + "=========================" + "<br>"
				+ hostName;
		
		message.setBody(bodyMessage);
		message.setMimeType("text/html");
		message.setSubject(subject);
		mailService.sendMessage(message);

	}
	
	public static void SendConfirmationAddonPublishedEmail(String receiver,String nodeName, String hostName) throws Exception{
		
		String fromEmail = Utils.getPortletPreference(PREFERENCE_FROM);
		String fromName = Utils.getPortletPreference(PREFERENCE_FROM_NAME);
		String itemPath = Utils.getPortletPreference(PREFERENCE_ITEM_PATH);
		String[] strs = itemPath.split("/");
	    String folder = strs[strs.length-2];
	    
		MailService mailService = WCMCoreUtils.getService(MailService.class);
		Message message = new Message();
		String link = hostName + "/portal/intranet/addon-detail?content-id=/repository/collaboration/sites/intranet/web contents/Contributions/" + nodeName;
		message.setTo(receiver);
		message.setFrom(fromName + "<" +fromEmail + ">");
		String bodymess = "Thank your for contributing to the eXo Add-ons, your add-on has been validated and published." + "<br>"
				          +   "Please access you add-on here: " + "<a href='link' >" + link + "</a>" +  "<br><br>"
				          +   "=========================" + "<br>"
		     			    + hostName;
		message.setBody(bodymess);
		message.setMimeType("text/html");
		message.setSubject("Your Add-on has been validated and published on " + hostName);


		mailService.sendMessage(message);

		
		
	}
	
	
	/**
	 * validate the fields
	 */
	public static boolean validateEmail(String email) {
		 String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		 Pattern pattern = Pattern.compile(EMAIL_PATTERN);
		 Matcher matcher = pattern.matcher(email);
		 return matcher.matches();
	}
	
}

