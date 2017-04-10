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
package org.exoplatform.community.portlet.addon.search;

import org.exoplatform.addon.service.AddOnService;
import org.exoplatform.addon.utils.ImageUtils;
import org.exoplatform.community.portlet.addon.UIAddOnWizard;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.input.UIUploadInput;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
       

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchEdit.gtmpl", events = {
    @EventConfig(listeners = UIAddOnSearchEdit.UpdateActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnSearchEdit.AddUIUploadActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnSearchEdit.RemoveUIUploadActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnSearchEdit.RemoveImageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnSearchEdit.RemoveAvatarImageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnSearchEdit.CancelActionListener.class, phase = Phase.DECODE) })
public class UIAddOnSearchEdit extends UIForm implements UIPopupComponent {

  private static final Log   log            = ExoLogger.getLogger(UIAddOnSearchEdit.class);

  public static final String WIZARD_FORM_ID = "wizardFormId";

  private int                imgCount       = 1;

  private String             nodeId;

  private List<String>       imageGallery   = new ArrayList<String>();

  private List<String>       imagesRemoved  = new ArrayList<String>();
  
  private String             avatarImage;
  
  public enum ImageType {SCREENSHOT, AVATAR, THUMBNAIL};

  public UIAddOnSearchEdit() throws Exception {
    UIAddOnWizard uiAddOnWizard = new UIAddOnWizard(WIZARD_FORM_ID);
    addChild(uiAddOnWizard);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
    context.getJavascriptManager()
           .getRequireJS()
           .require("SHARED/addons", "addons")
           .addScripts("addons.init();");

  }

  public void reset() {
    super.reset();
    this.imgCount = 1;
    this.setImageGallery(new ArrayList<String>());
    this.setImagesRemoved(new ArrayList<String>());
    this.setAvatarImage("");
    try {
      this.getImagesNode();
      this.getAvatarNode();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public void init() throws PathNotFoundException, RepositoryException {

    UIAddOnWizard uiAddOnWizard = this.getChildById(WIZARD_FORM_ID);
    uiAddOnWizard.initVals(this.getNode());

  }

  public String getStrProperty(String propertyName) throws RepositoryException {

    return AddOnService.getStrProperty(this.getNode(), propertyName);

  }

  public String getInputValue(String compName) throws RepositoryException {

    if (this.getNode() == null)
      return null;
    String[] properties = { UIAddOnWizard.ADDON_TITLE, UIAddOnWizard.ADDON_DESCRIPTION,
        UIAddOnWizard.ADDON_DOWNLOAD_URL, UIAddOnWizard.ADDON_DOCUMENT_URL,
        UIAddOnWizard.ADDON_SOURCE_URL, UIAddOnWizard.ADDON_COMPABILITY,
        UIAddOnWizard.ADDON_LICENSE, UIAddOnWizard.ADDON_VERSION, UIAddOnWizard.ADDON_CODE_URL,
        UIAddOnWizard.ADDON_DEMO_URL, UIAddOnWizard.ADDON_INSTALL_COMMAND};

    for (int i = 0; i < properties.length; i++) {
      if (compName.equals(properties[i])) {

        return this.getStrProperty("exo:" + compName);
      }
    }
    return null;

  }

  public Boolean isHosted() throws RepositoryException {

    if (this.getNode() != null && this.getNode().hasProperty("exo:hosted")) {

      return this.getNode().getProperty("exo:hosted").getValue().getBoolean();

    }
    return null;
  }

  public void setNodeId(String id) {
    this.nodeId = id;
  }

  public String getNodeId() {
    return this.nodeId;
  }

  public void setImageGallery(List<String> imgGal) {
    this.imageGallery = imgGal;
  }

  public List<String> getImageGallery() {
    return this.imageGallery;

  }

  public String getAvatarImage() {
    return avatarImage;
  }

  public void setAvatarImage(String avatarImage) {
    this.avatarImage = avatarImage;
  }

  public void setImagesRemoved(List<String> images) {

    this.imagesRemoved = images;
  }

  public List<String> getImagesRemoved() {
    return this.imagesRemoved;
  }

  public Node getNode() throws PathNotFoundException, RepositoryException {
    return AddOnService.getNodeById(this.getNodeId());
  }

  public void getImagesNode() throws Exception, RepositoryException {

    this.setImageGallery(AddOnService.getImagesNode(this.getNode()));

  }
  
  public void getAvatarNode() throws Exception, RepositoryException {

    this.setAvatarImage(AddOnService.getAvatarNode(this.getNode()));

  }

  public void removeImageNode() throws PathNotFoundException, RepositoryException {
    if (0 == this.getImagesRemoved().size())
      return;
    if (this.getNode() != null) {
      Node mediaNode = this.getNode().getNode("medias/images");
      NodeIterator nodeIterator = mediaNode.getNodes();
    /*
      while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        String src = AddOnService.imgPathBase + img.getPath();
        for (int i = 0; i < this.getImagesRemoved().size(); i++) {
          if (src.equals(this.getImagesRemoved().get(i))) {
            log.debug(" ===== remove image " + img.getPath());
            img.remove();
          }
        }
      }
      */
      
      //BEGIN use systemSession to remove ImageNode
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(), WCMCoreUtils.getRepository());
      
      while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        String src = AddOnService.imgPathBase + img.getPath();
        for (int i = 0; i < this.getImagesRemoved().size(); i++) {
          if (src.equals(this.getImagesRemoved().get(i))) {
            log.debug(" ===== remove image " + img.getPath());
            Node toRemove =  (Node) session.getItem(img.getPath());
            toRemove.remove();
          }
        }
      }
      session.save();
      //END use systemSession to remove ImageNode
    }
  }
  
  public void removeAvatarNode() throws PathNotFoundException, RepositoryException {
    if (this.getNode() != null && this.getNode().hasNode("medias/avatar")) {
      Node avatarFolderNode = this.getNode().getNode("medias/avatar");
      NodeIterator nodeIterator = avatarFolderNode.getNodes();
      /*
       while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        img.remove();
      }
      */
      
      //BEGIN use systemSession to remove AvatarNode
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(), WCMCoreUtils.getRepository());
      
      
      while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        Node toRemove =  (Node) session.getItem(img.getPath());
        toRemove.remove();
      }
      session.save();
      //END use systemSession to remove AvatarNode

    }
  }
  
  public void removeThumbnalNode() throws PathNotFoundException, RepositoryException {
    if (this.getNode() != null && this.getNode().hasNode("medias/thumbnail")) {
      Node thumbnailFolderNode = this.getNode().getNode("medias/thumbnail");
      
      //use systemSession to remove thumbnailFolderNode
      
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession(WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName(), WCMCoreUtils.getRepository());
      Node toRemove =  (Node) session.getItem(thumbnailFolderNode.getPath());
      toRemove.remove();
      session.save();
      
      /*   
      NodeIterator nodeIterator = thumbnailFolderNode.getNodes();

      while (nodeIterator.hasNext()) {
        Node img = nodeIterator.nextNode();
        img.remove();
      }*/

    }
  }
  
  public boolean updateThumbnailImage() throws Exception{
    removeThumbnalNode();
    
    //find first node in screenshot
    Node firstNode = null;
    Node mediaNode = this.getNode().getNode("medias/images");                
    if(mediaNode != null){
      NodeIterator iterator = mediaNode.getNodes(); 
      if (iterator.getSize() > 0) {
        firstNode= iterator.nextNode();
      }
    }
    if(firstNode!=null){
      if(!this.getNode().hasNode("medias/thumbnail")){
        this.getNode().addNode("medias/thumbnail");
      }
      String imageMimeType = firstNode.getNode("jcr:content").getProperty("jcr:mimeType").getString();
      String imageFileName = firstNode.getName();
      InputStream imageInputStream = firstNode.getNode("jcr:content").getProperty("jcr:data").getStream();
      
      Node imageNode = this.getNode().addNode("medias/thumbnail" + "/thumbnail_" + imageFileName, "nt:file");
      Node imageContent = imageNode.addNode("jcr:content", "nt:resource");
      InputStream thumbnalInputStream = ImageUtils.createResizedImage(imageInputStream, 450, 360, imageMimeType);
      imageContent.setProperty("jcr:data", thumbnalInputStream);
      imageContent.setProperty("jcr:mimeType", imageMimeType);
      imageContent.setProperty("jcr:lastModified", Calendar.getInstance());
    }else{
      return false;
    }
    return true;
  }

  public boolean addImageNode(String targetSubNodeName, UIUploadInput child, Event<UIAddOnSearchEdit> event, ImageType imageType) throws FileNotFoundException,
                                               ItemExistsException,
                                               PathNotFoundException,
                                               NoSuchNodeTypeException,
                                               LockException,
                                               VersionException,
                                               ConstraintViolationException,
                                               RepositoryException {

    InputStream[] inputStreams = null;
    UploadResource[] uploadResource = ((UIUploadInput) child).getUploadResources();
    inputStreams = ((UIUploadInput) child).getUploadDataAsStreams();

    if (uploadResource.length > 0) {

      String imgFileName = uploadResource[0].getFileName();
      imgFileName = imgFileName.replaceAll("[^a-zA-Z0-9.-]", "-");
      String imgMineType = uploadResource[0].getMimeType();
      if(imgMineType.substring(0,imgMineType.lastIndexOf("/")).equals("image") == false){
        UIApplication uiApp = this.getAncestorOfType(UIApplication.class);
        UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
        uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.invalidImage",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
        return false;
      }
      if(!this.getNode().hasNode("medias/"+ targetSubNodeName)){
        this.getNode().addNode("medias/"+ targetSubNodeName);
      }
      
      if(imageType.equals(ImageType.AVATAR)){
        Node imageNode = this.getNode().addNode("medias/"+ targetSubNodeName + "/avatar_" + imgFileName, "nt:file");
        Node imageContent = imageNode.addNode("jcr:content", "nt:resource");
        InputStream avatarInputStream = ImageUtils.createResizedImage(inputStreams[0], 242, 242, imgMineType);
        imageContent.setProperty("jcr:data", avatarInputStream);
        imageContent.setProperty("jcr:mimeType", imgMineType);
        imageContent.setProperty("jcr:lastModified", Calendar.getInstance());
      }
      
      if(imageType.equals(ImageType.SCREENSHOT)){
        Node imageNode = this.getNode().addNode("medias/"+ targetSubNodeName + "/" + imgFileName, "nt:file");
        Node imageContent = imageNode.addNode("jcr:content", "nt:resource");
        imageContent.setProperty("jcr:data", inputStreams[0]);
        imageContent.setProperty("jcr:mimeType", imgMineType);
        imageContent.setProperty("jcr:lastModified", Calendar.getInstance());
      }
    }
    return true;
  }
  
  
  

  public static class CancelActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      UIPopupContainer uiPopupContainer = uiAddOnSearchEdit.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.cancelPopupAction();
    }

  }

  public static class RemoveUIUploadActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {
      // TODO Auto-generated method stub

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      uiAddOnSearchEdit.imgCount--;
      UIAddOnWizard uiAddOnWizard = uiAddOnSearchEdit.getChildById(UIAddOnSearchEdit.WIZARD_FORM_ID);

      String id = event.getRequestContext().getRequestParameter(OBJECTID);

      uiAddOnWizard.removeChildById(id);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
    }

  }

  public static class AddUIUploadActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      String componentName = "img" + uiAddOnSearchEdit.imgCount;
      uiAddOnSearchEdit.imgCount++;
      UIUploadInput Upload = new UIUploadInput(componentName, componentName);
      UIAddOnWizard uiAddOnWizard = uiAddOnSearchEdit.getChildById(UIAddOnSearchEdit.WIZARD_FORM_ID);
      uiAddOnWizard.addUIFormInput(Upload);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);

    }

  }

  public static class RemoveImageActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      uiAddOnSearchEdit.getImagesRemoved().add(uiAddOnSearchEdit.getImageGallery()
                                                                .get(Integer.parseInt(id)));
      uiAddOnSearchEdit.getImageGallery().remove(Integer.parseInt(id));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
    }

  }
  
  public static class RemoveAvatarImageActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      uiAddOnSearchEdit.setAvatarImage(null);
      UIAddOnWizard uiAddOnWizard = uiAddOnSearchEdit.getChildById(UIAddOnSearchEdit.WIZARD_FORM_ID);
      List<UIComponent> listChildren = uiAddOnWizard.getChildren();
      for (UIComponent child : listChildren) {
        if(child instanceof UIUploadInput && child.getName().equals(UIAddOnWizard.ADDON_AVATAR)){
          child.setRendered(true);
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
    }

  }

  public static class UpdateActionListener extends EventListener<UIAddOnSearchEdit> {

    @Override
    public void execute(Event<UIAddOnSearchEdit> event) throws Exception {

      UIAddOnSearchEdit uiAddOnSearchEdit = event.getSource();
      UIAddOnWizard uiAddOnWizard = uiAddOnSearchEdit.getChildById(UIAddOnSearchEdit.WIZARD_FORM_ID);
      UIApplication uiApp = uiAddOnSearchEdit.getAncestorOfType(UIApplication.class);

      Node currentNode = null;

      Map<String, String> mapProperties = new HashMap<String, String>();

      String email = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_EMAIL).getValue();  

      mapProperties.put("exo:" + UIAddOnWizard.ADDON_EMAIL, email);

      String author = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_AUTHOR).getValue();
      if (author == null) author = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_AUTHOR, author);

      String description = ((UIFormRichtextInput) uiAddOnWizard.getChildById(UIAddOnWizard.ADDON_DESCRIPTION)).getValue();
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DESCRIPTION, description);

      String version = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_VERSION).getValue();
      if (version == null) version = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_VERSION, version);

      String license = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_LICENSE).getValue();
      if (license == null) license = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_LICENSE, license);

      String compatibility = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_COMPABILITY)
                                          .getValue();
      if (compatibility == null) compatibility = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_COMPABILITY, compatibility);

      String sourceUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_SOURCE_URL).getValue();
      if (sourceUrl == null) sourceUrl = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_SOURCE_URL, sourceUrl);

      String documentUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_DOCUMENT_URL)
                                        .getValue();
      if (documentUrl == null) documentUrl = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DOCUMENT_URL, documentUrl);

      String downloadUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_DOWNLOAD_URL)
                                        .getValue();
      String codeUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_CODE_URL).getValue();
      if (codeUrl == null) codeUrl = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_CODE_URL, codeUrl);
      
      String demoUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_DEMO_URL).getValue();
      if (demoUrl == null) demoUrl = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DEMO_URL, demoUrl);

      String installCommand = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_INSTALL_COMMAND).getValue();
      if (installCommand == null) installCommand = "";
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_INSTALL_COMMAND, installCommand);
      
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DOWNLOAD_URL, downloadUrl);

      UICheckBoxInput hostedCb = (UICheckBoxInput) uiAddOnWizard.getUICheckBoxInput(UIAddOnWizard.ADDON_HOSTED);
      Boolean hosted = hostedCb.isChecked();

      String titleAddon = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_TITLE).getValue();
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_TITLE, titleAddon);

      //--- Manage Categories : Edit category value
      String categoryName = uiAddOnWizard.getUIFormSelectBox(UIAddOnWizard.ADDON_CATEGORY).getValue();
      //--- END

      // Validate fields
      if (email == null || titleAddon == null || description == null || downloadUrl == null) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.invalid",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
        return;
      }
      //Check list emails
      String listEmail[] = email.split(",");
      for (String emailItem : listEmail) {
        if (!AddOnService.validateEmail(emailItem)) {
          uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.invalidemail",
                                                  null,
                                                  ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
          return;
        }
      }
      
      try {
        URL url = new URL(downloadUrl);

      } catch (MalformedURLException e) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.malformurl",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
        return;
      }
      
      try {
        if(codeUrl!=null && !codeUrl.isEmpty()){
          URL url = new URL(codeUrl);
        }
      } catch (MalformedURLException e) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.invalidCodeUrl",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
        return;
      }
      
      try {
        if(demoUrl!=null && !demoUrl.isEmpty()){
          URL url = new URL(demoUrl);
        }
      } catch (MalformedURLException e) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnSearchPortlet.msg.invalidDemoUrl",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnSearchEdit);
        return;
      }
      
      uiAddOnSearchEdit.removeImageNode();
      String nodeName = uiAddOnSearchEdit.getNode().getName();

      List<UIComponent> listChildren = new ArrayList<UIComponent>();
      listChildren = uiAddOnWizard.getChildren();
      
      for (UIComponent child : listChildren) {
         
        if (child instanceof UIUploadInput && !child.getName().equals(UIAddOnWizard.ADDON_AVATAR)) {
          //update screenShots
          boolean canUpload = uiAddOnSearchEdit.addImageNode("images",(UIUploadInput) child, event, ImageType.SCREENSHOT);
          if(canUpload==false) return;
          
        }else if(child instanceof UIUploadInput && child.getName().equals(UIAddOnWizard.ADDON_AVATAR)) {
          //remove old avatar
          if(child.isRendered()){
            uiAddOnSearchEdit.removeAvatarNode();
          }
          //Update avatar image
          UploadResource[] uploadResource = ((UIUploadInput) child).getUploadResources();
          if(uploadResource.length>0){
            boolean canUpload = uiAddOnSearchEdit.addImageNode("avatar",(UIUploadInput) child, event, ImageType.AVATAR);
            if(canUpload==false) return;
          }

        }
      }
      
      //update thumbnail image
      uiAddOnSearchEdit.updateThumbnailImage();

      try {

        currentNode = AddOnService.updateNode(titleAddon, nodeName, hosted,categoryName, mapProperties, false);
        
        log.info("Clean addon cache after update addon");
        AddOnService.cleanAddonCacheByUuid(uiAddOnSearchEdit.getNodeId());
      } catch (Exception e) {
        log.error(e.getMessage());
      }
      // reset
      for (UIComponent child : listChildren) {
        if (child instanceof UIUploadInput) {
          UploadService uploadService = uiAddOnSearchEdit.getApplicationComponent(UploadService.class);
          String[] uploadIds = ((UIUploadInput) child).getUploadIds();
          for (String uploadId : uploadIds) {
            uploadService.removeUploadResource(uploadId);
          }
          String childName = ((UIUploadInput) child).getName();
          if (!childName.equals(UIAddOnWizard.ADDON_IMG_0) && !childName.equals(UIAddOnWizard.ADDON_AVATAR)) {
            uiAddOnWizard.removeChildById(child.getId());
          }

        }

      }

      // publish
      WCMPublicationService wcmPublicationService = (WCMPublicationService) ExoContainerContext.getCurrentContainer()
                                                                                               .getComponentInstanceOfType(WCMPublicationService.class);
      wcmPublicationService.updateLifecyleOnChangeContent(currentNode,
                                                          Util.getPortalRequestContext()
                                                              .getSiteName(),
                                                          Util.getPortalRequestContext()
                                                              .getRemoteUser(),
                                                          PublicationDefaultStates.PUBLISHED);

      UIPopupContainer uiPopupContainer = uiAddOnSearchEdit.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      
      UIAddOnSearchPageLayout uiAddOnSearchPageLayout = uiPopupContainer.getAncestorOfType(UIAddOnSearchPageLayout.class);
      UIAddOnSearchResult uiAddOnSearchResult = uiAddOnSearchPageLayout.getChild(UIAddOnSearchResult.class);
      UIAddOnSearchOne addOnSearchOne = uiAddOnSearchResult.getChildById(uiAddOnSearchEdit.getNodeId());
      try {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
        event.getRequestContext().addUIComponentToUpdateByAjax(addOnSearchOne);
        
        //execute JS from UIAddOnSearchOne.gtmpl to update CoverImage animation
        event.getRequestContext().getJavascriptManager().addJavascript("loadCoverImage()");
      } catch (Exception e) {
        // init addons list
        // and show directly my addons

        UIAddOnSearchForm.REFRESH = false;
        UIAddOnSearchForm.filterSelected = "myaddons";
        uiAddOnSearchResult.showMyAddons();  
        event.getRequestContext()
             .addUIComponentToUpdateByAjax(uiAddOnSearchPageLayout);
      }
    }
  }

  @Override
  public void activate() {
    try {
      this.init();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void deActivate() {
    // TODO Auto-generated method stub

  }

}
