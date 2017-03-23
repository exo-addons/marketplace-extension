package org.exoplatform.addon.marketplace.controller;

import juzu.*;
import juzu.impl.common.Tools;
import juzu.plugin.jackson.Jackson;
import juzu.request.SecurityContext;
import org.exoplatform.addon.marketplace.GenericController;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.exception.MarketPlaceException;
import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.addon.service.AddOnService;
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

        //--- Display categories page
        return master.ok().withCharset(Tools.UTF_8);
    }

    @Ajax
    @Resource(method = HttpMethod.POST)
    @MimeType.JSON
    @Jackson
    public Category saveCategory(@Jackson Category category) throws Exception {
        //--- Init Category with parameters sent from client layer
        //--- xeditable fwk send also the id based on order in the matrix, thus I need to set only usefull data such as «name» and «description»
       //--- Category holder
        Category createdCat = null;
        //--- Hold temp categName
        String oldCategoryName = null;

        try {
            //--- Get category from DB
            Category cat =  marketPlaceService.getCategory(category.getId());
            //--- If the category with the ID exists then do an update
            if (cat != null ) {
                LOG.info("Update the category {}",category.getName());
                oldCategoryName = cat.getName();
                cat.setName(category.getName());
                cat.setDescription(category.getDescription());
                createdCat = marketPlaceService.updateCategory(cat);
                //---Update addons categories impacted
                AddOnService.updateAddonsCategoriesInBulk(oldCategoryName,category.getName(),"update");

            }
            //--- Else create a new category
            else {
                LOG.info("Save a new category {}",category.getName());
                //--- Create a new category
                createdCat = new Category(category.getName(),category.getDescription());
                //--- Create a new category
                createdCat = marketPlaceService.createCategory(createdCat);

            }

        } catch (Exception e) {
            LOG.error("Exception raised when persisting category {}",category.getName(), e);
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
            //---Update addons categories impacted
            AddOnService.updateAddonsCategoriesInBulk(category.getName(),null,"drop");
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
