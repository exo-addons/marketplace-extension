package org.exoplatform.addon.marketplace.dao.hibernate;

import org.exoplatform.addon.marketplace.bo.Category;
import org.exoplatform.addon.marketplace.dao.CategoryDAO;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.commons.utils.ListAccess;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;

/**
 * Created by kmenzli on 12/11/2016.
 */
public class CategoryDAOImpl extends GenericDAOJPAImpl<Category, Long>  implements CategoryDAO {
    private static final Logger LOG = Logger.getLogger("ProjectDAOImpl");

    public CategoryDAOImpl() {
    }

    @Override
    public Category update(Category entity) {
        return super.update(entity);
    }

    @Override
    public void delete(Category entity) {
        Category p = getEntityManager().find(Category.class, entity.getId());
        if (p != null) {
            super.delete(p);
        }
    }

    @Override
    public Category removeCategory(long categoryId, boolean deleteChild) {
        Category c = getEntityManager().find(Category.class, categoryId);
        if (c == null) {
            return null;
        }
        if (!deleteChild && c.getChildren() != null) {
            for(Category pj : c.getChildren()) {
                pj.setParent(c.getParent());
                getEntityManager().persist(pj);
            }
            c.getChildren().clear();
        }

        super.delete(c);
        return c;
    }

    @Override
    public Category findCategoryByName(String name) {
        TypedQuery<Category> query = getEntityManager().createNamedQuery("category.findCategoryByName", Category.class);
        query.setParameter("name", name);

        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public ListAccess<Category> findSubCategory(Category category) {
        //TODO : not yet implemented
        return null;
    }

    @Override
    public ListAccess<Category> findCategories(String query) {
        //TODO : not yet implemented
        return null;
    }
}
