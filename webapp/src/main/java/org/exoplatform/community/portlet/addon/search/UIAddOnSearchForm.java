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


import org.exoplatform.portal.webui.util.Util;

import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;


@ComponentConfig(
		lifecycle = UIFormLifecycle.class,
		template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchForm.gtmpl", 
		events = { @EventConfig(listeners = UIAddOnSearchForm.SearchActionListener.class),
					@EventConfig(listeners = UIAddOnSearchForm.SortActionListener.class) 
		}
	)
public class UIAddOnSearchForm extends UIForm {

	/** The Constant KEYWORD_INPUT. */
	public static final String KEYWORD_INPUT = "keywordInput";
	
	/** The Constant MESSAGE_NOT_SUPPORT_KEYWORD. */
	public static final String MESSAGE_NOT_SUPPORT_KEYWORD     = "UISearchForm.message.keyword-not-support";
	
	  /** The Constant MESSAGE_NOT_EMPTY_KEYWORD. */
	public static final String MESSAGE_NOT_EMPTY_KEYWORD       = "UISearchForm.message.keyword-not-empty";

	public static String filterSelected = "";
	public static Boolean REFRESH = true;
	private Boolean btnBackToAddonsVisible = false;
	
	public UIAddOnSearchForm() throws Exception {

		UIFormStringInput uiKeywordInput = new UIFormStringInput(KEYWORD_INPUT,KEYWORD_INPUT, null);
		uiKeywordInput.setHTMLAttribute("placeholder","Search");

		addUIFormInput(uiKeywordInput);
	
	}
  public void processRender(WebuiRequestContext context) throws Exception {
    if (REFRESH)
      filterSelected = "";
    super.processRender(context);
  }
	
	public Boolean isMyAddonsVisible(){
		String userId = Util.getPortalRequestContext().getRemoteUser();
		if(userId != null)
			return true;
		
		return false;
	}
	public String getStyleFilterSelected(String strIn){
		
		if(UIAddOnSearchForm.filterSelected.equals(strIn))
			return "btnFilter btn active";
		
		return "actionIcon btnFilter";
	}
	public void setBtnBackToAddonsVisible(Boolean visible){
		this.btnBackToAddonsVisible = visible;
	}
	public Boolean getBtnBackToAddonsVisible(){
		return this.btnBackToAddonsVisible;
	}
	

	public static class SearchActionListener extends EventListener<UIAddOnSearchForm> {

		@Override
		public void execute(Event<UIAddOnSearchForm> event) throws Exception {

			UIAddOnSearchForm uiAddOnSearchForm = event.getSource();
      UIAddOnSearchForm.REFRESH=true;
			UIAddOnSearchResult.REFRESH = false;
			UIAddOnSearchPageLayout uiAddonsSearchPageContainer = (UIAddOnSearchPageLayout)uiAddOnSearchForm.getParent();
			UIAddOnSearchResult uiAddOnSearchResult = uiAddonsSearchPageContainer.getChildById(UIAddOnSearchPageLayout.SEARCH_RESULT);
			
			PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();

			UIFormStringInput uiKeywordInput = uiAddOnSearchForm.getUIStringInput(UIAddOnSearchForm.KEYWORD_INPUT);				
			String keyword = uiKeywordInput.getValue();
			if(keyword != null){
				
				keyword = keyword.replace('-', ' ').toLowerCase(portletRequestContext.getLocale());
			    keyword = keyword.replaceAll("'","''");				
			
			}else
				keyword = "";
			uiAddOnSearchResult.clearResult();
			uiAddOnSearchResult.setKeyword(keyword);

			uiAddOnSearchResult.doSearch();
			uiAddonsSearchPageContainer.manageView(UIAddOnSearchPageLayout.SEARCH_RESULT);
            portletRequestContext.addUIComponentToUpdateByAjax(uiAddonsSearchPageContainer);
			
			
		}

	}
	public static class SortActionListener extends EventListener<UIAddOnSearchForm> {

		@Override
		public void execute(Event<UIAddOnSearchForm> event) throws Exception {
      UIAddOnSearchForm.REFRESH=false;
			UIAddOnSearchForm uiAddOnSearchForm = event.getSource();
			String strSortOrder = event.getRequestContext().getRequestParameter(OBJECTID);
			
			PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();			
			UIAddOnSearchPageLayout uiAddonsSearchPageContainer = (UIAddOnSearchPageLayout)uiAddOnSearchForm.getParent();
			UIAddOnSearchResult uiAddOnSearchResult = uiAddonsSearchPageContainer.getChildById(UIAddOnSearchPageLayout.SEARCH_RESULT);			

			if(strSortOrder.equals("myaddons")){
				
				UIAddOnSearchForm.filterSelected="myaddons";	
				uiAddOnSearchResult.showMyAddons();
					
			}else if(strSortOrder.equals("za")){
				uiAddOnSearchResult.SortAddons("za");
				UIAddOnSearchForm.filterSelected="za";					
			}
			else{
        uiAddOnSearchResult.SortAddons("az");
				UIAddOnSearchForm.filterSelected="az";				
			}
			uiAddonsSearchPageContainer.manageView(UIAddOnSearchPageLayout.SEARCH_RESULT);	
			portletRequestContext.addUIComponentToUpdateByAjax(uiAddonsSearchPageContainer);	
			
		}
	}
	
}
