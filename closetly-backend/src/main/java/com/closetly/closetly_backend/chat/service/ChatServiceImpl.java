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

        if (!authUserId.equals(buyerId) && !authUserId.equals(sellerId)) {
            throw new AccessDeniedException("You are not a chat participant for this product");
        }

        boolean rentApproved = bookingRepository
                .findByProductIdAndCustomerIdAndStatus(productId, buyerId, BookingStatus.APPROVED)
                .isPresent();

        boolean orderPurchased = orderRepository
                .findFirstByProductIdAndCustomerIdAndStatusIn(productId, buyerId, PURCHASED_STATUSES)
                .isPresent();

        if (!rentApproved && !orderPurchased) {
            throw new IllegalArgumentException(
                    "Chat room can only be created after rent approval or product purchase"
            );
        }

        Optional<ChatRoom> existing = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId);
        if (existing.isPresent()) {
            ChatRoom room = existing.get();
            return toDto(room, authUserId);
        }

        ChatRoom created = ChatRoom.builder()
                .productId(productId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();

        return toDto(chatRoomRepository.save(created), authUserId);
    }

    // ✅ FIXED METHOD
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

    // ==============================
    // 🔥 MAIN DTO LOGIC
    // ==============================
    private ChatRoomDTO toDto(ChatRoom room, Long currentUserId) {
        ChatRoomDTO dto = new ChatRoomDTO();

        dto.setId(room.getId());
        dto.setProductId(room.getProductId());
        dto.setBuyerId(room.getBuyerId());
        dto.setSellerId(room.getSellerId());
        dto.setCreatedAt(room.getCreatedAt());

        // ✅ OTHER USER
        Long otherUserId = room.getBuyerId().equals(currentUserId)
                ? room.getSellerId()
                : room.getBuyerId();

        User otherUser = userRepository.findById(otherUserId).orElse(null);

        if (otherUser != null) {
            dto.setName(
                    otherUser.getFullName() != null
                            ? otherUser.getFullName()
                            : otherUser.getEmail()
            );
        }

        // ✅ LAST MESSAGE
        Optional<Message> lastMessageOpt =
                messageRepository.findTopByChatRoomIdOrderBySentAtDesc(room.getId());

        if (lastMessageOpt.isPresent()) {
            Message lastMsg = lastMessageOpt.get();

            dto.setLastMessage(lastMsg.getContent());
            dto.setTime(
                    lastMsg.getSentAt() != null
                            ? lastMsg.getSentAt().toString()
                            : ""
            );
        }

        return dto;
    }

    // ==============================
    // HELPERS
    // ==============================
    private User requireAuthUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Invalid user"));
    }

    private ChatRoom requireRoom(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

  @Override
public void assertUserCanAccessRoom(Long chatRoomId, Long userId) {
    ChatRoom room = requireRoom(chatRoomId);

    if (!userId.equals(room.getBuyerId()) && !userId.equals(room.getSellerId())) {
        throw new AccessDeniedException("Not part of this chat");
    }
}

    private MessageDTO toDto(Message message) {
        MessageDTO dto = new MessageDTO();

        dto.setId(message.getId());
        dto.setChatRoomId(message.getChatRoom().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(
                message.getSender().getFullName() != null
                        ? message.getSender().getFullName()
                        : message.getSender().getEmail()
        );
        dto.setContent(message.getContent());
        dto.setSentAt(
                message.getSentAt() != null
                        ? message.getSentAt().toString()
                        : null
        );
        dto.setRead(message.isRead());

        return dto;
    }
}