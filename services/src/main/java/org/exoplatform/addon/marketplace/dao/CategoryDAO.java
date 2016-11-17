package org.exoplatform.addon.marketplace.dao;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.addon.marketplace.bo.Category;
/**
 * Created by kmenzli on 12/11/2016.
 */
public interface CategoryDAO extends GenericDAO<Category, Long> {

    Category removeCategory(long categoryId, boolean deleteChild);

    Category getCategoryByName (String name);

    ListAccess<Category> findSubCategory(Category category);

    ListAccess<Category> findCategories(String query);
}
