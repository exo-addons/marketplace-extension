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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addon.service.AddOnService;
import org.exoplatform.addon.service.model.Addon;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;


@ComponentConfig(
		lifecycle = Lifecycle.class,
		template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchOne.gtmpl",
		events = { 
			@EventConfig(listeners = UIAddOnSearchOne.EditActionListener.class),
			@EventConfig(listeners = UIAddOnSearchOne.DetailActionListener.class)			
			
			}	
	)
public class UIAddOnSearchOne extends UIContainer {
  
  private Addon addon;
		
	private String nodeId;
	private Boolean canEdit = false;
	public UIAddOnSearchOne() throws Exception{
		
	}
	
	
	public Addon getAddon() {
    return addon;
  }


  public void setAddon(Addon addon) {
    this.addon = addon;
  }
  
  public void loadData(){
    setAddon(AddOnService.getAddonFromCache(this.getId()));
  }

  public void setNodeId(String id){
		this.nodeId = id;
	}
	public String getNodeId(){
		return this.nodeId;
	}
	
	
	public Boolean getCanEdit() {
    return canEdit;
  }
	
  public void setCanEdit(Boolean canEdit) {
    this.canEdit = canEdit;
  }
  
  public Node getNode() throws PathNotFoundException, RepositoryException{
		return AddOnService.getNodeById(this.getNodeId());
	}

	public String getImageCover() throws PathNotFoundException, RepositoryException{

		return addon.getCoverImagePath();
		
	}
	
/*	public String getStrProperty(String propertyName) throws RepositoryException{
		
		return AddOnService.getStrProperty(this.getNode(), propertyName);
		
	}	*/
	
	public Boolean canEdit() throws RepositoryException{
		if(getCanEdit()){
		  return true;
		}
		String userId = Util.getPortalRequestContext().getRemoteUser();
		String ownerid = addon.getOwnerid();

		if(userId != null && ownerid != null && userId.equals(ownerid)){
			return true;	
		}	
		return false;
	}
	
	
	public String getURL() throws Exception {
	  if(StringUtils.isEmpty(addon.getSeeDetailUrl())==false){
	    return addon.getSeeDetailUrl();
	  }
	  
		Node node = this.getNode();
		String repository = WCMCoreUtils.getRepository().getConfiguration().getName();
		String workspace = node.getSession().getWorkspace().getName();
		String basePath = "addon-detail";
		String detailParameterName = "content-id";    
		
		StringBuffer path = new StringBuffer();
		path.append("/").append(repository).append("/").append(workspace);
		NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);   
		NavigationResource resource = new NavigationResource(SiteType.PORTAL,
		                                                     Util.getPortalRequestContext()
		                                                         .getPortalOwner(), basePath);
		nodeURL.setResource(resource);
		if (node.isNodeType("nt:frozenNode")) {
		  String uuid = node.getProperty("jcr:frozenUuid").getString();
		  Node originalNode = node.getSession().getNodeByUUID(uuid);
		  path.append(originalNode.getPath());      
		  nodeURL.setQueryParameterValue("version", node.getParent().getName());
		} else {
		  path.append(node.getPath());
		}
		
		nodeURL.setQueryParameterValue(detailParameterName, path.toString());
		nodeURL.setSchemeUse(true);
		FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
		String link = friendlyService.getFriendlyUri(nodeURL.toString());
		
		AddOnService.updateAddonDetailUrlToCache(this.getNodeId(), link);
		return link;
	}

	public static class EditActionListener extends EventListener<UIAddOnSearchOne> {

		@Override
		public void execute(Event<UIAddOnSearchOne> event) throws Exception {

			UIAddOnSearchOne uiAddOnSearchOne = event.getSource();
			UIAddOnSearchPageLayout uiAddOnSearchPageLayout = uiAddOnSearchOne.getAncestorOfType(UIAddOnSearchPageLayout.class);
			UIPopupContainer uiPopupContainer = uiAddOnSearchPageLayout.getChild(UIPopupContainer.class);
			UIAddOnSearchEdit uiAddOnSearchEdit = uiPopupContainer.createUIComponent(UIAddOnSearchEdit.class, null, null);
			uiAddOnSearchEdit.setNodeId(uiAddOnSearchOne.getNodeId());
			uiAddOnSearchEdit.reset();
			uiPopupContainer.activate(uiAddOnSearchEdit, 600, 670);
			uiPopupContainer.setRendered(true);
			event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
		}
		
	}
	public static class DetailActionListener extends EventListener<UIAddOnSearchOne> {

		@Override
		public void execute(Event<UIAddOnSearchOne> event) throws Exception {

			UIAddOnSearchOne uiAddOnSearchOne = event.getSource();
			UIAddOnSearchPageLayout uiAddOnSearchPageLayout = uiAddOnSearchOne.getAncestorOfType(UIAddOnSearchPageLayout.class);
			UIAddOnSearchForm.filterSelected = "default";
			UIAddOnDetail uiAddOnDetail =  uiAddOnSearchPageLayout.getChildById(UIAddOnSearchPageLayout.ADDON_DETAIL);
			uiAddOnDetail.setNodeId(uiAddOnSearchOne.getNodeId());
			uiAddOnSearchPageLayout.manageView(UIAddOnSearchPageLayout.ADDON_DETAIL);
			PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
			portletRequestContext.addUIComponentToUpdateByAjax(uiAddOnSearchPageLayout);			
		}
		
	}


	

}
