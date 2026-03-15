package com.closetly.closetly_backend.wishlist.repository;

import com.closetly.closetly_backend.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    List<Wishlist> findByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product p WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);
}