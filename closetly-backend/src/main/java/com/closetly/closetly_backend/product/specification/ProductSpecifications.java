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
            } else if (type.equalsIgnoreCase("buy")) {
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
        if (brand == null || brand.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.toLowerCase().trim());
    }

    public static Specification<Product> hasCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase().trim());
    }

    public static Specification<Product> hasSize(String size) {
        // ✅ VALIDATION: Ignore numeric values (like page size 20)
        if (size == null || size.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        // Check if size is numeric - if so, ignore it
        try {
            Integer.parseInt(size.trim());
            // If we get here, it's a number - ignore this filter
            return (root, query, cb) -> cb.conjunction();
        } catch (NumberFormatException e) {
            // Not a number, proceed with filter
        }

        return (root, query, cb) -> cb.equal(cb.lower(root.get("size")), size.toLowerCase().trim());
    }

    public static Specification<Product> hasCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("productCondition")), condition.toLowerCase().trim());
    }

    public static Specification<Product> hasPriceBetween(Double minPrice, Double maxPrice, String type) {

        if (minPrice == null && maxPrice == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {

            Path<Double> salePrice = root.get("salePrice");
            Path<Double> rentPrice = root.get("rentPrice");

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
                        cb.between(rentPrice, minPrice, maxPrice));
            }

            if (minPrice != null) {
                return cb.or(
                        cb.greaterThanOrEqualTo(salePrice, minPrice),
                        cb.greaterThanOrEqualTo(rentPrice, minPrice));
            }

            return cb.or(
                    cb.lessThanOrEqualTo(salePrice, maxPrice),
                    cb.lessThanOrEqualTo(rentPrice, maxPrice));
        };
    }

    public static Specification<Product> isForSale() {
        return (root, query, cb) -> cb.isTrue(root.get("forSale"));
    }

    public static Specification<Product> hasQuantityGreaterThan(int quantity) {
        return (root, query, cb) -> cb.greaterThan(root.get("quantity"), quantity);
    }

    public static Specification<Product> withinRadius(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null || radiusKm == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            // Haversine formula for distance calculation in SQL
            // distance = 6371 * acos(cos(radians(lat)) * cos(radians(p.latitude)) *
            // cos(radians(p.longitude) - radians(lng)) + sin(radians(lat)) *
            // sin(radians(p.latitude)))
            final int EARTH_RADIUS_KM = 6371;

            // Build the distance calculation expression
            var lat1Radians = cb.function("radians", Double.class, cb.literal(latitude));
            var lat2Radians = cb.function("radians", Double.class, root.get("latitude"));
            var lng1Radians = cb.function("radians", Double.class, cb.literal(longitude));
            var lng2Radians = cb.function("radians", Double.class, root.get("longitude"));

            var cosLat1 = cb.function("cos", Double.class, lat1Radians);
            var cosLat2 = cb.function("cos", Double.class, lat2Radians);
            var sinLat1 = cb.function("sin", Double.class, lat1Radians);
            var sinLat2 = cb.function("sin", Double.class, lat2Radians);

            // ✅ FIXED: Correct Haversine formula - use cos(dLng) not sin(dLng)
            var dLng = cb.diff(lng2Radians, lng1Radians);
            var cosDLng = cb.function("cos", Double.class, dLng);

            // ✅ BONUS: Clamp acos input to avoid NaN
            var acos_arg = cb.sum(
                    cb.prod(cb.prod(cosLat1, cosLat2), cosDLng),
                    cb.prod(sinLat1, sinLat2));

            var acosSafe = cb.function("least", Double.class,
                    cb.literal(1.0),
                    cb.function("greatest", Double.class, cb.literal(-1.0), acos_arg));

            var distance = cb.prod(
                    cb.literal(EARTH_RADIUS_KM),
                    cb.function("acos", Double.class, acosSafe));

            return cb.le(distance, radiusKm);
        };
    }
}
