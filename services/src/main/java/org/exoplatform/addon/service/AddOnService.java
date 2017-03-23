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

import org.exoplatform.addon.marketplace.Constants;
import org.exoplatform.addon.marketplace.upgrade.UpgradeAddonNodeType;
import org.exoplatform.addon.service.model.Addon;
import org.exoplatform.addon.utils.ImageUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddOnService {
  
  public static final String ADDON_CACHE_NAME = "addon.cache";
  
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
  
  public final static String  MIX_VOTEABLE_NODE_TYPE = "mix:votable";
  public final static String  MIX_COMMENTABLE_NODE_TYPE = "mix:commentable";
  public final static String  EXO_PRIVILEGEABLE_NODE_TYPE = "exo:privilegeable";
  
	
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
	public static String getMixinProperty(Node node,String mixinType, String mixinProperty) throws RepositoryException{
		StringBuffer sb;

		//--- Manage categories
		//--- Store category mixin
		if(node.isNodeType(mixinType)){
			sb = new StringBuffer();
			Value[] categories = node.getProperty(mixinProperty).getValues();
			Arrays.stream(categories).forEach(val -> {
				try {
					sb.append(val.getString()+" - ");
				} catch (RepositoryException re) {
					log.error("Error to compute category property from JCR node ["+ node+"]",re);
				}
			});
			return sb != null ? sb.substring(0, sb.length()-3).toString():"";

		}
		return "";
	}
	public static Node createAddonThumbnailImageCover(Node node) throws Exception{
    //find first node in screenshot
    Node firstNode = null;
    Node mediaNode = node.getNode("medias/images");                
    if(mediaNode != null){
      NodeIterator iterator = mediaNode.getNodes(); 
      if (iterator.getSize() > 0) {
        firstNode= iterator.nextNode();
      }
    }
    if(firstNode!=null){
      if(!node.hasNode("medias/thumbnail")){
        node.addNode("medias/thumbnail");
      }
      String imageMimeType = firstNode.getNode("jcr:content").getProperty("jcr:mimeType").getString();
      String imageFileName = firstNode.getName();
      InputStream imageInputStream = firstNode.getNode("jcr:content").getProperty("jcr:data").getStream();
      
      Node imageNode = node.addNode("medias/thumbnail" + "/thumbnail_" + imageFileName, "nt:file");
      Node imageContent = imageNode.addNode("jcr:content", "nt:resource");
      InputStream thumbnalInputStream = ImageUtils.createResizedImage(imageInputStream, 450, 360, imageMimeType);
      imageContent.setProperty("jcr:data", thumbnalInputStream);
      imageContent.setProperty("jcr:mimeType", imageMimeType);
      imageContent.setProperty("jcr:lastModified", Calendar.getInstance());
      node.save();
      return imageNode;
    }else{
      return null;
    }
  }
	
	public static String getImageCover(Node node) throws PathNotFoundException, RepositoryException{
		
		String path ="/marketplace-extension-webapp/skin/css/images/addons-icon.jpg";
		try{
		if(node != null){
	    //get thumbnailImageCover
		  if(node.hasNode("medias/thumbnail")){
  		  Node thumbnailNode = node.getNode("medias/thumbnail");   
  		  if(thumbnailNode != null){
  
          NodeIterator iterator = thumbnailNode.getNodes(); 
          if (iterator.getSize() > 0) {
            Node firstNode= iterator.nextNode();
            path = "/rest/jcr/repository/collaboration" + firstNode.getPath();
            return path;
          }
          
        }
		  }
		  
		  //if thumbnailImage is not existed, create ImageCover from screenshot
			Node mediaNode = node.getNode("medias/images");			   			   
			if(mediaNode != null){

				NodeIterator iterator = mediaNode.getNodes();	
				if (iterator.getSize() > 0) {
					//Node firstNode= iterator.nextNode();
				  try {
            Node thumbnailNodeImage = createAddonThumbnailImageCover(node);
            path = "/rest/jcr/repository/collaboration" + thumbnailNodeImage.getPath();
          } catch (Exception e) {
            log.warn("Can not create ThumbnailImageCover for " + node.getPath(), e);
          }
				}
				
			}
			
		}
		}catch(Exception e){
		  log.warn("Can not get ImageCover of " + node.getPath(), e);
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
	
	public static String getAvatarNode(Node node) throws Exception, RepositoryException{
    if(node != null && node.hasNode("medias/avatar")){
      Node mediaNode = node.getNode("medias/avatar"); 
      NodeIterator nodeIterator = mediaNode.getNodes();
      while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        if(img.getPath() != null)
          return (imgPathBase + img.getPath());
      }
      
    }
    return null;
	}
		
	public static Node updateNode(String title,String name,Boolean hosted, List<String> categoriesMixinList, Map<String,String> map,Boolean isNew) throws Exception {

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
		//--- Manage Categories : update category name
		//--- Add category mixin
		if (categoriesMixinList != null) {

			List<Value> tempMix = new ArrayList<Value>();
			for (String str : categoriesMixinList) {
				tempMix.add(currentNode.getSession().getValueFactory().createValue(str));
			}

			if(currentNode.canAddMixin(UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY)) {
				currentNode.addMixin(UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY);

			}
			//--- method «setProperty» should be outside the test block, because «canAddMixin» return «true» only when the mixin doesn't already exist
			currentNode.setProperty(UpgradeAddonNodeType.ADDON_MIXIN_PROPPERTY_NAME, tempMix.toArray(new Value[tempMix.size()]));
			currentNode.save();
		}
		//--- FIN category mixin
		homeNode.getSession().save();
		
		return currentNode;
	}
		
	public static Node storeNode(String title,String name,Boolean hosted, List<String> categoriesMixinList, Map<String,String> map,Boolean isNew) throws Exception {
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

		//--- FIN
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
/*  Comment out to avoid add comment in Activity: ORG-1017
		Node htmlNode = currentNode.addNode("default.html", "nt:file");
		htmlNode.addMixin("exo:htmlFile");
		Node contentNode = htmlNode.addNode("jcr:content", "nt:resource");
		contentNode.setProperty("jcr:data","default.html");
		contentNode.setProperty("jcr:mimeType", "text/html");
		contentNode.setProperty("jcr:encoding", "UTF-8");
		contentNode.setProperty("jcr:lastModified", new Date().getTime());
*/
		
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

		//add mixin to allow comment and vote
		currentNode.addMixin(MIX_COMMENTABLE_NODE_TYPE);
		currentNode.addMixin(MIX_VOTEABLE_NODE_TYPE);


		//--- Add category mixin
		if (categoriesMixinList != null) {

			List<Value> tempMix = new ArrayList<Value>();
			for (String str : categoriesMixinList) {
				tempMix.add(currentNode.getSession().getValueFactory().createValue(str));
			}
			if(!currentNode.isNodeType(UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY)){
				if(currentNode.canAddMixin(UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY)) {
					currentNode.addMixin(UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY);
					currentNode.setProperty(UpgradeAddonNodeType.ADDON_MIXIN_PROPPERTY_NAME, tempMix.toArray(new Value[tempMix.size()]));
					currentNode.save();
				}
			}
		}
		//--- FIN category mixin
		
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

	public static String getAddOnHomePath() throws Exception{
	  String preference_item_path = Utils.getPortletPreference(PREFERENCE_ITEM_PATH);
    if(preference_item_path == null)
      preference_item_path = "web contents/Contributions";
    
    String siteName = Utils.getPortletPreference(PREFERENCE_SITE);
    if(siteName == null)
      siteName = Util.getPortalRequestContext().getSiteName();
    
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, siteName);
    String homePath = dummyNode.getPath() + "/" + preference_item_path + "/";
    return homePath;
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
			cmNode.addMixin(EXO_PRIVILEGEABLE_NODE_TYPE);
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
			                                     String email,  String titleAddon, String description,
			                                     String version,String license, String author,
			                                     String compatibility,String  sourceUrl,
			                                     String documentUrl ,String downloadUrl , String codeUrl,
			                                     String demoUrl, String installCommand, Boolean hosted,
			                                     String hostName)
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
		
		String _codeUrl= codeUrl!= null ? "Code URL: " +  codeUrl : "Code URL: ";
		String _demoUrl= demoUrl!= null ? "Demo URL: " +  demoUrl : "Demo URL: ";
		String _installCommand= installCommand!= null ? "Install command: " +  installCommand : "Install command: ";
		
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
        + _codeUrl + "<br><br>"
        + _demoUrl + "<br><br>"
        + _installCommand + "<br><br>"
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
	
	
	public static ExoCache<String, Addon> getAddonCache(){
	  CacheService addonCache = CommonsUtils.getService(CacheService.class);
	  ExoCache<String, Addon> cacheData = addonCache.getCacheInstance(ADDON_CACHE_NAME);
	  return cacheData;
	  
	}
	
	public static void cleanAllAddonCache(){
	    ExoCache<String, Addon> cacheData = getAddonCache();
	    cacheData.clearCache();
	}
	
	public static void cleanAddonCacheByUuid(String uuid){
	  log.info("Clean addon cache with id: " +uuid);
    ExoCache<String, Addon> cacheData = getAddonCache();
    cacheData.remove(uuid);
}
	
	public static void updateAddonDetailUrlToCache(String uuid, String detailUrl){
	  Addon addon = getAddonFromCache(uuid);
	  addon.setSeeDetailUrl(detailUrl);
	}
	
	
  public static Addon getAddonFromCache(String uuid){
     ExoCache<String, Addon> cacheData = getAddonCache();
     Addon addon = cacheData.get(uuid);
     if(null == addon){
       addon = buildAddonFromJcrNode(uuid);
       cacheData.put(uuid, addon);
     }
     return addon;
  }
  
  public static Addon buildAddonFromJcrNode(String uuid){
    log.info("Build addon cache data for uuid: " + uuid);
    Addon addon = new Addon();
    try {
      Node node = getNodeById(uuid);
      
      addon.setUuid(node.getUUID());
      addon.setDescription(getStrProperty(node, "exo:description"));
      addon.setDownloadLink(getStrProperty(node, "exo:downloadUrl"));
      addon.setJcrNodePath(node.getPath());
      addon.setName(getStrProperty(node, "exo:title"));
      addon.setOwnerid(getStrProperty(node, "exo:owner"));
      addon.setCoverImagePath(getImageCover(node));
      addon.setAuthor(getStrProperty(node, "exo:author"));
	  // --- Manage categories (store category mixin within the jcr node)
	  addon.setCategory(getMixinProperty(node,UpgradeAddonNodeType.ADDON_MIXIN_CATEGORY , UpgradeAddonNodeType.ADDON_MIXIN_PROPPERTY_NAME));

      Double voteRate=0.0;
      if (node.isNodeType("mix:votable")) {
        if (node.hasProperty("exo:votingRate"))
            voteRate = new Double(node.getProperty("exo:votingRate").getString());
      }
      addon.setVoteRate(voteRate);
      
      Integer totalVote = 0;
      if (node.isNodeType("mix:votable")) {
        if (node.hasProperty("exo:voteTotalOfLang"))
            totalVote = new Integer(node.getProperty("exo:voteTotalOfLang").getString());
      }
      addon.setTotalVote(totalVote);
      
    } catch (Exception e) {
      log.error("Can not get Addon node", e);
      return null;
    }
    
    return addon;
  }

  public static void updateAddonsCategoriesInBulk (String oldCategory, String newCategory, String event) {

	  //--- JCR path to addons
	  String addonsHomePath;
	  //--- JCR Session ;
	  Session session = null;
	  //--- Addons query
	  String addonsQuery = null;
	  //--- Query results
	  //--- JCR Query REsult Obj
	  QueryResult addonsResult = null;
	  try {

		  //--- Get session JCR
		  session = getJCRSession();
		  //--- Get Addon JCR Home Path
		  addonsHomePath = getAddonHomePath (Constants.ADDON_ITEM_PATH,Constants.ADDON_SITE_NAME);
		  //--- Compute the addon query to get addons by categories
		  addonsQuery = buildJCRQuery(addonsHomePath,oldCategory);
		  //--- Execute JCR Query
		  addonsResult = executeJCRQuery(session,addonsQuery);
		  //--- Update performed
		  updateAddonsCategoriesList(session, addonsResult,oldCategory,newCategory,event);

	  } catch (Exception e) {
		  log.error("Error when method updateAddonsCategoriesInBulk is called ",e);

	  }
  }

	/**
	 * Create and return a JCR Session
	 * @return JCR Session
	 */
	public static Session getJCRSession () {
		//--- JCR Session
		Session session = null;
		try {
			SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
			session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(),WCMCoreUtils.getRepository());

		} catch (Exception e) {
			log.error("Fail to create JCR session",e.getMessage(),e);
		}
		return session;

	}

	/**
	 *
	 * @param addonsHomePath
	 * @param oldCategory
	 * @return
	 */
	public static String buildJCRQuery (String addonsHomePath, String oldCategory) {

		//--- Query to load addons
		StringBuffer query;
		//--- Build select
		query = new StringBuffer("SELECT * FROM exo:addon ");
		//--- Build query constreintsto search all addons with oldCategory
		query.append("WHERE jcr:path like '"+addonsHomePath+"%' AND NOT jcr:path LIKE '" + addonsHomePath + "%/%'");
		//--- Build query constreintsto search all addons with oldCategory
		query.append(" AND publication:currentState='published' AND  NOT (jcr:mixinTypes = 'exo:restoreLocation') ");
		//--- Build query : add category name constreint
		query.append(" AND mix:mpkaceAddonCatName = '"+oldCategory+"' ORDER BY exo:voteTotal DESC, exo:votingRate DESC ");
		return query.toString();
	}
	public static QueryResult executeJCRQuery (Session session, String theQuery) {
		//--- JCR Query REsult Obj
		QueryResult addonsResult = null;
		//--- Quary Manager Ins
		QueryManager queryManager = null;
		//--- Query IMPL Ins
		QueryImpl jcrQ = null;
		try {
			//--- make SQL query/*
			queryManager = session.getWorkspace().getQueryManager();
			jcrQ = (QueryImpl) queryManager.createQuery(theQuery, Query.SQL);
			// execute query and fetch result*/
			addonsResult = jcrQ.execute();

		} catch (Exception e) {
			log.error("Fail to execude JCR query [{}]",theQuery,e);

		}

		return addonsResult;
	}

	/**
	 * Get JCR path to Addons path
	 * @param defaultAddonRootPath
	 * @return
	 */
	public static String getAddonHomePath (String addonRootPath, String siteName) {
		//--- Addon JCR Homepath
		String addonPath = null;
		//--- SessionProfider Obj
		SessionProvider sessionProvider = null;
		try {
			//--- GEt sessionProvider
			sessionProvider = SessionProvider.createSystemProvider();

			//--- Fetch addon items path
			LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
			//TODO : Get site node should be dynamic, think to a way to remove ad-hoc code to get to «intranet» site node
			Node siteNode = livePortalManagerService.getLivePortal(sessionProvider, siteName);
			addonPath = siteNode.getPath() + "/" + addonRootPath + "/";

		} catch (Exception e) {
			log.error("Fail to get Addons JCR Hmepath ",e);
		}

		return addonPath;

	}

	/**
	 * Update categories list for each addon
	 * @param results
	 */
	public static void updateAddonsCategoriesList (Session session, QueryResult results, String oldCategory, String newCategory, String event) {
		try {
			//--- Transform results
			NodeIterator it = results.getNodes();
			//--- Temp list to hold addon's categories
			List<String> tempCat = null;
			//--- Mixin List
			List<Value> updatedMixinList = null;
			while (it.hasNext()) {
				Node addon = it.nextNode();
				log.info("Start updating addons categories in bulk");
				tempCat = new ArrayList<String>();

				//--- Get mixin values : categories within an addon
				for (Value value : addon.getProperty(UpgradeAddonNodeType.ADDON_MIXIN_PROPPERTY_NAME).getValues()) {
					tempCat.add(value.getString());
				}
				//--- Remove oldCategory
				tempCat.removeIf(p -> p.equalsIgnoreCase(oldCategory));

				if (event.equalsIgnoreCase("update")) {
					log.info("Drop category [{}] from all addons ",oldCategory);
					//--- Add the new category
					tempCat.add(newCategory);
				}
				updatedMixinList = new ArrayList<Value>();
				for (String category : tempCat) {

					updatedMixinList.add(session.getValueFactory().createValue(category));
				}


				//--- method «setProperty» should be outside the test block, because «canAddMixin» return «true» only when the mixin doesn't already exist
				addon.setProperty(UpgradeAddonNodeType.ADDON_MIXIN_PROPPERTY_NAME, updatedMixinList.toArray(new Value[updatedMixinList.size()]));
				addon.save();

			}


		} catch (Exception e) {
			log.error("Fail to update categories list for Addon",e);

		}

	}
}

