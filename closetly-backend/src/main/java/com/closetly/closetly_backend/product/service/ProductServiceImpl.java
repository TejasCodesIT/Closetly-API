package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import com.closetly.closetly_backend.product.entity.ProductType;
import com.closetly.closetly_backend.product.repository.ProductRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ProductDTO createProduct(ProductRequestDTO request, String email) {
        log.info("[Product Creation] Starting product creation for user: {}", email);
        log.debug("[Product Creation] Incoming request - title: {}, category: {}, brand: {}",
                request.getTitle(), request.getCategory(), request.getBrand());

        // ✅ Log the DTO values immediately after deserialization
        log.info("[Product Creation] DTO DESERIALIZED - isForSale: {}, isForRent: {}",
                request.isForSale(), request.isForRent());
        log.info("[Product Creation] DTO DESERIALIZED - productType: {}, salePrice: {}, rentPricePerDay: {}",
                request.getProductType(), request.getSalePrice(), request.getRentPricePerDay());
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
        log.debug("[Product Creation] Request pricing - salePrice: {}, rentPricePerDay: {}",
                request.getSalePrice(), request.getRentPricePerDay());
        log.debug("[Product Creation] Request images count: {}",
                request.getImages() != null ? request.getImages().size() : 0);

        ProductType productType = resolveProductType(request.getProductType(), request.isForRent(),
                request.isForSale());
        boolean forRent = productType == ProductType.RENT || productType == ProductType.BOTH;
        boolean forSale = productType == ProductType.BUY || productType == ProductType.BOTH;
        Double buyPrice = firstNonNull(request.getBuyPrice(), request.getSalePrice());

        log.info("[Product Creation] Resolved productType: {}, forRent: {}, forSale: {}",
                productType, forRent, forSale);

        Product product = Product.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .brand(request.getBrand())
                .category(request.getCategory())
                .size(request.getSize())
                .productCondition(request.getCondition())
                .productType(productType)
                .salePrice(firstNonNull(request.getSalePrice(), buyPrice))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(request.getCity())
                .rentPricePerDay(request.getRentPricePerDay())
                .buyPrice(buyPrice)
                .popularity(0)
                .isForSale(forSale)
                .isForRent(forRent)
                .quantity(request.getQuantity())
                .images(request.getImages())
                .seller(seller)
                .status(Product.ProductStatus.ACTIVE)
                .build();

        log.debug("[Product Creation] Entity before save - title: {}, description: '{}' (length: {})",
                product.getTitle(),
                product.getDescription(),
                product.getDescription() != null ? product.getDescription().length() : 0);

        Product saved = productRepository.save(product);

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
        ProductType productType = resolveProductType(request.getProductType(), request.isForRent(),
                request.isForSale());
        boolean forRent = productType == ProductType.RENT || productType == ProductType.BOTH;
        boolean forSale = productType == ProductType.BUY || productType == ProductType.BOTH;
        Double buyPrice = firstNonNull(request.getBuyPrice(), request.getSalePrice());

        existing.setProductType(productType);
        existing.setSalePrice(firstNonNull(request.getSalePrice(), buyPrice));
        existing.setRentPricePerDay(request.getRentPricePerDay());
        existing.setBuyPrice(buyPrice);
        existing.setForSale(forSale);
        existing.setForRent(forRent);
        existing.setQuantity(request.getQuantity());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setCity(request.getCity());
        existing.setImages(request.getImages());

        Product updated = productRepository.save(existing);
        log.info("[Product Update] Product updated successfully - id: {}, title: {}", updated.getId(),
                updated.getTitle());
        return toDto(updated);
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
        var pg = productRepository.findByStatus(ProductStatus.ACTIVE, PageRequest.of(page, size));
        List<ProductDTO> content = pg.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pg.getPageable(), pg.getTotalElements());
    }

    @Override
    public List<ProductDTO> findNearbyProducts(double lat, double lng, double radiusKm) {
        List<Product> nearby = productRepository.findNearby(lat, lng, radiusKm);
        return nearby.stream()
                .map(product -> {
                    ProductDTO dto = toDto(product);
                    dto.setDistance(calculateDistance(lat, lng, product.getLatitude(), product.getLongitude()));
                    return dto;
                })
                .collect(Collectors.toList());
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
            String size,
            String condition,
            Double minPrice,
            Double maxPrice,
            String type,
            String sort,
            int page,
            int sizePerPage) {
        Sort sortObj = toSort(sort);
        var pageRequest = PageRequest.of(page, sizePerPage, sortObj);

        Specification<Product> spec = Specification
                .where(ProductSpecifications.isNotDeleted())
                .and(ProductSpecifications.hasStatus(ProductStatus.ACTIVE));

        if (type != null && !type.trim().isEmpty()) {

            if (type.equalsIgnoreCase("rent")) {
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("isForRent")));
            } else if (type.equalsIgnoreCase("buy")) {
                spec = spec.and((root, queryObj, cb) -> cb.isTrue(root.get("isForSale")));
            }
            if (type != null && !type.trim().isEmpty()) {
                // spec = spec.and(ProductSpecifications.hasType(type));
                spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, type));
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
        if (size != null && !size.trim().isEmpty()) {
            spec = spec.and(ProductSpecifications.hasSize(size));
        }
        if (condition != null && !condition.trim().isEmpty()) {
            spec = spec.and(ProductSpecifications.hasCondition(condition));
        }
        if (minPrice != null || maxPrice != null) {
            // spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice));
            spec = spec.and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice, type));
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
            case "popularity" -> Sort.by(Sort.Direction.DESC, "popularity");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
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
        dto.setRentPricePerDay(p.getRentPricePerDay());
        dto.setBuyPrice(firstNonNull(p.getBuyPrice(), p.getSalePrice()));
        dto.setPopularity(p.getPopularity());
        dto.setForSale(p.allowsBuy());
        dto.setForRent(p.allowsRent());
        dto.setQuantity(p.getQuantity());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setCity(p.getCity());
        dto.setSellerId(p.getSeller() != null ? p.getSeller().getId() : null);
        dto.setImages(p.getImages());
        return dto;
    }

    private static ProductType resolveProductType(ProductType explicit, boolean isForRent, boolean isForSale) {
        if (explicit != null) {
            return explicit;
        }
        if (isForRent && isForSale) {
            return ProductType.BOTH;
        }
        if (isForRent) {
            return ProductType.RENT;
        }
        if (isForSale) {
            return ProductType.BUY;
        }
        return ProductType.RENT;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }
}
