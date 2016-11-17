package org.exoplatform.addon.marketplace.bo;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Created by kmenzli on 11/11/2016.
 */
@Entity(name = "MPCategoryEntity")
@ExoEntity
@Table(name = "ADDON_MARKETPLACE_CATEGORY")
@NamedQueries({
        @NamedQuery(
                name = "category.findAllOrderBy",
                query = "SELECT c FROM MPCategoryEntity c ORDER BY c.id asc"
        ),
        @NamedQuery(
                name = "category.findCategoryByName",
                query = "SELECT c FROM MPCategoryEntity c WHERE c.name = :name "
        ),
        @NamedQuery(
                name = "category.count",
                query = "SELECT count(c.id) FROM MPCategoryEntity c")
})
public class Category implements Serializable {
    public static final String PREFIX_CLONE = "Copy of ";

    @Id
    @Column(name = "MPLACE_CAT_ID")
    private long      id;

    @Column(name = "MPLACE_CAT_NAME", unique=true)
    private String    name;

    @Column(name = "MPLACE_CAT_DESCRIPTION")
    private String    description;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_MPLACE_CAT_ID", nullable = true)
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private List<Category> children = new LinkedList<Category>();

    public Category() {
    }

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

   public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    @Deprecated
    public List<Category> getChildren() {
        return children;
    }

    @Deprecated
    public void setChildren(List<Category> children) {
        this.children = children;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (getId() ^ (getId() >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Category))
            return false;
        Category other = (Category) obj;
        if (getId() != other.getId())
            return false;
        return true;
    }

    public Category clone(boolean cloneCategory) {

        Category category = new Category(this.getName(), this.getDescription());
        category.setId(getId());
        if (this.getParent() != null) {
            category.setParent(getParent().clone(false));
        }
        category.children = new LinkedList<Category>();

        return category;
    }
}
