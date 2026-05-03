package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import com.closetly.closetly_backend.product.entity.ProductImage;
import com.closetly.closetly_backend.product.entity.ProductType;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.product.service.ImageUploadService;
import com.closetly.closetly_backend.product.specification.ProductSpecifications;
import com.closetly.closetly_backend.user.entity.Role;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;

    @Override
    public ProductDTO createProduct(ProductRequestDTO request, String email) {
        log.info("[Product Creation] Starting product creation for user: {}", email);
        log.debug("[Product Creation] Incoming request - title: {}, category: {}, brand: {}",
                request.getTitle(), request.getCategory(), request.getBrand());

        // ✅ Log the DTO values immediately after deserialization
        log.info("[Product Creation] DTO DESERIALIZED - isForSale: {}, isForRent: {}",
                request.isForSale(), request.isForRent());
        log.info("[Product Creation] DTO DESERIALIZED - productType: {}, salePrice: {}, rentPrice: {}",
                request.getProductType(), request.getSalePrice(), request.getRentPrice());
        log.info("[Product Creation] DTO DESERIALIZED - description: '{}' (length: {})",
                request.getDescription(),
                request.getDescription() != null ? request.getDescription().length() : 0);

        request.validate();
        log.info("[Product Creation] Validation passed");

        // verify authenticated user matches email and has USER role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(email)) {
            log.error("[Product Creation] Authentication failed for email: {}", email);
            throw new AccessDeniedException("Not authenticated as the given seller");
        }

        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        boolean isUserRole = seller.getRoles().stream()
                .map(Role::getName)
                .anyMatch(r -> r == Role.RoleName.USER);
        if (!isUserRole) {
            log.error("[Product Creation] User {} does not have USER role", email);
            throw new AccessDeniedException("Only users with USER role can create products");
        }

        log.debug("[Product Creation] Request details - productType: {}, isForSale: {}, isForRent: {}",
                request.getProductType(), request.isForSale(), request.isForRent());
        log.debug("[Product Creation] Request pricing - salePrice: {}, rentPrice: {}",
                request.getSalePrice(), request.getRentPrice());
        log.debug("[Product Creation] Request images count: {}",
                request.getImages() != null ? request.getImages().size() : 0);

        // Debug: Log the values being passed to resolveProductType
        log.info("[Product Creation] INPUT TO resolveProductType - productType: '{}', isForRent: {}, isForSale: {}",
                request.getProductType(), request.isForRent(), request.isForSale());

        ProductType productType = resolveProductType(request.getProductType(), request.isForRent(),
                request.isForSale());

        log.info("[Product Creation] OUTPUT FROM resolveProductType - productType: {}",
                productType);

        // ✅ FIX: Use the boolean flags directly from the request instead of calculating
        // from productType
        // This ensures the flags are saved exactly as selected in the UI
        boolean forSale = request.isForSale();
        boolean forRent = request.isForRent();

        log.info("[Product Creation] BOOLEAN FLAGS FROM REQUEST - forSale: {}, forRent: {}",
                forSale, forRent);

        // Ensure productType is consistent with the flags
        ProductType resolvedProductType = resolveProductType(request.getProductType(), forRent, forSale);
        Double buyPrice = firstNonNull(request.getBuyPrice(), request.getSalePrice());

        log.info("[Product Creation] RESOLVED productType: {} (from explicit: {}, flags: isForRent={}, isForSale={})",
                resolvedProductType, request.getProductType(), forRent, forSale);

        Product product = Product.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .brand(request.getBrand())
                .category(request.getCategory())
                .size(request.getSize())
                .productCondition(request.getCondition())
                .productType(resolvedProductType)
                .salePrice(firstNonNull(request.getSalePrice(), buyPrice))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(request.getCity())
                .address(request.getAddress())
                .state(request.getState() != null ? request.getState() : "Maharashtra")
                .rentPrice(request.getRentPrice())
                .buyPrice(buyPrice)
                .popularity(0)
                .forSale(forSale)
                .forRent(forRent)
                .quantity(request.getQuantity())
                .images(convertImageUrlsToProductImages(request.getImages()))
                .seller(seller)
                .status(Product.ProductStatus.ACTIVE)
                .build();

        // Debug: Log entity values immediately after building
        log.info("[Product Creation] ENTITY BUILT - productType: {}, forSale: {}, forRent: {}",
                product.getProductType(), product.isForSale(), product.isForRent());
        log.info("[Product Creation] ENTITY BUILT - salePrice: {}, rentPrice: {}",
                product.getSalePrice(), product.getRentPrice());

        log.debug("[Product Creation] Entity before save - title: {}, description: '{}' (length: {})",
                product.getTitle(),
                product.getDescription(),
                product.getDescription() != null ? product.getDescription().length() : 0);

        Product saved = productRepository.save(product);

        // Debug: Log entity values immediately after saving
        log.info("[Product Creation] ENTITY SAVED - id: {}, productType: {}, forSale: {}, forRent: {}",
                saved.getId(), saved.getProductType(), saved.isForSale(), saved.isForRent());

        log.info("[Product Creation] Product saved successfully - id: {}, title: {}",
                saved.getId(), saved.getTitle());
        log.info("[Product Creation] Saved entity description: '{}' (length: {})",
                saved.getDescription(),
                saved.getDescription() != null ? saved.getDescription().length() : 0);

        return toDto(saved);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductRequestDTO request) {
        log.info("[Product Update] Starting product update for product id: {}", id);

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User authUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (!existing.getSeller().getId().equals(authUser.getId())) {
            throw new AccessDeniedException("Only the seller can update this product");
        }

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setBrand(request.getBrand());
        existing.setCategory(request.getCategory());
        existing.setSize(request.getSize());
        existing.setProductCondition(request.getCondition());

        // ✅ FIX: Use the boolean flags directly from the request instead of calculating
        // from productType
        boolean forSale = request.isForSale();
        boolean forRent = request.isForRent();
        ProductType resolvedProductType = resolveProductType(request.getProductType(), forRent, forSale);
        Double buyPrice = firstNonNull(request.getBuyPrice(), request.getSalePrice());

        log.info("[Product Update] BOOLEAN FLAGS FROM REQUEST - forSale: {}, forRent: {}", forSale, forRent);
        log.info("[Product Update] RESOLVED productType: {}", resolvedProductType);

        existing.setProductType(resolvedProductType);
        existing.setSalePrice(firstNonNull(request.getSalePrice(), buyPrice));
        existing.setRentPrice(request.getRentPrice());
        existing.setBuyPrice(buyPrice);
        existing.setForSale(forSale);
        existing.setForRent(forRent);
        existing.setQuantity(request.getQuantity());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setCity(request.getCity());
        existing.setAddress(request.getAddress());
        existing.setState(request.getState() != null ? request.getState() : "Maharashtra");

        log.info("[Product Update] About to reconcile images - productId: {}", id);
        log.info("[Product Update] Existing images count: {}",
                existing.getImages() != null ? existing.getImages().size() : 0);
        log.info("[Product Update] Request images: {}", request.getImages());
        log.info("[Product Update] New image URLs: {}", request.getNewImageUrls());
        log.info("[Product Update] Remove publicIds: {}", request.getRemoveImagePublicIds());

        existing.setImages(reconcileProductImages(existing.getImages(), request));

        log.info("[Product Update] After reconciliation - images count: {}", existing.getImages().size());

        Product updated = productRepository.save(existing);
        log.info("[Product Update] Product updated successfully - id: {}, title: {}", updated.getId(),
                updated.getTitle());
        return toDto(updated);
    }

    private List<ProductImage> reconcileProductImages(List<ProductImage> existingImages, ProductRequestDTO request) {
        log.info("[Reconcile Images] Starting reconciliation");
        log.info("[Reconcile Images] Existing images count: {}", existingImages != null ? existingImages.size() : 0);
        log.info("[Reconcile Images] Request images count: {}",
                request.getImages() != null ? request.getImages().size() : 0);

        if (existingImages == null) {
            existingImages = new ArrayList<>();
        }

        List<String> requestedUrls = request.getImages() != null ? request.getImages() : List.of();
        List<String> newImageUrls = request.getNewImageUrls() != null ? request.getNewImageUrls() : List.of();
        Set<String> removePublicIds = request.getRemoveImagePublicIds() != null
                ? new java.util.HashSet<>(request.getRemoveImagePublicIds())
                : Set.of();

        log.info("[Reconcile Images] removePublicIds count: {}, values: {}", removePublicIds.size(), removePublicIds);

        // Preserve images that are still present in the request and not explicitly
        // removed
        List<ProductImage> keptImages = existingImages.stream()
                .filter(image -> requestedUrls.contains(image.getUrl())
                        && !removePublicIds.contains(image.getPublicId()))
                .collect(Collectors.toList());
        log.info("[Reconcile Images] Kept images count: {}", keptImages.size());

        // Delete images that were removed from the listing
        List<ProductImage> imagesToDelete = existingImages.stream()
                .filter(image -> !requestedUrls.contains(image.getUrl())
                        || removePublicIds.contains(image.getPublicId()))
                .collect(Collectors.toList());

        log.info("[Reconcile Images] Images to delete count: {}", imagesToDelete.size());
        imagesToDelete.forEach(image -> {
            if (image.getPublicId() != null && !image.getPublicId().isBlank()) {
                log.info("[Reconcile Images] Deleting from Cloudinary - publicId: {}, url: {}",
                        image.getPublicId(), image.getUrl());
                try {
                    imageUploadService.deleteImage(image.getPublicId());
                    log.info("[Reconcile Images] ✅ Deleted from Cloudinary: {}", image.getPublicId());
                } catch (Exception ex) {
                    log.error("[Reconcile Images] ❌ Failed to delete from Cloudinary {}: {}",
                            image.getPublicId(), ex.getMessage(), ex);
                }
            } else {
                log.warn("[Reconcile Images] No publicId for image - url: {}", image.getUrl());
            }
        });

        // Add any newly uploaded images
        for (String newImageUrl : newImageUrls) {
            if (newImageUrl == null || newImageUrl.isBlank()) {
                continue;
            }
            boolean alreadyPresent = keptImages.stream()
                    .anyMatch(image -> newImageUrl.equals(image.getUrl()));
            if (alreadyPresent) {
                continue;
            }
            String publicId = extractPublicIdFromCloudinaryUrl(newImageUrl);
            keptImages.add(ProductImage.builder()
                    .url(newImageUrl)
                    .publicId(publicId)
                    .displayOrder(keptImages.size())
                    .build());
            log.info("[Reconcile Images] Added new image - url: {}, publicId: {}", newImageUrl, publicId);
        }

        // If request did not send any explicit newImageUrls, preserve any requested
        // image URLs that came from existing data
        for (String requestedUrl : requestedUrls) {
            boolean alreadyPresent = keptImages.stream().anyMatch(image -> requestedUrl.equals(image.getUrl()));
            if (!alreadyPresent) {
                String publicId = extractPublicIdFromCloudinaryUrl(requestedUrl);
                keptImages.add(ProductImage.builder()
                        .url(requestedUrl)
                        .publicId(publicId)
                        .displayOrder(keptImages.size())
                        .build());
                log.info("[Reconcile Images] Added kept image - url: {}, publicId: {}", requestedUrl, publicId);
            }
        }

        log.info("[Reconcile Images] ✅ Complete - final size: {}", keptImages.size());
        return keptImages;
    }

    private List<ProductImage> convertImageUrlsToProductImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return new ArrayList<>();
        }
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            if (url != null && !url.isBlank()) {
                images.add(ProductImage.builder()
                        .url(url)
                        .publicId(extractPublicIdFromCloudinaryUrl(url))
                        .displayOrder(i)
                        .build());
            }
        }
        return images;
    }

    private String extractPublicIdFromCloudinaryUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String filename = path.substring(path.lastIndexOf('/') + 1);
            if (filename.isBlank()) {
                return null;
            }
            int dotIndex = filename.lastIndexOf('.');
            return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        } catch (IllegalArgumentException ex) {
            log.debug("[Product Image] Failed to parse Cloudinary publicId from URL: {}", imageUrl);
            return null;
        }
    }

    @Override
    public void deleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        // only seller may delete (soft delete)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User authUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (!existing.getSeller().getId().equals(authUser.getId())) {
            throw new AccessDeniedException("Only the seller can delete this product");
        }
        existing.setDeleted(true);
        productRepository.save(existing);
    }

    @Override
    public List<ProductDTO> getMyProducts(String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        return productRepository.findBySeller_IdOrderByCreatedAtDesc(seller.getId()).stream()
                .filter(p -> !p.isDeleted())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toDto(product);
    }

    @Override
    public Page<ProductDTO> listActiveProducts(int page, int size) {
        var pg = productRepository.findByForSaleTrueAndQuantityGreaterThanAndDeletedFalse(0,
                PageRequest.of(page, size));
        List<ProductDTO> content = pg.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pg.getPageable(), pg.getTotalElements());
    }

    @Override
    public Page<ProductDTO> findNearbyProducts(double lat, double lng, double radiusKm, int page, int size,
            String sort) {
        // For nearby search, we use fixed sorting: distance ASC, then created_at DESC
        // The SQL query handles this, so we use unsorted Pageable for pagination only
        var pageable = PageRequest.of(page, size);
        Page<Product> nearbyPage = productRepository.findNearby(lat, lng, radiusKm, pageable);

        List<ProductDTO> content = nearbyPage.getContent().stream()
                .map(product -> {
                    ProductDTO dto = toDto(product);
                    // Calculate distance in Java - this ensures accurate results
                    double calculatedDistance = calculateDistance(lat, lng, product.getLatitude(),
                            product.getLongitude());
                    dto.setDistance(calculatedDistance);
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, nearbyPage.getPageable(), nearbyPage.getTotalElements());
    }

    private double calculateDistance(double lat1, double lng1, Double lat2, Double lng2) {
        if (lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    public Page<ProductDTO> searchProducts(
            String query,
            String brand,
            String category,
            String itemSize, // ✅ Renamed from 'size'
            String condition,
            Double minPrice,
            Double maxPrice,
            String type,
            String sort,
            int page,
            int sizePerPage) {
        Sort sortObj = toSort(sort);
        var pageRequest = PageRequest.of(page, sizePerPage, sortObj);

        // Base specification: not deleted, active, has stock
        // ✅ REMOVED: .and(ProductSpecifications.isForSale()) - was blocking rent items
        Specification<Product> spec = Specification
                .where(ProductSpecifications.isNotDeleted())
                .and(ProductSpecifications.hasStatus(ProductStatus.ACTIVE))
                .and(ProductSpecifications.hasQuantityGreaterThan(0));

        // ✅ FIXED: TYPE FILTER - Now handles rent/buy/both correctly
        if (type != null && !type.trim().isEmpty()) {
            log.info("[Search] Applying type filter: {}", type);

            if (type.equalsIgnoreCase("rent")) {
                // RENT ONLY: forRent=true
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("forRent")));
            } else if (type.equalsIgnoreCase("buy")) {
                // BUY ONLY: forSale=true
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("forSale")));
            } else if (type.equalsIgnoreCase("both")) {
                // BOTH: forRent=true OR forSale=true
                spec = spec.and((root, queryObj, cb) -> cb.or(
                        cb.isTrue(root.get("forRent")),
                        cb.isTrue(root.get("forSale"))));
            }

            // Apply price range for the specific type
            if (minPrice != null || maxPrice != null) {
                spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, type));
            }
        } else {
            // ✅ NO TYPE SPECIFIED: Show both rent and buy items
            spec = spec.and((root, queryObj, cb) -> cb.or(
                    cb.isTrue(root.get("forRent")),
                    cb.isTrue(root.get("forSale"))));

            // Apply generic price range
            if (minPrice != null || maxPrice != null) {
                spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, null));
            }
        }

        if (query != null && !query.trim().isEmpty()) {
            spec = spec.and(ProductSpecifications.hasKeyword(query));
        }
        if (brand != null && !brand.trim().isEmpty()) {
            spec = spec.and(ProductSpecifications.hasBrand(brand));
        }
        if (category != null && !category.trim().isEmpty()) {
            spec = spec.and(ProductSpecifications.hasCategory(category));
        }
        if (itemSize != null && !itemSize.trim().isEmpty()) {
            // ✅ EXTRA SAFETY: Validate size filter on backend
            String trimmedSize = itemSize.trim().toLowerCase();
            if (!trimmedSize.matches("(?i)xs|s|m|l|xl")) {
                log.warn("[Search] Invalid size filter received: {} - ignoring", itemSize);
                itemSize = null; // Ignore invalid size
            } else {
                log.info("[Search] Applying size filter: {}", trimmedSize);
                spec = spec.and(ProductSpecifications.hasSize(trimmedSize));
            }
        }

        // CONDITION FILTER
        if (condition != null && !condition.trim().isEmpty()) {
            log.info("[Search] Applying condition filter: {}", condition);
            spec = spec.and(ProductSpecifications.hasCondition(condition));
        }

        var pg = productRepository.findAll(spec, pageRequest);
        List<ProductDTO> content = pg.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pg.getPageable(), pg.getTotalElements());
    }

    private Sort toSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.toLowerCase().trim()) {
            case "price-low" -> Sort.by(Sort.Direction.ASC, "salePrice");
            case "price-high" -> Sort.by(Sort.Direction.DESC, "salePrice");
            case "popularity", "popular" -> Sort.by(Sort.Direction.DESC, "popularity");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Sort toSortForNearby(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.unsorted(); // Let the native query handle distance sorting
        }
        return switch (sort.toLowerCase().trim()) {
            case "distance" -> Sort.unsorted(); // Native query already sorts by distance
            case "price-low" -> Sort.by(Sort.Direction.ASC, "salePrice");
            case "price-high" -> Sort.by(Sort.Direction.DESC, "salePrice");
            case "popularity", "popular" -> Sort.by(Sort.Direction.DESC, "popularity");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.unsorted(); // Default to distance sorting
        };
    }

    @Override
    public Page<ProductDTO> searchProductsWithLocation(
            String query,
            String brand,
            String category,
            String itemSize, // ✅ Renamed from 'size'
            String condition,
            Double minPrice,
            Double maxPrice,
            String type,
            String sort,
            Double latitude,
            Double longitude,
            Double radiusKm,
            int page,
            int sizePerPage) {

        log.info("[Search] Location params - lat: {}, lng: {}, radius: {}km", latitude, longitude, radiusKm);
        log.info("[Search] Filter params - query: {}, brand: {}, category: {}, type: {}",
                query, brand, category, type);

        Sort sortObj = toSort(sort);
        var pageRequest = PageRequest.of(page, sizePerPage, sortObj);

        // Base specification: not deleted, active, has stock
        // ✅ REMOVED: .and(ProductSpecifications.isForSale()) - was blocking rent items
        Specification<Product> spec = Specification
                .where(ProductSpecifications.isNotDeleted())
                .and(ProductSpecifications.hasStatus(ProductStatus.ACTIVE))
                .and(ProductSpecifications.hasQuantityGreaterThan(0));

        // ✅ LOCATION FILTER - Apply only if latitude and longitude provided
        if (latitude != null && longitude != null) {
            log.info("[Search] Applying location filter with radius: {}km", radiusKm != null ? radiusKm : 20);
            double effectiveRadius = radiusKm != null ? radiusKm : 20.0;
            spec = spec.and(ProductSpecifications.withinRadius(latitude, longitude, effectiveRadius));
        } else {
            log.info("[Search] No location provided, skipping location filter");
        }

        // ✅ FIXED: TYPE FILTER - Now handles rent/buy/both correctly
        if (type != null && !type.trim().isEmpty()) {
            log.info("[Search] Applying type filter: {}", type);

            if (type.equalsIgnoreCase("rent")) {
                // RENT ONLY: forRent=true
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("forRent")));
            } else if (type.equalsIgnoreCase("buy")) {
                // BUY ONLY: forSale=true
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("forSale")));
            } else if (type.equalsIgnoreCase("both")) {
                // BOTH: forRent=true OR forSale=true
                spec = spec.and((root, queryObj, cb) -> cb.or(
                        cb.isTrue(root.get("forRent")),
                        cb.isTrue(root.get("forSale"))));
            }

            // Price range specific to rent/buy
            if (minPrice != null || maxPrice != null) {
                spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, type));
            }
        } else {
            // ✅ NO TYPE SPECIFIED: Show both rent and buy items
            spec = spec.and((root, queryObj, cb) -> cb.or(
                    cb.isTrue(root.get("forRent")),
                    cb.isTrue(root.get("forSale"))));

            // Apply generic price range
            if (minPrice != null || maxPrice != null) {
                spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, null));
            }
        }

        // KEYWORD SEARCH
        if (query != null && !query.trim().isEmpty()) {
            log.info("[Search] Applying keyword filter: {}", query);
            spec = spec.and(ProductSpecifications.hasKeyword(query));
        }

        // BRAND FILTER
        if (brand != null && !brand.trim().isEmpty()) {
            log.info("[Search] Applying brand filter: {}", brand);
            spec = spec.and(ProductSpecifications.hasBrand(brand));
        }

        // CATEGORY FILTER
        if (category != null && !category.trim().isEmpty()) {
            log.info("[Search] Applying category filter: {}", category);
            spec = spec.and(ProductSpecifications.hasCategory(category));
        }

        // SIZE FILTER
        if (itemSize != null && !itemSize.trim().isEmpty()) {
            // ✅ EXTRA SAFETY: Validate size filter on backend
            String trimmedSize = itemSize.trim().toLowerCase();
            if (!trimmedSize.matches("(?i)xs|s|m|l|xl")) {
                log.warn("[Search] Invalid size filter received: {} - ignoring", itemSize);
                itemSize = null; // Ignore invalid size
            } else {
                log.info("[Search] Applying size filter: {}", trimmedSize);
                spec = spec.and(ProductSpecifications.hasSize(trimmedSize));
            }
        }

        // CONDITION FILTER
        if (condition != null && !condition.trim().isEmpty()) {
            log.info("[Search] Applying condition filter: {}", condition);
            spec = spec.and(ProductSpecifications.hasCondition(condition));
        }

        // ✅ DEBUG LOGS: Log final filters applied
        log.info(
                "[Search] FINAL FILTERS APPLIED: query={}, brand={}, category={}, itemSize={}, condition={}, type={}, minPrice={}, maxPrice={}, location={}",
                query != null && !query.trim().isEmpty() ? query : "none",
                brand != null && !brand.trim().isEmpty() ? brand : "none",
                category != null && !category.trim().isEmpty() ? category : "none",
                itemSize != null && !itemSize.trim().isEmpty() ? itemSize : "none",
                condition != null && !condition.trim().isEmpty() ? condition : "none",
                type != null && !type.trim().isEmpty() ? type : "none",
                minPrice != null ? minPrice : "none",
                maxPrice != null ? maxPrice : "none",
                (latitude != null && longitude != null)
                        ? String.format("%.4f,%.4f (radius: %s)", latitude, longitude, radiusKm)
                        : "none");

        // ✅ FINAL DEBUG CHECK: Log the actual itemSize value being used
        log.info("[Search] FINAL itemSize USED: {}", itemSize);

        var pg = productRepository.findAll(spec, pageRequest);
        List<ProductDTO> content = pg.getContent().stream()
                .map(product -> {
                    ProductDTO dto = toDto(product);
                    // If location params provided, calculate distance
                    if (latitude != null && longitude != null) {
                        double distance = calculateDistance(latitude, longitude, product.getLatitude(),
                                product.getLongitude());
                        dto.setDistance(distance);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pg.getPageable(), pg.getTotalElements());
    }

    private ProductDTO toDto(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setBrand(p.getBrand());
        dto.setCategory(p.getCategory());
        dto.setSize(p.getSize());
        dto.setCondition(p.getProductCondition());
        dto.setProductType(p.getProductType());
        dto.setSalePrice(p.getSalePrice());
        dto.setRentPrice(p.getRentPrice());
        dto.setBuyPrice(firstNonNull(p.getBuyPrice(), p.getSalePrice()));
        dto.setPopularity(p.getPopularity());
        // Use entity fields directly instead of helper methods
        dto.setForSale(p.isForSale());
        dto.setForRent(p.isForRent());
        dto.setQuantity(p.getQuantity());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setCity(p.getCity());
        dto.setAddress(p.getAddress());
        dto.setState(p.getState());
        dto.setSellerId(p.getSeller() != null ? p.getSeller().getId() : null);
        dto.setImages(p.getImageUrls());
        return dto;
    }

    private static ProductType resolveProductType(ProductType explicit, boolean isForRent, boolean isForSale) {
        if (explicit != null) {
            System.out.println("[resolveProductType] Using explicit productType: " + explicit);
            return explicit;
        }
        // Fallback to boolean flags if no explicit productType
        if (isForRent && isForSale) {
            System.out.println("[resolveProductType] Both rent and sale flags true -> returning BOTH");
            return ProductType.BOTH;
        }
        if (isForRent) {
            System.out.println("[resolveProductType] Only rent flag true -> returning RENT");
            return ProductType.RENT;
        }
        if (isForSale) {
            System.out.println("[resolveProductType] Only sale flag true -> returning BUY");
            return ProductType.BUY;
        }
        System.out.println("[resolveProductType] No flags set -> defaulting to RENT");
        return ProductType.RENT;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }
}
