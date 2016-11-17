package org.exoplatform.addon.marketplace.controller;

import juzu.*;
import juzu.impl.common.Tools;
import juzu.request.SecurityContext;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.exception.MarketPlaceException;
import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ResourceBundle;

/**
 * Created by kmenzli on 12/11/2016.
 */
public class CategoryManagement {

    private static final Log LOG = ExoLogger.getExoLogger(CategoryManagement.class);

    public static enum MSG_TYPE {
        INFO, WARNING, ERROR
    }

    @Inject
    MarketPlaceService marketPlaceService;

    @Inject
    @Path("index.gtmpl")
    org.exoplatform.addon.marketplace.templates.index index;
    @Inject
    @Path("messageDialog.gtmpl")
    org.exoplatform.addon.marketplace.templates.messageDialog messageDialog;

    @Inject
    ResourceBundle bundle;


    @View
    public Response index(SecurityContext securityContext) throws MarketPlaceException {

        //--- check the else add a default category to hold all unclassifed addons
        try {
            if(marketPlaceService.count() == 0) {

                Category defaultCat = new Category("default","Default category to hold eXo addons");

                marketPlaceService.createCategory(defaultCat);

            }

        } catch (Exception e) {

        }
        String errorMessage = "";
        String depthOfCategories = "2";

        return index.with()
                .errorMessage(errorMessage)
                .depthOfCategories(depthOfCategories)
                .ok().withCharset(Tools.UTF_8);
    }

    @Resource
    @Ajax
    @MimeType.JSON
    public Response addCategory(String name, String description, SecurityContext securityContext) throws MarketPlaceException, JSONException {

        String currentUser = securityContext.getRemoteUser();
        if(currentUser == null) {
            return Response.status(401).body("You must login to create new category");
        }

        if(name == null || name.isEmpty()) {
            return Response.status(412).body("Name of category is required");
        }

        Category cat = new Category(name,description);

        Category catCtxPersistance = marketPlaceService.createCategory(cat);

        JSONObject result = new JSONObject();
        result.put("id", catCtxPersistance.getId());//Can throw JSONException (same for all #json.put methods below)
        result.put("name", catCtxPersistance.getName());
        result.put("description", catCtxPersistance.getDescription());

        return Response.ok(result.toString()).withCharset(Tools.UTF_8);
    }

    @Resource
    @Ajax
    @MimeType.HTML
    public Response openWarningDialog(String msg) {
        return buildMessage(msg, MSG_TYPE.WARNING);
    }

    public Response buildMessage(String message, MSG_TYPE msgType) {
        return messageDialog
                .with()
                .msg(message)
                .type(msgType)
                .ok().withCharset(Tools.UTF_8);
    }



}
