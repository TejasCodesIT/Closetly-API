package com.closetly.closetly_backend.chat.repository;

import com.closetly.closetly_backend.chat.entity.ChatRoom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // Normal active-only lookup via @Where(deleted=false)
    Optional<ChatRoom> findByBookingId(Long bookingId);

    Optional<ChatRoom> findByOrderId(Long orderId);

    // All-state lookups, including deleted rows, to avoid unique constraint hits on
    // re-create after soft delete.
    @Query(value = "SELECT * FROM chat_rooms WHERE booking_id = :bookingId LIMIT 1", nativeQuery = true)
    Optional<ChatRoom> findByBookingIdIncludeDeleted(@Param("bookingId") Long bookingId);

    @Query(value = "SELECT * FROM chat_rooms WHERE order_id = :orderId LIMIT 1", nativeQuery = true)
    Optional<ChatRoom> findByOrderIdIncludeDeleted(@Param("orderId") Long orderId);

    @Query(value = "SELECT * FROM chat_rooms WHERE product_id = :productId AND buyer_id = :buyerId AND seller_id = :sellerId LIMIT 1", nativeQuery = true)
    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerIdIncludeDeleted(@Param("productId") Long productId,
            @Param("buyerId") Long buyerId,
            @Param("sellerId") Long sellerId);

    // Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);

    List<ChatRoom> findByBuyerIdOrSellerId(Long buyerId, Long sellerId);

}
