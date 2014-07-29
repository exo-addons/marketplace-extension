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
package org.exoplatform.community.portlet.addon;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.community.portlet.addon.search.UIAddOnSearchEdit;
import org.exoplatform.community.portlet.addon.search.UIAddOnSearchForm;
import org.exoplatform.community.portlet.addon.search.UIAddOnSearchResult;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.input.UIUploadInput;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.addon.service.AddOnService;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/templates/AddOnPortlet/UIAddOnForm.gtmpl", events = {
    @EventConfig(listeners = UIAddOnForm.SubmitActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnForm.RemoveActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIAddOnForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UIAddOnForm extends UIForm {

  private static final Log   log            = ExoLogger.getLogger(UIAddOnForm.class);

  final static public String wizard_form_id = "uiAddOnWizard";

  private int                imgCount       = 1;

  public UIAddOnForm() throws Exception {

    UIAddOnWizard uiAddOnWizard = new UIAddOnWizard(UIAddOnForm.wizard_form_id);
    addUIComponentInput(uiAddOnWizard);

  }

  public static class SubmitActionListener extends EventListener<UIAddOnForm> {
    public void execute(Event<UIAddOnForm> event) throws Exception {

      UIAddOnForm uiAddOnForm = event.getSource();
      UIAddOnWizard uiAddOnWizard = uiAddOnForm.getChildById(wizard_form_id);

      UIAddOnPortlet uiPortlet = uiAddOnForm.getAncestorOfType(UIAddOnPortlet.class);
      UIApplication uiApp = uiAddOnForm.getAncestorOfType(UIApplication.class);

      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      InputStream[] inputStreams = null;
      Node currentNode = null;

      Map<String, String> mapProperties = new HashMap<String, String>();

      String email = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_EMAIL).getValue();
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_EMAIL, email);

      String author = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_AUTHOR).getValue();
      if (author != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_AUTHOR, author);

      String description = ((UIFormRichtextInput) uiAddOnWizard.getChildById(UIAddOnWizard.ADDON_DESCRIPTION)).getValue();
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DESCRIPTION, description);

      String version = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_VERSION).getValue();
      if (version != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_VERSION, version);

      String license = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_LICENSE).getValue();
      if (license != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_LICENSE, license);

      String compatibility = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_COMPABILITY)
                                          .getValue();
      if (compatibility != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_COMPABILITY, compatibility);

      String sourceUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_SOURCE_URL).getValue();
      if (sourceUrl != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_SOURCE_URL, sourceUrl);

      String documentUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_DOCUMENT_URL)
                                        .getValue();
      if (documentUrl != null)
        mapProperties.put("exo:" + UIAddOnWizard.ADDON_DOCUMENT_URL, documentUrl);

      String downloadUrl = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_DOWNLOAD_URL)
                                        .getValue();
      mapProperties.put("exo:" + UIAddOnWizard.ADDON_DOWNLOAD_URL, downloadUrl);

      UICheckBoxInput hostedCb = (UICheckBoxInput) uiAddOnWizard.getUICheckBoxInput(UIAddOnWizard.ADDON_HOSTED);
      Boolean hosted = hostedCb.isChecked();

      String titleAddon = uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_TITLE).getValue();
      // Validate fields
      if (email == null || titleAddon == null || description == null || downloadUrl == null) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnPortlet.msg.invalid",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
        return;
      }
      if (!AddOnService.validateEmail(email)) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnPortlet.msg.invalidemail",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
        return;
      }
      try {
        URL url = new URL(downloadUrl);
      } catch (MalformedURLException e) {
        uiApp.addMessage(new ApplicationMessage("UIAddOnPortlet.msg.malformurl",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
        return;
      }
      
      //validate screenshot images
      List<UIComponent> listChildren = new ArrayList<UIComponent>();
      listChildren = uiAddOnWizard.getChildren();
      for (UIComponent child : listChildren) {
        if (child instanceof UIUploadInput) {
          child = (UIUploadInput) child;
          UploadResource[] uploadResource = ((UIUploadInput) child).getUploadResources();
          inputStreams = ((UIUploadInput) child).getUploadDataAsStreams();
          if (uploadResource.length > 0) {
            String imgMineType = uploadResource[0].getMimeType();
            if(imgMineType.substring(0,imgMineType.lastIndexOf("/")).equals("image") == false){
              uiApp.addMessage(new ApplicationMessage("UIAddOnPortlet.msg.invalidImage",
                                                      null,
                                                      ApplicationMessage.WARNING));
              event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
              return;
            }
          }
        }
      }

      String nameAddon = titleAddon.replaceAll(" ", "-").toLowerCase();
      try {
        currentNode = AddOnService.storeNode(titleAddon, nameAddon, hosted, mapProperties, true);
      } catch (Exception e) {
        log.debug("Exceptions happen while storing data",e);
      }

      for (UIComponent child : listChildren) {

        if (child instanceof UIUploadInput) {
          child = (UIUploadInput) child;

          UploadResource[] uploadResource = ((UIUploadInput) child).getUploadResources();
          inputStreams = ((UIUploadInput) child).getUploadDataAsStreams();

          if (inputStreams.length == 0 && listChildren.size() == 1) {

            // TODO : Using default image
          }
          if (uploadResource.length > 0) {
            String imgFileName = uploadResource[0].getFileName();
            imgFileName = imgFileName.replaceAll("[^a-zA-Z0-9.-]", "-");
            String imgMineType = uploadResource[0].getMimeType();

            Node imageNode = currentNode.addNode("medias/images/" + imgFileName, "nt:file");
            Node imageContent = imageNode.addNode("jcr:content", "nt:resource");

            imageContent.setProperty("jcr:data", inputStreams[0]);
            imageContent.setProperty("jcr:mimeType", imgMineType);
            imageContent.setProperty("jcr:lastModified", Calendar.getInstance());
          }
        }
      }

      try {

        currentNode.getSession().save();

        // publish
        WCMPublicationService wcmPublicationService = (WCMPublicationService) ExoContainerContext.getCurrentContainer()
                                                                                                 .getComponentInstanceOfType(WCMPublicationService.class);
        wcmPublicationService.updateLifecyleOnChangeContent(currentNode,
                                                            Util.getPortalRequestContext()
                                                                .getSiteName(),
                                                            Util.getPortalRequestContext()
                                                                .getRemoteUser(),
                                                            PublicationDefaultStates.PUBLISHED);

        // reset
        for (UIComponent child : listChildren) {
          if (child instanceof UIUploadInput) {
            UploadService uploadService = uiAddOnForm.getApplicationComponent(UploadService.class);
            String[] uploadIds = ((UIUploadInput) child).getUploadIds();
            for (String uploadId : uploadIds) {
              uploadService.removeUploadResource(uploadId);
            }
            if (!((UIUploadInput) child).getName().equals(UIAddOnWizard.ADDON_IMG_0)) {
              uiAddOnWizard.removeChildById(child.getId());
            }

          }

        }
        uiAddOnWizard.reset();
        if (author != null)
          uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_AUTHOR).setValue(author);
        uiAddOnWizard.getUIStringInput(UIAddOnWizard.ADDON_EMAIL).setValue(email);

        /**
         * TODO send notification of submission addon to eXo marketing and to
         * the person who submit the add-on
         */
         //AddOnService.sendRequestReceiveMail(email,
         //Utils.getPortletPreference(UIAddOnPortlet.PREFERENCE_FROM) );
         //AddOnService.SendConfirmationAddonPublishedEmail(email, nameAddon);
   
        PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
        HttpServletRequest httpServletRequest= portalRequestContext.getRequest();
        String hostName = httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + ":" + httpServletRequest.getServerPort();
        
        AddOnService.sendNewAddonSubmisson(Utils.getPortletPreference(UIAddOnPortlet.PREFERENCE_RECEIVER),
                                        Utils.getPortletPreference(UIAddOnPortlet.PREFERENCE_FROM),
                                        Utils.getPortletPreference(UIAddOnPortlet.PREFERENCE_EMAIL_SUBJECT),
                                        email,
                                        titleAddon,
                                        description,
                                        version,
                                        license,
                                        author,
                                        compatibility,
                                        sourceUrl,
                                        documentUrl,
                                        downloadUrl,
                                        hosted,hostName);

      } catch (Exception e) {
        log.error(e.getMessage());
      }
      uiApp.addMessage(new ApplicationMessage("UIAddOnPortlet.msg.save-successful",
                                              null,
                                              ApplicationMessage.INFO));
      // init addons list
      UIAddOnSearchResult.REFRESH = true;
      UIAddOnSearchForm.REFRESH = true;
      UIAddOnPortlet portlet = uiAddOnForm.getAncestorOfType(UIAddOnPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(portlet);

    }
  }

  public static class CancelActionListener extends EventListener<UIAddOnForm> {
    public void execute(Event<UIAddOnForm> event) throws Exception {

      UIAddOnForm uiAddOnForm = event.getSource();
      uiAddOnForm.reset();

    }
  }

  static public class AddActionListener extends EventListener<UIAddOnForm> {
    public void execute(Event<UIAddOnForm> event) throws Exception {
      UIAddOnForm uiAddOnForm = event.getSource();

      String componentName = "img" + uiAddOnForm.imgCount;
      uiAddOnForm.imgCount++;
      UIUploadInput Upload = new UIUploadInput(componentName, componentName);
      UIAddOnWizard uiAddOnWizard = uiAddOnForm.getChildById(UIAddOnForm.wizard_form_id);
      uiAddOnWizard.addUIFormInput(Upload);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
    }
  }

  static public class RemoveActionListener extends EventListener<UIAddOnForm> {
    public void execute(Event<UIAddOnForm> event) throws Exception {
      UIAddOnForm uiAddOnForm = event.getSource();
      uiAddOnForm.imgCount--;
      UIAddOnWizard uiAddOnWizard = uiAddOnForm.getChildById(wizard_form_id);

      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      uiAddOnWizard.removeChildById(id);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddOnForm);
    }
  }

}
