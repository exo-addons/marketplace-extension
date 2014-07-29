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

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

@ComponentConfig(
		  lifecycle = Lifecycle.class,
		  template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchPageLayout.gtmpl"
		)
public class UIAddOnSearchPageLayout extends UIContainer{

	
	  /** The Constant SEARCH_FORM. */
	  public static final String SEARCH_FORM   = "uiAddOnSearchForm";

	  /** The Constant SEARCH_RESULT. */
	  public static final String SEARCH_RESULT = "UIAddOnSearchResult";

	  public static final String ADDON_DETAIL = "uiAddOnDetail";
	  
	  public static final  String POPUP_Window_ID = "UIPopupEditAddon";
	  
	  public static final  String EDIT_FORM_ID = "UIEditFormId";    

	
	public UIAddOnSearchPageLayout() throws Exception{

	  UIAddOnSearchForm.REFRESH = true;
		addChild(UIAddOnSearchForm.class,null,UIAddOnSearchPageLayout.SEARCH_FORM);
		UIAddOnDetail uiAddOnDetail = addChild(UIAddOnDetail.class, null, UIAddOnSearchPageLayout.ADDON_DETAIL);
		uiAddOnDetail.setRendered(false);
	  UIAddOnSearchResult.REFRESH = true;
	  addChild(UIAddOnSearchResult.class,null,UIAddOnSearchPageLayout.SEARCH_RESULT);
		addChild(UIPopupContainer.class, null, POPUP_Window_ID);
		
			
	}
	public void manageView(String child){
		
		UIAddOnSearchForm uiAddOnSearchForm = this.getChildById(UIAddOnSearchPageLayout.SEARCH_FORM);
		UIAddOnSearchResult uiAddOnSearchResult = this.getChildById(UIAddOnSearchPageLayout.SEARCH_RESULT);
		UIAddOnDetail  uiAddOnDetail = this.getChildById(UIAddOnSearchPageLayout.ADDON_DETAIL);

		if(child.equals(UIAddOnSearchPageLayout.SEARCH_RESULT)){
			uiAddOnSearchForm.setBtnBackToAddonsVisible(false);
			uiAddOnDetail.setRendered(false);
			uiAddOnSearchResult.setRendered(true);

			
		}
		else if(child.equals(UIAddOnSearchPageLayout.ADDON_DETAIL)){
			uiAddOnSearchForm.setBtnBackToAddonsVisible(true);
			uiAddOnSearchResult.setRendered(false);
			uiAddOnDetail.setRendered(true);
		}
		
		
		
	}
	

	
	
	
}
