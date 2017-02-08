package org.exoplatform.addon.marketplace.controller;

import juzu.*;
import juzu.impl.common.Tools;
import juzu.plugin.jackson.Jackson;
import juzu.request.SecurityContext;
import org.exoplatform.addon.marketplace.GenericController;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.exception.MarketPlaceException;
import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by kmenzli on 12/11/2016.
 */
@SessionScoped
public class CategoryManagement extends GenericController {

    private static final Log LOG = ExoLogger.getExoLogger(CategoryManagement.class);

    @Inject
    MarketPlaceService marketPlaceService;

    @Inject
    @Path("master.gtmpl")
    org.exoplatform.addon.marketplace.templates.master master;
    @Inject
    @Path("messageDialog.gtmpl")
    org.exoplatform.addon.marketplace.templates.messageDialog messageDialog;


    @View
    public Response index(SecurityContext securityContext) throws MarketPlaceException {

        //--- check the else add a default category to hold all unclassifed addons
        try {
            if(marketPlaceService.count() == 0) {

                Category defaultCat = new Category("Default","Default category to hold eXo addons");

                marketPlaceService.createCategory(defaultCat);

            }

        } catch (Exception e) {

        }

        return master.ok().withCharset(Tools.UTF_8);
    }

    @Ajax
    @Resource(method = HttpMethod.POST)
    @MimeType.JSON
    @Jackson
    public Category saveCategory(@Jackson Category category) throws Exception {
        //--- Init Category with parameters sent from client layer
        //--- xeditable fwk send also the id based on order in the matrix, thus I need to set only usefull data such as «name» and «description»
        Category createdCat = new Category(category.getName(),category.getDescription());
        if (LOG.isInfoEnabled()) {
            LOG.info("Save new category with [name = "+category.getName()+"]");
        }

        try {
            createdCat = marketPlaceService.createCategory(createdCat);
        } catch (Exception ex) {
            LOG.error("Exception raised when storing category ["+category.getName()+"]", ex);
        }
        return createdCat;
    }

    @Ajax
    @Resource(method = HttpMethod.POST)
    @MimeType.JSON
    @Jackson
    public void deleteCategory(@Jackson Category category) throws Exception {
        if (LOG.isInfoEnabled()) {
            LOG.info("Delete the category with [name = "+category.getName()+"]");
        }
        try {
            marketPlaceService.removeCategory(category.getId(),false);
        } catch (Exception ex) {
            LOG.error("Exception raised when storing category ["+category.getName()+"]", ex);
        }

    }


    @Override
    public Log getLogger() {
        return LOG;
    }

    @Resource
    @Ajax
    @MimeType.JSON
    @Jackson
    public List<Category> getCategories() throws Exception {
        List<Category> categories = new ArrayList<Category>();
        categories.addAll(marketPlaceService.findAllCategories());
        return categories;
    }

    @Ajax
    @juzu.Resource
    @MimeType.JSON
    @Jackson
    public Response getBundle(String locale) {
        return super.getBundle(new Locale(locale));
    }


}
