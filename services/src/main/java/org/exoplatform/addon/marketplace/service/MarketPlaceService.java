package org.exoplatform.addon.marketplace.service;

import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.exception.MarketPlaceException;
import org.exoplatform.commons.utils.ListAccess;

import java.util.List;

/**
 * Created by kmenzli on 11/11/2016.
 */
public interface MarketPlaceService {
    /**
     * Create a category with given <code>category</code> model object.
     *
     * @param category
     * @return
     */
    Category createCategory(Category category);

    /**
     * Create a sub-category with given <code>category</code> model object and parent category ID.
     *
     * @param category the category metadata to create.
     * @param parentId parent Category ID
     * @return
     * @throws MarketPlaceException the category associated with <code>parentId</code> doesn't exist.
     */
    Category createCategory(Category category, long parentId) throws MarketPlaceException;

    /**
     * Update the category.
     *
     * It should throws MarketPlaceException if the category has been removed OR not existed from database.
     *
     * @param category
     * @return
     */
    Category updateCategory(Category category);

    /**
     * Remove the category with given <code>categoryId</code>,
     * and also its descendants if <code>deleteChild</code> is true.
     *
     * @param categoryId
     * @param deleteChild
     * @throws MarketPlaceException
     */
    void removeCategory(long categoryId, boolean deleteChild) throws MarketPlaceException;

    /**
     * Return the category with given <code>categoryId</code>.
     *
     * @param categoryId
     * @return
     * @throws MarketPlaceException
     */
    Category getCategory(Long categoryId) throws MarketPlaceException;

    /**
     * Return a list of children of a parent category with given <code>parentId</code>.
     *
     * @param parentId
     * @return
     */
    ListAccess<Category> getSubCategory(long parentId);

    ListAccess<Category> findCategories(String query);

    /**
     * Return the total numbre of registered categories
     * @return
     */
    long count () ;

    /**
     * Return all categories registered in the DB
     * @return
     */
    List<Category> findAllCategories () throws MarketPlaceException;
}
