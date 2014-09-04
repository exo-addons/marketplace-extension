/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.service;

import java.net.URI;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wcm.connector.BaseConnector;

/**
 * Created by The eXo Platform SAS
 * 20 Aug 2014  
 */
@Path("/addonservice/")
@RolesAllowed("users")
public class AddOnRestService extends BaseConnector implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(AddOnRestService.class.getName());
  
  private static final CacheControl cacheControl;
  static {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }
  
  CommentsService commentsService_;
  IdentityManager identityManager_;
  OrganizationIdentityProvider organizationIdentityProvider_;
  OrganizationService organizationService_;
  UserACL userACL_;
  AddOnService addOnService_;
  
  public AddOnRestService(UserACL userACL){
    organizationService_ = (OrganizationService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    commentsService_ = (CommentsService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(CommentsService.class);
    identityManager_ = (IdentityManager)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    organizationIdentityProvider_ = (OrganizationIdentityProvider)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationIdentityProvider.class);
    userACL_= userACL;
    addOnService_ = (AddOnService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(AddOnService.class);
  }
  
  
  @GET
  @Path("/migrate")
  @Produces("application/json")
  @RolesAllowed({"administrators"})
  public Response migrate(@Context SecurityContext sc,
                          @Context UriInfo uriInfo){
    try {
      Node homeNode =AddOnService.getNode("/sites/intranet/web contents/Contributions");
      NodeIterator iterator= homeNode.getNodes();
      while (iterator.hasNext()) {
        try {
          Node node = iterator.nextNode();
          LOG.info("Start migrate node: " + node.getPath()) ;
          //add mixin to allow comment and vote
          node.addMixin(AddOnService.MIX_COMMENTABLE_NODE_TYPE);
          node.addMixin(AddOnService.MIX_VOTEABLE_NODE_TYPE);
          node.save();
        } catch (Exception e) {
          LOG.warn(e);
        }
      }
      
      return Response.ok("Migrate successfully" , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      // TODO: handle exception
    }
    return Response.ok("Migrate failured" , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
  }
  @POST
  @Path("/edit-comment")
  @Produces("application/json")
  public Response editComment(@Context SecurityContext sc,
                              @Context UriInfo uriInfo,
                              @FormParam("jcrPath") String jcrPath, 
                              @FormParam("commentId") String commentId,
                              @FormParam("newComment") String newComment) throws Exception {
    
    
    String viewUsername = getUserId(sc, uriInfo);
    boolean viewerIsAdmin = checkInvokeUserPermission(viewUsername);
    
    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
    try {
      Node content = getContent(workspaceName, jcrPath, null, false);

      List<Node> comments = commentsService_.getComments(content, null);

      for (Node comment:comments) {
        String id = comment.getProperty("exo:name").getString();
        String commentor = comment.getProperty("exo:commentor").getString();
        
        if(commentId.equals(id) && (commentor.equals(viewUsername) || viewerIsAdmin)){
          commentsService_.updateComment(comment, newComment);
          return Response.ok(true , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
        }
        
      }

    }catch (Exception e){
        Response.serverError().build();
    }
    
    return Response.ok(false , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
  }
  
  @GET
  @Path("/delete-comment")
  @Produces("application/json")
  public Response deleteComment(@Context SecurityContext sc,
                              @Context UriInfo uriInfo, @QueryParam("jcrPath") String jcrPath, 
                              @QueryParam("commentId") String commentId) throws Exception {
    
    String viewUsername = getUserId(sc, uriInfo);
    boolean viewerIsAdmin = checkInvokeUserPermission(viewUsername);
    
    
    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
    try {
      Node content = getContent(workspaceName, jcrPath, null, false);

      List<Node> comments = commentsService_.getComments(content, null);

      for (Node comment:comments) {
        String id = comment.getProperty("exo:name").getString();
        String commentor = comment.getProperty("exo:commentor").getString();
        
        if(commentId.equals(id) && (commentor.equals(viewUsername) || viewerIsAdmin)){
          commentsService_.deleteComment(comment);
          return Response.ok(true , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
        }
        
      }

    }catch (Exception e){
        Response.serverError().build();
    }
    return Response.ok(false , MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
  }
  @GET
  @Path("/all-comment")
  @Produces("application/json")
  public Response getComments(@Context SecurityContext sc,
                              @Context UriInfo uriInfo, @QueryParam("jcrPath") String jcrPath) throws Exception {
    
    List<CommentMessage> listCommentMessages = new ArrayList<AddOnRestService.CommentMessage>();
    String viewUsername = getUserId(sc, uriInfo);
    boolean viewerIsAdmin = checkInvokeUserPermission(viewUsername);
    
    
    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);

    try {
      Node content = getContent(workspaceName, jcrPath, null, false);

      List<Node> comments = commentsService_.getComments(content, null);

      for (Node comment:comments) {
        
        CommentMessage commentMessage= new CommentMessage();
        if (comment.hasProperty("exo:name")) {
          commentMessage.setId(comment.getProperty("exo:name").getString());
        }
        if (comment.hasProperty("exo:commentContent")) {
          commentMessage.setCommentDetail(comment.getProperty("exo:commentContent").getString());
        }
        if (comment.hasProperty("exo:commentor")) {
          commentMessage.setCommentorUsername(comment.getProperty("exo:commentor").getString());
        }
        /*
        if (comment.hasProperty("exo:commentorFullName")) {
          commentMessage.setCommentorFullname(comment.getProperty("exo:commentorFullName").getString());
        }*/
        
        if (comment.hasProperty("exo:dateCreated")) {
          commentMessage.setCreateDate(DateFormat.getDateTimeInstance().format(comment.getProperty("exo:dateCreated").getDate().getTime()));
          commentMessage.setCommentCreatedDate(comment.getProperty("exo:dateCreated").getDate().getTime());
        }
        Profile userProfile = getSocialProfile(commentMessage.getCommentorUsername());
        commentMessage.setCommentorAvataUrl(userProfile.getAvatarUrl());
        commentMessage.setCommentorFullname(userProfile.getFullName(true));
        if(null!=viewUsername && (viewUsername.equals(commentMessage.getCommentorUsername()) || viewerIsAdmin)){
          commentMessage.setCanDelete(true);
        }else{
          commentMessage.setCanDelete(false);
        }
               
        listCommentMessages.add(commentMessage);
      }
      Collections.sort(listCommentMessages);
      
      DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
      return Response.ok(listCommentMessages, MediaType.APPLICATION_JSON)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .cacheControl(cacheControl)
                     .build();
    } catch (Exception e){
      Response.serverError().build();
    }
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).cacheControl(cacheControl).build();

  }
  
  Profile getSocialProfile(String username) {
    String avatar = "/social-resources/skin/images/ShareImages/UserAvtDefault.png";
    try {
      Identity userIdentity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username,true);
      Profile userProfile = userIdentity.getProfile();

      if(null == userProfile.getAvatarUrl() || userProfile.getAvatarUrl().length() ==0){
         userProfile.setAvatarUrl(avatar);
      }
      
      return userProfile;
    } catch (Exception e) {
      return null;
    }
  }
  
  private boolean checkInvokeUserPermission(String invokeUserId)
  {
    //return true if invokeUserId is root
    if(userACL_.getSuperUser().equalsIgnoreCase(invokeUserId))
      return true;

    try {
      Collection<Membership> membershipCollection = organizationService_.getMembershipHandler().findMembershipsByUserAndGroup(invokeUserId, "/platform/administrators");

      if(membershipCollection.isEmpty())
        return false;

      for (Membership membership : membershipCollection) {
        String membershipType = membership.getMembershipType();
        if (membershipType.equals("*") || membershipType.equals("manager"))
          return true;
      }

    } catch (Exception e) {
      LOG.error(e);
      return false;
    }

    return false;
  }


  private String getUserId(SecurityContext sc, UriInfo uriInfo) {

    try {
      return sc.getUserPrincipal().getName();
    } catch (NullPointerException e) {
      return getViewerId(uriInfo);
    } catch (Exception e) {
      return null;
    }
  }
  
  private String getViewerId(UriInfo uriInfo) {
    
    URI uri = uriInfo.getRequestUri();
    String requestString = uri.getQuery();
    if (requestString == null) {
      return null;
    }
    String[] queryParts = requestString.split("&");
    
    for (String queryPart : queryParts) {
      if (queryPart.startsWith("opensocial_viewer_id")) {
        return queryPart.substring(queryPart.indexOf("=") + 1, queryPart.length());
      }
    }
    
    return null;
  }
  
  
  
  @Override
  protected Node getRootContentStorage(Node parentNode) throws Exception {
    try {
      PortalFolderSchemaHandler folderSchemaHandler =
        webSchemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
      return folderSchemaHandler.getDocumentStorage(parentNode);
    } catch (Exception e) {
      WebContentSchemaHandler webContentSchemaHandler =
        webSchemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
      return webContentSchemaHandler.getDocumentFolder(parentNode);
    }
  }

  @Override
  protected String getContentStorageType() throws Exception {
    return FCKUtils.DOCUMENT_TYPE;
  }
  
  
  public class CommentMessage implements Comparable<CommentMessage>{
    private String Id;
    private String commentorFullname;
    private String commentorUsername;
    private String commentorAvataUrl;
    private String commentDetail;
    private String createDate;
    private  Date commentCreatedDate;
    private boolean canDelete;

    
    public Date getCommentCreatedDate() {
      return commentCreatedDate;
    }
    public void setCommentCreatedDate(Date commentCreatedDate) {
      this.commentCreatedDate = commentCreatedDate;
    }
    public boolean isCanDelete() {
      return canDelete;
    }
    public void setCanDelete(boolean canDelete) {
      this.canDelete = canDelete;
    }
    public String getId() {
      return Id;
    }
    public void setId(String id) {
      Id = id;
    }
    public String getCommentorFullname() {
      return commentorFullname;
    }
    public void setCommentorFullname(String commentorFullname) {
      this.commentorFullname = commentorFullname;
    }
    public String getCommentorUsername() {
      return commentorUsername;
    }
    public void setCommentorUsername(String commentorUsername) {
      this.commentorUsername = commentorUsername;
    }
    public String getCommentorAvataUrl() {
      return commentorAvataUrl;
    }
    public void setCommentorAvataUrl(String commentorAvataUrl) {
      this.commentorAvataUrl = commentorAvataUrl;
    }
    public String getCommentDetail() {
      return commentDetail;
    }
    public void setCommentDetail(String commentDetail) {
      this.commentDetail = commentDetail;
    }
    public String getCreateDate() {
      return createDate;
    }
    public void setCreateDate(String createDate) {
      this.createDate = createDate;
    }
    @Override
    public int compareTo(CommentMessage commentMessage) {
      return this.commentCreatedDate.compareTo(commentMessage.getCommentCreatedDate());
    }
    
  }

}
