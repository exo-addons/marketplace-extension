package org.exoplatform.addon.marketplace.service.impl;
import org.exoplatform.addon.marketplace.dao.CategoryDAO;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.exception.MarketPlaceException;
import org.exoplatform.addon.marketplace.service.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by kmenzli on 12/11/2016.
 */
@Singleton
public class MarketPlaceServiceImpl implements MarketPlaceService {

    private static final Log LOG = ExoLogger.getExoLogger(MarketPlaceServiceImpl.class);

    @Inject
    CategoryDAO categoryDAO;

    public MarketPlaceServiceImpl() {
    }

    public MarketPlaceServiceImpl(CategoryDAO daoDAO) {

        this.categoryDAO = daoDAO;
    }

    @Override
    @ExoTransactional
    public Category createCategory(Category category) {
        Category categ = categoryDAO.create(category);
        return categ;
    }

    @Override
    @ExoTransactional
    public Category createCategory(Category category, long parentId) throws MarketPlaceException {
        Category parentCategory = categoryDAO.find(parentId);
        if (parentCategory != null) {
            category.setParent(parentCategory);

            //persist project
            category = createCategory(category);

            return category;
        } else {
            LOG.info("Can not find category for parent with ID: " + parentId);
            throw new MarketPlaceException(parentId, Category.class);
        }
    }

    @Override
    @ExoTransactional
    public Category updateCategory(Category category) {
        Category categ = categoryDAO.update(category);
        return categ;
    }

    @Override
    @ExoTransactional
    public void removeCategory(long id, boolean deleteChild) throws MarketPlaceException {
        Category project = getCategory(id);
        if (project == null) throw new MarketPlaceException(id, Category.class);
        categoryDAO.removeCategory(id, deleteChild);
    }

    @Override
    public Category getCategory(Long id) throws MarketPlaceException {

        Category project = categoryDAO.find(id);
        if (project == null) throw new MarketPlaceException(id, Category.class);

        return project;

    }

    @Override
    public ListAccess<Category> getSubCategory(long parentId) {
        try {
            Category parent = getCategory(parentId);
            return categoryDAO.findSubCategory(parent);
        } catch (MarketPlaceException ex) {
            return new ListAccess<Category>() {
                @Override
                public int getSize() throws Exception {
                    return 0;
                }

                @Override
                public Category[] load(int arg0, int arg1) throws Exception, IllegalArgumentException {
                    return new Category[0];
                }
            };
        }
    }

    @Override
    public ListAccess<Category> findCategories(String query) {
        return categoryDAO.findCategories(query);
    }

    @Override
    public long count() {
        return categoryDAO.count();
    }
}
