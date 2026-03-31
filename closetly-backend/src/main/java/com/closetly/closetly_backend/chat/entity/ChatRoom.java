package com.closetly.closetly_backend.chat.entity;

import com.closetly.closetly_backend.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
                @UniqueConstraint(name = "uq_chat_rooms_booking_deleted", columnNames = { "booking_id", "deleted" }),
                @UniqueConstraint(name = "uq_chat_rooms_order_deleted", columnNames = { "order_id", "deleted" }),
                @UniqueConstraint(name = "uq_chat_rooms_product_buyer_seller_deleted", columnNames = { "product_id",
                                "buyer_id", "seller_id", "deleted" })
})
@Where(clause = "deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // Optional: rental chats come from an approved booking; purchase chats use
        // order reference.
        @OneToOne(fetch = FetchType.LAZY, optional = true)
        @JoinColumn(name = "booking_id", nullable = true)
        private Booking booking;

        @ManyToOne(fetch = FetchType.LAZY, optional = true)
        @JoinColumn(name = "order_id", nullable = true)
        private com.closetly.closetly_backend.order.entity.Order order;

        @Column(name = "product_id")
        private Long productId;

        @Column(name = "buyer_id")
        private Long buyerId;

        @Column(name = "seller_id")
        private Long sellerId;

        @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<Message> messages = new ArrayList<>();

        @CreationTimestamp
        private LocalDateTime createdAt;

        @UpdateTimestamp
        private LocalDateTime updatedAt;

        @Builder.Default
        private boolean deleted = false;
}
