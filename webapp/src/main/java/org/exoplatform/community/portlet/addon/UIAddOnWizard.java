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


import org.exoplatform.addon.marketplace.Constants;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.addon.service.AddOnService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.wcm.webui.validator.MandatoryValidator;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.input.UIUploadInput;
import org.exoplatform.webui.form.validator.EmailAddressValidator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;


public class UIAddOnWizard extends UIFormInputSet{
	private static final Log log = ExoLogger.getLogger(UIAddOnWizard.class);
	public static String ADDON_TITLE = "title";
	public static String ADDON_DESCRIPTION = "description";
	public static String ADDON_EMAIL = "email";
	public static String ADDON_VERSION = "version";
	public static String ADDON_LICENSE = "license";
	public static String ADDON_AUTHOR = "author";
	public static String ADDON_COMPABILITY = "compatibility";
	public static String ADDON_SOURCE_URL = "sourceUrl";
	public static String ADDON_DOCUMENT_URL = "documentUrl";
	public static String ADDON_DOWNLOAD_URL = "downloadUrl";
	public static String ADDON_HOSTED = "hosted";
	public static String ADDON_IMG_0 = "img0";
	public static String ADDON_CODE_URL = "codeUrl"; 
	public static String ADDON_DEMO_URL = "demoUrl"; 
	public static String ADDON_INSTALL_COMMAND = "installCommand"; 
	public static String ADDON_AVATAR = "avatar";
	public static String ADDON_CATEGORY = "category";
	MarketPlaceService marketPlaceService = null;
	
	public UIAddOnWizard(String id) throws Exception{
		marketPlaceService = getApplicationComponent(MarketPlaceService.class);
		setId(id) ;
		setComponentConfig(getClass(), null);
		reset();	
		String email = "";
		String userId = Util.getPortalRequestContext().getRemoteUser();  		
		String displayName = "";
		if(userId != null){
			
			OrganizationService orgService = getApplicationComponent(OrganizationService.class);
			User user = orgService.getUserHandler().findUserByName(userId);
			if(user != null){
				
				email = user.getEmail();
				displayName = user.getFirstName()+" "+user.getLastName();
			}					
		}
		else{
			
			userId = "";
		}
	    
		UIFormStringInput titleInput = (UIFormStringInput) new UIFormStringInput(ADDON_TITLE, null, "").addValidator(MandatoryValidator.class);
	    UIFormStringInput emailInput = (UIFormStringInput) new UIFormStringInput(ADDON_EMAIL, null, email).addValidator(MandatoryValidator.class).addValidator( EmailAddressValidator.class);
	    if(email == null || email.equals("")){
	    	
	    	emailInput.setHTMLAttribute("placeholder","for internal use only");
	    }
	    
	    UIFormRichtextInput descriptionRichTextInput = (UIFormRichtextInput) new UIFormRichtextInput(ADDON_DESCRIPTION , null, "").addValidator(MandatoryValidator.class);	   
	    descriptionRichTextInput.setHTMLAttribute("placeholder","will be used on the add-on page");
	    
	    UIFormStringInput versionInput = new UIFormStringInput(ADDON_VERSION, null, "");
	    versionInput.setHTMLAttribute("placeholder","1.0");
	    UIFormStringInput licenseInput = new UIFormStringInput(ADDON_LICENSE, null, null);
	    UIFormStringInput authorInput = new UIFormStringInput(ADDON_AUTHOR, null,displayName);
	    if(displayName == null || displayName.equals("")){
	    	
	    	authorInput.setHTMLAttribute("placeholder","published with the add-on");
	    	
	    }
	    UIFormStringInput compatibilityInput = new UIFormStringInput(ADDON_COMPABILITY, null, "");
	    compatibilityInput.setHTMLAttribute("placeholder","version of platform");
	    
	    UIFormStringInput sourceUrlInput = new UIFormStringInput(ADDON_SOURCE_URL, null, null);
	    UIFormStringInput documentUrlInput = new UIFormStringInput(ADDON_DOCUMENT_URL, null, null);
	    UIFormStringInput downloadUrlInput = (UIFormStringInput) new UIFormStringInput(ADDON_DOWNLOAD_URL, null, "").addValidator(MandatoryValidator.class);
	    downloadUrlInput.setHTMLAttribute("placeholder","http://");
	    
	    UIFormStringInput codeUrlInput = new UIFormStringInput(ADDON_CODE_URL, null, null);
	    codeUrlInput.setHTMLAttribute("placeholder","http://");
	    UIFormStringInput demoUrlInput = new UIFormStringInput(ADDON_DEMO_URL, null, null);
	    demoUrlInput.setHTMLAttribute("placeholder","http://");
	    UIFormStringInput installCommandInput = new UIFormStringInput(ADDON_INSTALL_COMMAND, null, null);
	    
	    UICheckBoxInput   hostedCbInput = new UICheckBoxInput(ADDON_HOSTED, null, false);
	    
	    UIUploadInput avatarUploadInput = new UIUploadInput(ADDON_AVATAR, "");
	    
	    UIUploadInput imgUploadInput = new UIUploadInput(ADDON_IMG_0, "img0");

		//---Build categories
		//--- Add DropDown to display categories
		List<Category> categories = marketPlaceService.findAllCategories();

		//--- Init tje list each time the combobox is displayed
		List<SelectItemOption<String>> categoriesOptions = new ArrayList<SelectItemOption<String>>();

		//--- Fill categories
		for (Category category : categories) {
			categoriesOptions.add(new SelectItemOption<String>(category.getName(),category.getName()));
		}

		UIFormSelectBox categorySelectBox = new UIFormSelectBox(ADDON_CATEGORY, ADDON_CATEGORY, categoriesOptions);
      
	    addChild(titleInput);
	    addChild(descriptionRichTextInput);
	    addChild(versionInput);
		addChild(categorySelectBox);
	    
	    addChild(licenseInput);
	    addChild(compatibilityInput);
	    addChild(sourceUrlInput);
	    
	    addChild(documentUrlInput);
	    addChild(downloadUrlInput);
	    addChild(authorInput);
	    addChild(emailInput);
	    
	    addChild(codeUrlInput);
	    addChild(demoUrlInput);
	    addChild(installCommandInput);
	    
	    addChild(hostedCbInput);
	    addChild(avatarUploadInput);  
	    addChild(imgUploadInput);
	}
	
	public void initVals(Node aNode){

		if(null != aNode) {

			String propertyName = null;
			String txt = null;
			String[] properties= {ADDON_TITLE,ADDON_DESCRIPTION,ADDON_DOWNLOAD_URL,ADDON_CODE_URL,ADDON_DEMO_URL,ADDON_INSTALL_COMMAND,ADDON_DOCUMENT_URL,ADDON_SOURCE_URL,ADDON_COMPABILITY,ADDON_LICENSE,ADDON_VERSION,ADDON_AUTHOR,ADDON_EMAIL,ADDON_CATEGORY};
			for(int i =0; i < properties.length; i++) {
				propertyName = properties[i];
				
				try {
					//--- Manage Category : set selected category
					if (propertyName.equalsIgnoreCase(ADDON_CATEGORY)) {
						txt = AddOnService.getStrProperty(aNode, Constants.ADDON_MIXIN_PROPPERTY_NAME);
						((UIFormSelectBox)this.getUIFormSelectBox(propertyName)).setValue(txt);
					} else {
						txt = AddOnService.getStrProperty(aNode,"exo:"+propertyName);
						if(null != txt){
							if(ADDON_DESCRIPTION.equals(propertyName)){
								((UIFormRichtextInput)this.getChildById(propertyName)).setValue(txt);
							}else{
								this.getUIStringInput(propertyName).setValue(txt);
							}
						}
					}

				} catch (RepositoryException e) {
					log.error("ERR init vals for edit addon "+propertyName);
				}
			}
			//check to display avatar image and uploadAvatar component
			try {
				String avatarImageUrl = AddOnService.getAvatarNode(aNode);
        		UIComponent uploadAvatar = null;
				List<UIComponent> listChildren = this.getChildren();
				for (UIComponent child : listChildren) {
				  if(child instanceof UIUploadInput && child.getName().equals(ADDON_AVATAR)){
					uploadAvatar = child;
				  }
				}

				if(null!=avatarImageUrl && avatarImageUrl.length()>0) {
				  uploadAvatar.setRendered(false);
				} else {
				  uploadAvatar.setRendered(true);
				}
      		} catch (Exception e) {
				log.error("ERR init vals for edit addon avatar",e);
      		}
			
		}
		
	}
}
