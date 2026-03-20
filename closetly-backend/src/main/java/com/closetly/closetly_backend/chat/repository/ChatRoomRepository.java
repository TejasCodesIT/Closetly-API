package com.closetly.closetly_backend.chat.repository;

import com.closetly.closetly_backend.chat.entity.ChatRoom;
import com.closetly.closetly_backend.chat.entity.Message;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBookingId(Long bookingId);

    // Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    List<ChatRoom> findByBuyerIdOrSellerId(Long buyerId, Long sellerId);

}
