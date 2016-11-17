package org.exoplatform.addon.marketplace.exception;

/**
 * Created by kmenzli on 12/11/2016.
 */
public class MarketPlaceException extends Exception {
    private Long entityId;

    private Class<?> entityType;

    public MarketPlaceException(Long entityId, Class<?> entityType) {
        this.entityId = entityId;
        this.entityType = entityType;
    }

    @Override
    public String getMessage() {
        if (entityId != null && entityType != null) {
            return "Exception on " + entityType + " with ID: "+entityId;
        }
        return super.getMessage();
    }

}
