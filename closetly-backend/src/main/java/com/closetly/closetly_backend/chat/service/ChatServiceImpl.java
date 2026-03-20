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

import jakarta.persistence.EntityManager;

import com.closetly.closetly_backend.common.ResourceNotFoundException;
import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import com.closetly.closetly_backend.order.repository.OrderRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
// import com.closetly.closetly_backend.security.websocket.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @Autowired
    private EntityManager entityManager;

    private static final List<OrderStatus> PURCHASED_STATUSES = List.of(
            OrderStatus.PAID,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED);

    @Override
    @Transactional
    public ChatRoomDTO createOrGetRoom(CreateChatRoomRequestDTO request, String authEmail) {
        User authUser = requireAuthUser(authEmail);
        Long authUserId = authUser.getId();

        Long productId = request.getProductId();
        Long bookingId = request.getBookingId();
        Long orderId = request.getOrderId();

        if (productId == null || (bookingId == null && orderId == null))
            throw new IllegalArgumentException("Missing required fields");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Long sellerId = product.getSeller() != null ? product.getSeller().getId() : null;
        if (sellerId == null)
            throw new IllegalArgumentException("Could not resolve sellerId from product");

        Booking booking = null;
        Order order = null;
        Long buyerId;

        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
            buyerId = booking.getCustomer().getId();
        } else {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            buyerId = order.getCustomer().getId();
        }

        if (!authUserId.equals(buyerId) && !authUserId.equals(sellerId))
            throw new AccessDeniedException("You are not a participant of this chat");

        // ✅ Safe check with pessimistic lock
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId);
        if (existingRoom.isPresent())
            return toDto(existingRoom.get(), authUserId);

        if (booking != null && booking.getStatus() != BookingStatus.APPROVED)
            throw new IllegalArgumentException("Chat room can only be created after rent approval");

        if (order != null && !PURCHASED_STATUSES.contains(order.getStatus()))
            throw new IllegalArgumentException("Chat room can only be created after product purchase");

        ChatRoom newRoom = ChatRoom.builder()
                .booking(booking)
                .productId(productId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();

        ChatRoom saved = chatRoomRepository.saveAndFlush(newRoom);
        return toDto(saved, authUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getMyRooms(String authEmail) {
        User authUser = requireAuthUser(authEmail);
        Long authUserId = authUser.getId();

        return chatRoomRepository.findByBuyerIdOrSellerId(authUserId, authUserId).stream()
                .map(room -> toDto(room, authUserId))
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

        if (messageDTO.getSenderId() == null) {
            messageDTO.setSenderId(authUserId);
        } else if (!authUserId.equals(messageDTO.getSenderId())) {
            throw new AccessDeniedException("Sender mismatch");
        }

        assertUserCanAccessRoom(messageDTO.getChatRoomId(), authUserId);

        ChatRoom room = requireRoom(messageDTO.getChatRoomId());

        Message saved = messageRepository.save(Message.builder()
                .chatRoom(room)
                .sender(authUser)
                .content(messageDTO.getContent())
                .isRead(false)
                .build());
        return toDto(saved);
    }

    private ChatRoomDTO toDto(ChatRoom room, Long currentUserId) {
        ChatRoomDTO dto = new ChatRoomDTO();

        Long roomBuyerId = room.getBuyerId();
        Long roomSellerId = room.getSellerId();

        dto.setId(room.getId());
        dto.setProductId(room.getProductId());
        dto.setBuyerId(roomBuyerId);
        dto.setSellerId(roomSellerId);
        dto.setCreatedAt(room.getCreatedAt());

        // Other participant
        Long otherUserId = roomBuyerId != null && roomBuyerId.equals(currentUserId) ? roomSellerId : roomBuyerId;
        User otherUser = otherUserId != null ? userRepository.findById(otherUserId).orElse(null) : null;

        dto.setName(
                otherUser != null ? (otherUser.getFullName() != null ? otherUser.getFullName() : otherUser.getEmail())
                        : "User");

        // Last message
        Optional<Message> lastMessage = messageRepository.findTopByChatRoomIdOrderBySentAtDesc(room.getId());
        lastMessage.ifPresent(msg -> {
            dto.setLastMessage(msg.getContent());
            dto.setTime(msg.getSentAt() != null ? msg.getSentAt().toString() : "");
        });

        return dto;
    }

    private User requireAuthUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Invalid user"));
    }

    private ChatRoom requireRoom(Long id) {
        if (id == null)
            throw new IllegalArgumentException("chatRoomId is required");
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

    @Override
    public void assertUserCanAccessRoom(Long chatRoomId, Long userId) {
        ChatRoom room = requireRoom(chatRoomId);
        if ((room.getBuyerId() == null || !userId.equals(room.getBuyerId())) &&
                (room.getSellerId() == null || !userId.equals(room.getSellerId()))) {
            throw new AccessDeniedException("Not part of this chat");
        }
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