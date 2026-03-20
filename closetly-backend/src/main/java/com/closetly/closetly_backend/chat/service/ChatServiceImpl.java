package com.closetly.closetly_backend.chat.service;

import com.closetly.closetly_backend.chat.dto.MessageDTO;
import com.closetly.closetly_backend.chat.entity.ChatRoom;
import com.closetly.closetly_backend.chat.entity.Message;
import com.closetly.closetly_backend.chat.repository.ChatRoomRepository;
import com.closetly.closetly_backend.chat.repository.MessageRepository;
import com.closetly.closetly_backend.booking.entity.Booking;
import com.closetly.closetly_backend.booking.entity.Booking.BookingStatus;
import com.closetly.closetly_backend.booking.repository.BookingRepository;
import com.closetly.closetly_backend.chat.dto.ChatRoomDTO;
import com.closetly.closetly_backend.chat.dto.CreateChatRoomRequestDTO;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.common.ResourceNotFoundException;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import com.closetly.closetly_backend.order.repository.OrderRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;

    private static final List<OrderStatus> PURCHASED_STATUSES = List.of(
            OrderStatus.PAID,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    @Override
    @Transactional
    public ChatRoomDTO createOrGetRoom(CreateChatRoomRequestDTO request, String authEmail) {
        User authUser = requireAuthUser(authEmail);
        Long authUserId = authUser.getId();

        Long productId = request.getProductId();
        Long buyerId = authUserId;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Long sellerId = product.getSeller().getId();

        // Security: only the buyer or seller can create/access the room.
        if (!authUserId.equals(buyerId) && !authUserId.equals(sellerId)) {
            throw new AccessDeniedException("You are not a chat participant for this product");
        }

        boolean rentApproved = bookingRepository
                .findByProductIdAndCustomerIdAndStatus(productId, buyerId, BookingStatus.APPROVED)
                .isPresent();

        boolean orderPurchased = orderRepository
                .findFirstByProductIdAndCustomerIdAndStatusIn(productId, buyerId, PURCHASED_STATUSES)
                .isPresent();

        // Business rule: room exists only after rent approval or purchase.
        if (!rentApproved && !orderPurchased) {
            throw new IllegalArgumentException(
                    "Chat room can only be created after rent approval or product purchase"
            );
        }

        // Unique room: productId + buyerId.
        Optional<ChatRoom> existing = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId);
        if (existing.isPresent()) {
            ChatRoom room = backfillRoomFields(existing.get(), productId, buyerId, sellerId, existing.get().getBooking());
            return toDto(room);
        }

        // Rental fallback for legacy/partial data.
        if (rentApproved) {
            Booking booking = bookingRepository
                    .findByProductIdAndCustomerIdAndStatus(productId, buyerId, BookingStatus.APPROVED)
                    .orElseThrow();

            Optional<ChatRoom> byBooking = chatRoomRepository.findByBookingId(booking.getId());
            if (byBooking.isPresent()) {
                ChatRoom room = backfillRoomFields(byBooking.get(), productId, buyerId, sellerId, booking);
                return toDto(room);
            }

            ChatRoom created = ChatRoom.builder()
                    .booking(booking)
                    .productId(productId)
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .build();
            return toDto(chatRoomRepository.save(created));
        }

        // Purchase chat room (booking is not required).
        ChatRoom created = ChatRoom.builder()
                .productId(productId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();
        return toDto(chatRoomRepository.save(created));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getMyRooms(String authEmail) {
        User authUser = requireAuthUser(authEmail);
        Long authUserId = authUser.getId();

        return chatRoomRepository.findByBuyerIdOrSellerId(authUserId, authUserId).stream()
                .map(this::backfillForDtoIfNeeded)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long chatRoomId, String authEmail) {
        User authUser = requireAuthUser(authEmail);
        assertUserCanAccessRoom(chatRoomId, authUser.getId());
        return messageRepository.findByChatRoomIdWithSender(chatRoomId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(MessageDTO messageDTO, String authEmail) {
        User authUser = requireAuthUser(authEmail);
        Long authUserId = authUser.getId();

        // Derive/verify sender from authenticated user.
        if (messageDTO.getSenderId() == null) {
            messageDTO.setSenderId(authUserId);
        } else if (!authUserId.equals(messageDTO.getSenderId())) {
            throw new AccessDeniedException("Sender does not match authenticated user");
        }

        assertUserCanAccessRoom(messageDTO.getChatRoomId(), authUserId);

        ChatRoom room = requireRoom(messageDTO.getChatRoomId());

        Message saved = messageRepository.save(Message.builder()
                .chatRoom(room)
                .sender(authUser)
                .content(messageDTO.getContent())
                .isRead(messageDTO.isRead())
                .build());

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertUserCanAccessRoom(Long chatRoomId, Long authUserId) {
        ChatRoom room = requireRoom(chatRoomId);
        Long buyerId = resolveBuyerId(room);
        Long sellerId = resolveSellerId(room);

        if (!authUserId.equals(buyerId) && !authUserId.equals(sellerId)) {
            throw new AccessDeniedException("You are not a participant of this chat room");
        }
    }

    private User requireAuthUser(String authEmail) {
        return userRepository.findByEmail(authEmail)
                .orElseThrow(() -> new AccessDeniedException("Invalid user"));
    }

    private ChatRoom requireRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

    private Long resolveBuyerId(ChatRoom room) {
        if (room.getBuyerId() != null) {
            return room.getBuyerId();
        }
        if (room.getBooking() != null && room.getBooking().getCustomer() != null) {
            return room.getBooking().getCustomer().getId();
        }
        throw new IllegalStateException("Chat room missing buyerId");
    }

    private Long resolveSellerId(ChatRoom room) {
        if (room.getSellerId() != null) {
            return room.getSellerId();
        }
        if (room.getBooking() != null && room.getBooking().getProduct() != null
                && room.getBooking().getProduct().getSeller() != null) {
            return room.getBooking().getProduct().getSeller().getId();
        }
        throw new IllegalStateException("Chat room missing sellerId");
    }

    private ChatRoom backfillForDtoIfNeeded(ChatRoom room) {
        if (room.getBuyerId() != null && room.getSellerId() != null && room.getProductId() != null) {
            return room;
        }
        if (room.getBooking() == null || room.getBooking().getProduct() == null || room.getBooking().getCustomer() == null) {
            return room;
        }

        Long productId = room.getBooking().getProduct().getId();
        Long buyerId = room.getBooking().getCustomer().getId();
        Long sellerId = room.getBooking().getProduct().getSeller() != null
                ? room.getBooking().getProduct().getSeller().getId()
                : null;

        if (buyerId == null || sellerId == null) {
            return room;
        }
        return backfillRoomFields(room, productId, buyerId, sellerId, room.getBooking());
    }

    private ChatRoom backfillRoomFields(ChatRoom room,
                                          Long productId,
                                          Long buyerId,
                                          Long sellerId,
                                          Booking bookingOrNull) {
        boolean changed = false;

        if (room.getProductId() == null && productId != null) {
            room.setProductId(productId);
            changed = true;
        }
        if (room.getBuyerId() == null && buyerId != null) {
            room.setBuyerId(buyerId);
            changed = true;
        }
        if (room.getSellerId() == null && sellerId != null) {
            room.setSellerId(sellerId);
            changed = true;
        }
        if (bookingOrNull != null && room.getBooking() == null) {
            room.setBooking(bookingOrNull);
            changed = true;
        }

        return changed ? chatRoomRepository.save(room) : room;
    }

    private ChatRoomDTO toDto(ChatRoom room) {
        ChatRoomDTO dto = new ChatRoomDTO();
        dto.setId(room.getId());
        dto.setProductId(room.getProductId());
        dto.setBuyerId(room.getBuyerId());
        dto.setSellerId(room.getSellerId());
        dto.setCreatedAt(room.getCreatedAt());
        return dto;
    }

    private MessageDTO toDto(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChatRoomId(message.getChatRoom().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName() != null ? message.getSender().getFullName()
                : message.getSender().getEmail());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt() != null ? message.getSentAt().toString() : null);
        dto.setRead(message.isRead());
        return dto;
}
}
