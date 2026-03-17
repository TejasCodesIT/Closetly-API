package com.closetly.closetly_backend.product.specification;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
 import jakarta.persistence.criteria.Path;

public final class ProductSpecifications {

    private ProductSpecifications() {
        // utility class
    }

    public static Specification<Product> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }
    public static Specification<Product> hasType(String type) {

    if (!StringUtils.hasText(type)) {
        return (root, query, cb) -> cb.conjunction();
    }

    return (root, query, cb) -> {
        if (type.equalsIgnoreCase("rent")) {
            return cb.isTrue(root.get("isForRent"));
        } 
        else if (type.equalsIgnoreCase("buy")) {
            return cb.isTrue(root.get("isForSale"));
        }
        return cb.conjunction();
    };
}

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> hasKeyword(String keyword) {
        String likePattern = "%" + keyword.toLowerCase().trim() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern));
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.toLowerCase().trim());
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase().trim());
    }

    public static Specification<Product> hasSize(String size) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("size")), size.toLowerCase().trim());
    }

    public static Specification<Product> hasCondition(String condition) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("productCondition")), condition.toLowerCase().trim());
    }

   

public static Specification<Product> hasPriceBetween(Double minPrice, Double maxPrice, String type) {

    if (minPrice == null && maxPrice == null) {
        return (root, query, cb) -> cb.conjunction();
    }

    return (root, query, cb) -> {

        Path<Double> salePrice = root.get("salePrice");
        Path<Double> rentPrice = root.get("rentPricePerDay");

        // ✅ RENT
        if ("rent".equalsIgnoreCase(type)) {

            if (minPrice != null && maxPrice != null) {
                return cb.between(rentPrice, minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(rentPrice, minPrice);
            }
            return cb.lessThanOrEqualTo(rentPrice, maxPrice);
        }

        // ✅ BUY
        if ("buy".equalsIgnoreCase(type)) {

            if (minPrice != null && maxPrice != null) {
                return cb.between(salePrice, minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(salePrice, minPrice);
            }
            return cb.lessThanOrEqualTo(salePrice, maxPrice);
        }

        // ✅ BOTH
        if (minPrice != null && maxPrice != null) {
            return cb.or(
                cb.between(salePrice, minPrice, maxPrice),
                cb.between(rentPrice, minPrice, maxPrice)
            );
        }

        if (minPrice != null) {
            return cb.or(
                cb.greaterThanOrEqualTo(salePrice, minPrice),
                cb.greaterThanOrEqualTo(rentPrice, minPrice)
            );
        }

        return cb.or(
            cb.lessThanOrEqualTo(salePrice, maxPrice),
            cb.lessThanOrEqualTo(rentPrice, maxPrice)
        );
    };
}

    

    
}
