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
import org.exoplatform.addon.service.AddOnService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;

@ComponentConfig(lifecycle = Lifecycle.class, template = "app:/templates/AddOnSearchPortlet/UIAddOnSearchResult.gtmpl", events = { @EventConfig(listeners = UIAddOnSearchResult.ShowMoreActionListener.class)

})
public class UIAddOnSearchResult extends UIContainer {

  private static final Log    log            = ExoLogger.getLogger(UIAddOnSearchResult.class);

  private ArrayList<Node>     data           = new ArrayList<Node>();

  /** The Constant ITEMS_PER_PAGE. */
  public final static Integer ITEMS_PER_PAGE = 9;

  public static Boolean       REFRESH        = true;

  private int                 showMoreCount  = 0;

  private String              sqlOrder;

  private int                 queryStart     = 0;

  private String              sqlCondition;

  private int                 totaItem       = 0;

  private String              keyword        = "";
  
  private Boolean canEdit = false;
  
  private String addonHomePath; 
  private int totalAddon = 0;

  //private final static String ADDONS_FOLDER  = "/Contributions/";
  
  public void processRender(WebuiRequestContext context) throws Exception {

    if (REFRESH){
      init();
      SortAddons("popular", Constants.CATEGORY_ITEM_ALL_VALUE);
    }
    //--- Get the paramter from url, this parameter is used to fetch categories by name
    HttpServletRequest request = Util.getPortalRequestContext().getRequest();
    if (request.getParameter(Constants.HTTP_PARAMETER_CATEGORY_NAME) != null) {
      log.debug("MarketPlace Addon, Load addons based on parameter passed through URL {} ", request.getParameter("category"));
      SortAddons("popular", request.getParameter("category"));
    }
    super.processRender(context);
  }

  public void setSQLOrder(String order) {
    this.sqlOrder = order;
  }

  public String getSQLOrder() {
    return this.sqlOrder;
  }

  public void setSQLCondition(String cond) {
    this.sqlCondition = cond;
  }

  public String getSQLCondition() {
    return this.sqlCondition;
  }

  public void setKeyword(String keyword) {
    keyword = keyword.trim();
    this.keyword = keyword;
    String cond = " AND ( (exo:title LIKE '%" + keyword + "%') OR ( exo:description LIKE '%" + keyword
        + "%' )  OR (exo:author LIKE '%" + keyword + "%') ) ";
    if (keyword != null && !("".equals(keyword))) {

      cond = " AND ( contains(exo:title,'" + keyword + "') OR (exo:title LIKE '%" + keyword
          + "%') OR (exo:name LIKE '%" + keyword + "%') OR  contains( exo:description,'" + keyword
          + "' ) OR (exo:description LIKE '%" + keyword + "%') OR contains(exo:author,'" + keyword
          + "') OR (exo:author LIKE '%" + keyword + "%')  ) ";
    }
    this.setSQLCondition(cond);

  }

  public String getKeyword() {
    return this.keyword;
  }

  public void setQueryStart(int st) {
    this.queryStart = st;
  }

  public int getQueryStart() {
    return this.queryStart;
  }

  public void setShowMoreCount(int count) {
    this.showMoreCount = count;
  }

  public int getShowMoreCount() {
    return this.showMoreCount;
  }

  public void setTotalItem(int total) {
    this.totaItem = total;
  }

  public int getTotalItem() {
    return (int) this.totaItem;
  }

  
  public Boolean getCanEdit() {
    return canEdit;
  }

  public void setCanEdit(Boolean canEdit) {
    this.canEdit = canEdit;
  }
  
  private boolean checkLoginUserPermission()
  {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortalRequestContext prc = (PortalRequestContext) context.getParentAppRequestContext();
    UIPortalApplication portalApp = (UIPortalApplication) prc.getUIApplication();
    UIPortal portal = portalApp.getCurrentSite();
    UserACL userACL = portal.getApplicationComponent(UserACL.class);
    
    String loginUserId = prc.getRemoteUser();
    if(null == loginUserId || loginUserId.length()==0)
      return false;
    //return true if invokeUserId is root
    if(userACL.getSuperUser().equalsIgnoreCase(loginUserId))
      return true;

    try {
      OrganizationService organizationService = (OrganizationService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
      Collection<Membership> membershipCollection = organizationService.getMembershipHandler().findMembershipsByUserAndGroup(loginUserId, "/platform/administrators");

      if(membershipCollection.isEmpty())
        return false;

      for (Membership membership : membershipCollection) {
        String membershipType = membership.getMembershipType();
        if (membershipType.equals("*") || membershipType.equals("manager"))
          return true;
      }

    } catch (Exception e) {
      log.warn(e);
      return false;
    }

    return false;
  }

  public void init() throws Exception {
    addonHomePath = AddOnService.getAddOnHomePath();
    setCanEdit(checkLoginUserPermission());
    clearResult();
    this.setKeyword("");
    this.searchDBAddonByKey();
    this.getTotalDBResources();
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    portletRequestContext.addUIComponentToUpdateByAjax(this);
  }

  public void clearResult() throws RepositoryException {
    this.resetChild();
    this.setData(new ArrayList<Node>());
    this.setShowMoreCount(0);
    this.setQueryStart(0);
    this.setSQLOrder(" ORDER BY exo:title ASC ");

  }

  private void resetChild() throws RepositoryException {
    if (this.getData().size() > 0) {
      for (Node aNode : this.getData()) {
        //this.removeChildById(aNode.getUUID());
        this.removeChild(UIAddOnSearchOne.class);
      }
    }

  }

  public void setData(ArrayList<Node> data) {

    this.data = data;

  }

  public ArrayList<Node> getData() {

    return this.data;
  }

  public void doSearch() throws Exception {

    this.searchDBAddonByKey();
    this.getTotalDBResources();

  }
  
  public void showMyAddons(){
    UIAddOnSearchResult.REFRESH = false;
    try {
      this.clearResult();
      String sqlCond = this.getSQLCondition();
      String userID = Util.getPortalRequestContext().getRemoteUser();
      sqlCond +=" AND exo:owner = '"+userID+"' ";
      this.setSQLCondition(sqlCond);
      this.doSearch();
    } catch (RepositoryException e) {
      log.error("ERR show my addons");
    }
    catch (Exception e) {
      log.error("ERR show my addons");
    }
  }

  /**
   * Return addons based on category name selected by end users
   * @param categoryName
   */
  public void showAddonsByCategory (String categoryName) {
      UIAddOnSearchResult.REFRESH = false;
      StringBuffer sqlQuery = null;
      try {
          this.clearResult();
          sqlQuery = new StringBuffer("SELECT * FROM exo:addon WHERE jcr:path like '" +
                                        addonHomePath +
                                        "%' AND NOT jcr:path LIKE '" + addonHomePath + "%/%'" +
                                        " AND publication:currentState='published' AND NOT (jcr:mixinTypes = 'exo:restoreLocation') ");
          if (!categoryName.equalsIgnoreCase(Constants.CATEGORY_ITEM_ALL_VALUE)) {
            sqlQuery.append( " AND mix:mpkaceAddonCatName = '"+categoryName+"' " );

          }

          QueryResult result = this.excSQL(sqlQuery.toString(), true);
          NodeIterator it = result.getNodes();
          while (it.hasNext()) {
                Node findedNode = it.nextNode();

                if (super.getChildById(findedNode.getUUID()) == null) {
                  UIAddOnSearchOne uiAddOnSearchOne = addChild(UIAddOnSearchOne.class, null, findedNode.getUUID());
                  uiAddOnSearchOne.setNodeId(findedNode.getUUID());
                  uiAddOnSearchOne.setCanEdit(this.getCanEdit());
                }

                this.data.add(findedNode);
          }

      } catch (RepositoryException re) {
          log.error("Error to load addons within the category called "+categoryName);
      } catch (Exception e) {
          log.error("Error to load addons within the category called "+categoryName);
      }

  }




  public void SortAddons(String sort, String selectedCat){

    UIAddOnSearchResult.REFRESH = false;
    
    try {
      this.clearResult();
      if(sort.equals("za")){
        if (!selectedCat.equalsIgnoreCase(Constants.CATEGORY_ITEM_ALL_VALUE)) {
          this.setSQLOrder( " AND mix:mpkaceAddonCatName = '"+selectedCat+"' ORDER BY exo:title DESC " );

        } else {
          this.setSQLOrder(" ORDER BY exo:title DESC ");
        }

      }
      else if(sort.equals("az")){
        if (!selectedCat.equalsIgnoreCase(Constants.CATEGORY_ITEM_ALL_VALUE)) {
          this.setSQLOrder( " AND mix:mpkaceAddonCatName = '"+selectedCat+"' ORDER BY exo:title ASC " );

        } else {
          this.setSQLOrder(" ORDER BY exo:title ASC ");
        }


      }else if(sort.equals("latest")){

        if (!selectedCat.equalsIgnoreCase(Constants.CATEGORY_ITEM_ALL_VALUE)) {
          this.setSQLOrder( " AND mix:mpkaceAddonCatName = '"+selectedCat+"' ORDER BY exo:dateModified DESC " );

        } else {
          //Oder by latest created
          this.setSQLOrder(" ORDER BY exo:dateModified DESC ");
        }

      }
      else{
        if (!selectedCat.equalsIgnoreCase(Constants.CATEGORY_ITEM_ALL_VALUE)) {
          this.setSQLOrder( " AND mix:mpkaceAddonCatName = '"+selectedCat+"' ORDER BY exo:voteTotal DESC, exo:votingRate DESC " );

        } else {
          // oder by vote
          this.setSQLOrder(" ORDER BY exo:voteTotal DESC, exo:votingRate DESC ");
        }

      }
      this.doSearch();
    } catch (RepositoryException e) {
      log.error("ERR show my addons");
    }
    catch (Exception e) {
      log.error("ERR show my addons");
    }    
    
  }
  public void searchDBAddonByKey() throws Exception {

    String sqlQuery = "";
    // create query
    String sqlStatement = " SELECT * FROM exo:addon WHERE jcr:path like '" +
                          addonHomePath + 
                          "%' AND NOT jcr:path LIKE '" + addonHomePath + "%/%'" +
                          " AND publication:currentState='published' AND  NOT (jcr:mixinTypes = 'exo:restoreLocation') ";
    sqlQuery = sqlStatement + this.getSQLCondition() + this.getSQLOrder();
    this.getDBResource(sqlQuery);

  }

  private QueryResult excSQL(String sqlQuery, Boolean limit) throws Exception {

    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(WCMCoreUtils.getRepository()
                                                             .getConfiguration()
                                                             .getDefaultWorkspaceName(),
                                                 WCMCoreUtils.getRepository());

    // make SQL query
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    QueryImpl query = (QueryImpl) queryManager.createQuery(sqlQuery, Query.SQL);
    if (limit) {

      if (this.getQueryStart() != 0) {
        query.setOffset(getQueryStart());
        log.debug(" ================ start " + getQueryStart() + " ========================== ");
      }
      query.setLimit(UIAddOnSearchResult.ITEMS_PER_PAGE);

    }
    // execute query and fetch result
    QueryResult result = query.execute();
    return result;
      
  }

  private void getDBResource(String sqlQuery) throws Exception {

    QueryResult result = this.excSQL(sqlQuery, true);
    NodeIterator it = result.getNodes();
    while (it.hasNext()) {
      Node findedNode = it.nextNode();
      
      if (super.getChildById(findedNode.getUUID()) == null) {
        UIAddOnSearchOne uiAddOnSearchOne = addChild(UIAddOnSearchOne.class, null, findedNode.getUUID());
        uiAddOnSearchOne.setNodeId(findedNode.getUUID());
        uiAddOnSearchOne.setCanEdit(this.getCanEdit());
      }
      
      this.data.add(findedNode);
    }
  }

  public void getTotalDBResources() throws Exception {
    String sqlQuery = "SELECT exo:name FROM exo:addon WHERE jcr:path like '" + 
                       addonHomePath + 
                      "%' AND NOT jcr:path LIKE '" + addonHomePath + "%/%'" +
                      " AND publication:currentState='published' AND NOT (jcr:mixinTypes = 'exo:restoreLocation') ";
    
    sqlQuery += this.getSQLCondition();
    QueryResult result = this.excSQL(sqlQuery, false);
    int count = (int) result.getRows().getSize();
    log.debug(" ================ total item " + count + " ========================== ");
    this.setTotalItem(count);
  }

  public Boolean isShowMore() {

    if (this.getTotalItem() > this.getData().size())
      return true;

    return false;

  }

  public static class ShowMoreActionListener extends EventListener<UIAddOnSearchResult> {

    @Override
    public void execute(Event<UIAddOnSearchResult> event) throws Exception {
      UIAddOnSearchResult.REFRESH = false;
      UIAddOnSearchResult uiAddOnSearchResult = event.getSource();
      UIAddOnSearchPageLayout uiAddonsSearchPageContainer = (UIAddOnSearchPageLayout) uiAddOnSearchResult.getParent();
      if (uiAddOnSearchResult.isShowMore()) {

        int i = uiAddOnSearchResult.getShowMoreCount();
        i++;
        uiAddOnSearchResult.setShowMoreCount(i);
        uiAddOnSearchResult.setQueryStart(i * UIAddOnSearchResult.ITEMS_PER_PAGE);
        uiAddOnSearchResult.searchDBAddonByKey();
        PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
        portletRequestContext.addUIComponentToUpdateByAjax(uiAddOnSearchResult);

      }

    }

  }

}
