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


import org.exoplatform.addon.marketplace.Constants;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.collaboration.RenameConnector;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIDropDownControl;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;



@ComponentConfigs({
  @ComponentConfig(
                   lifecycle = UIFormLifecycle.class,
                   template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchForm.gtmpl", 
                   events = { @EventConfig(listeners = UIAddOnSearchForm.SearchActionListener.class),
                         @EventConfig(listeners = UIAddOnSearchForm.SortActionListener.class) 
                   }
                 ),
  @ComponentConfig(
    type = UIDropDownControl.class, 
    id = "DisplayModesDropDown", 
    template = "system:/groovy/webui/core/UIDropDownControl.gtmpl",
    events = {
      @EventConfig(listeners = UIAddOnSearchForm.ChangeOptionActionListener.class)
    }
  ),
        @ComponentConfig(
                type = UIDropDownControl.class,
                id = "CategoryNameDropDown",
                template = "system:/groovy/webui/core/UIDropDownControl.gtmpl",
                events = {
                        @EventConfig(listeners = UIAddOnSearchForm.ChangeCategoryActionListener.class)
                }
        )
})
public class UIAddOnSearchForm extends UIForm implements Constants {

    private static final Log LOG                      = ExoLogger.getLogger(UIAddOnSearchForm.class.getName());

	public static String filterSelected = "";
	public static Boolean REFRESH = true;
	private Boolean btnBackToAddonsVisible = false;

    //--- Categories dropbox
    UIDropDownControl categoryNameDropDown = null;
    List<SelectItemOption<String>> categoriesList = null;
    MarketPlaceService marketPlaceService = null;

    public UIAddOnSearchForm() throws Exception {

        marketPlaceService = getApplicationComponent(MarketPlaceService.class);

		UIFormStringInput uiKeywordInput = new UIFormStringInput(KEYWORD_INPUT,KEYWORD_INPUT, null);
		uiKeywordInput.setHTMLAttribute("placeholder","Search");

		addUIFormInput(uiKeywordInput);
		ResourceBundle resourceBundle =  WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
		
		List<SelectItemOption<String>> displayModes = new ArrayList<SelectItemOption<String>>(4);

        displayModes.add(new SelectItemOption<String>(resourceBundle.getString("UIAddOnSearchPortlet.label.sort-" + FILTER_POPULAR), FILTER_POPULAR));
        displayModes.add(new SelectItemOption<String>(resourceBundle.getString("UIAddOnSearchPortlet.label.sort-" + FILTER_AZ), FILTER_AZ));
        displayModes.add(new SelectItemOption<String>(resourceBundle.getString("UIAddOnSearchPortlet.label.sort-" + FILTER_ZA), FILTER_ZA));
        displayModes.add(new SelectItemOption<String>(resourceBundle.getString("UIAddOnSearchPortlet.label.sort-" + FILTER_LATEST), FILTER_LATEST));
    
        UIDropDownControl uiDropDownControl = addChild(UIDropDownControl.class, "DisplayModesDropDown", null);
        uiDropDownControl.setOptions(displayModes);
    
        setSelectedMode(uiDropDownControl);

        //--- Add DropDown to display categories
        categoryNameDropDown = addChild(UIDropDownControl.class, "CategoryNameDropDown", null);

    
    //addChild(uiDropDownControl);
	
	}
  public void processRender(WebuiRequestContext context) throws Exception {
      if (REFRESH)
          filterSelected = "";

      //--- Add DropDown to display categories
      List<Category> categories = marketPlaceService.findAllCategories();

      //--- Init tje list each time the combobox is displayed
      categoriesList = new ArrayList<SelectItemOption<String>>();

      //--- Add «EMPTY» value each time the combo-box
      categoriesList.add(new SelectItemOption<String>("EMPTY","EMPTY"));

      //--- Fill categories
      for (Category category : categories) {
          categoriesList.add(new SelectItemOption<String>(category.getName(),category.getName()));
      }
      categoryNameDropDown.setOptions(categoriesList);
      //--- FIN categories box management

      super.processRender(context);
  }
	
  private void setSelectedMode(UIDropDownControl uiDropDownControl) {
    if (filterSelected != null && filterSelected.length()>0 && !filterSelected.equals(FILTER_MY_ADDONS)) {
      uiDropDownControl.setValue(filterSelected);
    }else{
      uiDropDownControl.setValue(FILTER_POPULAR);
    }
  }
  
	public Boolean isMyAddonsVisible(){
		String userId = Util.getPortalRequestContext().getRemoteUser();
		if(userId != null)
			return true;
		
		return false;
	}
    /**
     *
     * @return true if current user is administrative user; false if current user is normal user
     */
    public Boolean isAdminUser(){
        UserACL userACL = getService(UserACL.class);
        return userACL.isUserInGroup(userACL.getAdminGroups());
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
			
			//DisplayModesDropDown
			UIDropDownControl uiDropDownControl = uiAddOnSearchForm.getChild(UIDropDownControl.class);
			String strSortOrder = uiDropDownControl.getOptions().get(uiDropDownControl.getSelectedIndex()).getValue();
			//uiAddOnSearchResult.set
			
			if(strSortOrder.equals("myaddons")){
        if(UIAddOnSearchForm.filterSelected.equals("myaddons")){
          uiAddOnSearchResult.SortAddons("popular");
          UIAddOnSearchForm.filterSelected="popular";
        }else{
          UIAddOnSearchForm.filterSelected="myaddons";  
          uiAddOnSearchResult.showMyAddons();
        }
          
      }else if(strSortOrder.equals("za")){
        uiAddOnSearchResult.SortAddons("za");
        UIAddOnSearchForm.filterSelected="za";          
      }else if(strSortOrder.equals("az")){
        uiAddOnSearchResult.SortAddons("az");
        UIAddOnSearchForm.filterSelected="az";        
      }else if(strSortOrder.equals("latest")){
        uiAddOnSearchResult.SortAddons("latest");
        UIAddOnSearchForm.filterSelected="latest";        
      }else{
        //Sort by vote
        uiAddOnSearchResult.SortAddons("popular");
        UIAddOnSearchForm.filterSelected="popular";     
      }
			

			//uiAddOnSearchResult.doSearch();
			
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
			
			//BEGIN get and set keyword for keep search
			UIFormStringInput uiKeywordInput = uiAddOnSearchForm.getUIStringInput(UIAddOnSearchForm.KEYWORD_INPUT);        
      String keyword = uiKeywordInput.getValue();
      if(keyword != null){
        
        keyword = keyword.replace('-', ' ').toLowerCase(portletRequestContext.getLocale());
          keyword = keyword.replaceAll("'","''");       
      
      }else
        keyword = "";
      uiAddOnSearchResult.clearResult();
      uiAddOnSearchResult.setKeyword(keyword);
      //END get and set keyword for keep search

			if(strSortOrder.equals("myaddons")){
				if(UIAddOnSearchForm.filterSelected.equals("myaddons")){
				  uiAddOnSearchResult.SortAddons("popular");
	        UIAddOnSearchForm.filterSelected="popular";
				}else{
  				UIAddOnSearchForm.filterSelected="myaddons";	
  				uiAddOnSearchResult.showMyAddons();
				}
					
			}else if(strSortOrder.equals("za")){
				uiAddOnSearchResult.SortAddons("za");
				UIAddOnSearchForm.filterSelected="za";					
			}else if(strSortOrder.equals("az")){
        uiAddOnSearchResult.SortAddons("az");
				UIAddOnSearchForm.filterSelected="az";				
			}else if(strSortOrder.equals("latest")){
        uiAddOnSearchResult.SortAddons("latest");
        UIAddOnSearchForm.filterSelected="latest";        
      }else{
        //Sort by vote
        uiAddOnSearchResult.SortAddons("popular");
        UIAddOnSearchForm.filterSelected="popular";     
      }
			uiAddonsSearchPageContainer.manageView(UIAddOnSearchPageLayout.SEARCH_RESULT);	
			portletRequestContext.addUIComponentToUpdateByAjax(uiAddonsSearchPageContainer);	
			
		}
	}
	
	public static class ChangeOptionActionListener extends EventListener<UIDropDownControl> {

    public void execute(Event<UIDropDownControl> event) throws Exception {
      UIAddOnSearchForm.REFRESH=false;
      UIDropDownControl uiDropDownControl = event.getSource();
      UIAddOnSearchForm uiAddOnSearchForm = (UIAddOnSearchForm)uiDropDownControl.getParent();
      
      WebuiRequestContext requestContext = event.getRequestContext();
      String strSortOrder = requestContext.getRequestParameter(OBJECTID);
      
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();      
      UIAddOnSearchPageLayout uiAddonsSearchPageContainer = (UIAddOnSearchPageLayout)uiAddOnSearchForm.getParent();
      UIAddOnSearchResult uiAddOnSearchResult = uiAddonsSearchPageContainer.getChildById(UIAddOnSearchPageLayout.SEARCH_RESULT);      

      if(strSortOrder.equals("myaddons")){
        if(UIAddOnSearchForm.filterSelected.equals("myaddons")){
          uiAddOnSearchResult.SortAddons("popular");
          UIAddOnSearchForm.filterSelected="popular";
        }else{
          UIAddOnSearchForm.filterSelected="myaddons";  
          uiAddOnSearchResult.showMyAddons();
        }
          
      }else if(strSortOrder.equals("za")){
        uiAddOnSearchResult.SortAddons("za");
        UIAddOnSearchForm.filterSelected="za";          
      }else if(strSortOrder.equals("az")){
        uiAddOnSearchResult.SortAddons("az");
        UIAddOnSearchForm.filterSelected="az";        
      }else if(strSortOrder.equals("latest")){
        uiAddOnSearchResult.SortAddons("latest");
        UIAddOnSearchForm.filterSelected="latest";        
      }else{
        //Sort by vote
        uiAddOnSearchResult.SortAddons("popular");
        UIAddOnSearchForm.filterSelected="popular";     
      }
      uiDropDownControl.setValue(strSortOrder);
      
      uiAddonsSearchPageContainer.manageView(UIAddOnSearchPageLayout.SEARCH_RESULT);  
      portletRequestContext.addUIComponentToUpdateByAjax(uiAddonsSearchPageContainer);  
    }
	}


    public static class ChangeCategoryActionListener extends EventListener<UIDropDownControl> {

        public void execute(Event<UIDropDownControl> event) throws Exception {

            UIAddOnSearchForm.REFRESH=false;
            UIDropDownControl categoryNameDropDown = event.getSource();
            UIAddOnSearchForm uiAddOnSearchForm = (UIAddOnSearchForm)categoryNameDropDown.getParent();

            WebuiRequestContext requestContext = event.getRequestContext();

            //--- Get selected category
            String categoryName = requestContext.getRequestParameter(OBJECTID);

            PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
            UIAddOnSearchPageLayout uiAddonsSearchPageContainer = (UIAddOnSearchPageLayout)uiAddOnSearchForm.getParent();
            UIAddOnSearchResult uiAddOnSearchResult = uiAddonsSearchPageContainer.getChildById(UIAddOnSearchPageLayout.SEARCH_RESULT);

            //--- Launch JCR request to get addons by category
            uiAddOnSearchResult.showAddonsByCategory(categoryName);
            //--- END request
            categoryNameDropDown.setValue(categoryName);

            uiAddonsSearchPageContainer.manageView(UIAddOnSearchPageLayout.SEARCH_RESULT);
            portletRequestContext.addUIComponentToUpdateByAjax(uiAddonsSearchPageContainer);
        }
    }

   /**
     * Gets the service.
     *
     * @param clazz the clazz
     *
     * @return the service
     */
    public static <T> T getService(Class<T> clazz) {
        return getService(clazz, null);
    }

    /**
     * Gets the service.
     *
     * @param clazz the class
     * @param containerName the container's name
     *
     * @return the service
     */
    public static <T> T getService(Class<T> clazz, String containerName) {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        if (containerName != null) {
            container = RootContainer.getInstance().getPortalContainer(containerName);
        }
        if (container.getComponentInstanceOfType(clazz)==null) {
            containerName = PortalContainer.getCurrentPortalContainerName();
            container = RootContainer.getInstance().getPortalContainer(containerName);
        }
        return clazz.cast(container.getComponentInstanceOfType(clazz));
    }

	
}
