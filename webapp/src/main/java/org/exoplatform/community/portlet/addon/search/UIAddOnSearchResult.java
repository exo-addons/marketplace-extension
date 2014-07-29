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

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

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

  private final static String ADDONS_FOLDER  = "/Contributions/";
  
  public void processRender(WebuiRequestContext context) throws Exception {
    if (REFRESH)
      init();
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

  public void init() throws Exception {
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
      this.removeChild(UIAddOnSearchOne.class);
     /* for (Node aNode : this.getData()) {
        this.removeChildById(aNode.getUUID());
      }*/
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
  public void SortAddons(String sort){

    UIAddOnSearchResult.REFRESH = false;
    this.setSQLCondition("");
    try {
      this.clearResult();
      if(sort.equals("za")){
        
        this.setSQLOrder(" ORDER BY exo:title DESC ");
      }
      else{
        this.setSQLOrder(" ORDER BY exo:title ASC ");
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
    String sqlStatement = " SELECT * FROM exo:addon WHERE publication:currentState='published' AND  NOT (jcr:mixinTypes = 'exo:restoreLocation') AND jcr:path like '%"
        + ADDONS_FOLDER + "%' ";
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
      this.data.add(findedNode);
    }
    this.display();

  }

  private void display() throws RepositoryException, Exception {

    for (Node aNode : this.getData()) {
      if (super.getChildById(aNode.getUUID()) == null) {
        UIAddOnSearchOne uiAddOnSearchOne = addChild(UIAddOnSearchOne.class, null, aNode.getUUID());
        uiAddOnSearchOne.setNodeId(aNode.getUUID());
      }
    }
  }

  public void getTotalDBResources() throws Exception {

    String sqlQuery = "SELECT exo:name FROM exo:addon WHERE  publication:currentState='published' AND NOT (jcr:mixinTypes = 'exo:restoreLocation') AND jcr:path like '%"
        + ADDONS_FOLDER + "%' ";
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
