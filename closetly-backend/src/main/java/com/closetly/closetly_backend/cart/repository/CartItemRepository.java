package com.closetly.closetly_backend.cart.repository;

import com.closetly.closetly_backend.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByUserIdAndDeletedFalse(Long userId);

    Optional<CartItem> findByIdAndUserIdAndDeletedFalse(String id, Long userId);

    boolean existsByUserIdAndProductIdAndTypeAndDeletedFalse(Long userId, Long productId, CartItem.CartItemType type);

    @Query("SELECT c FROM CartItem c JOIN FETCH c.product p WHERE c.user.id = :userId AND c.deleted = false")
    List<CartItem> findByUserIdWithProducts(@Param("userId") Long userId);

    void deleteByUserIdAndDeletedFalse(Long userId);
}