package com.closetly.closetly_backend.chat.service;

import com.closetly.closetly_backend.chat.dto.MessageDTO;
import com.closetly.closetly_backend.chat.entity.ChatRoom;
import com.closetly.closetly_backend.chat.entity.Message;
import com.closetly.closetly_backend.chat.repository.ChatRoomRepository;
import com.closetly.closetly_backend.chat.repository.MessageRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    public MessageDTO sendMessage(MessageDTO messageDTO) {
        ChatRoom room = chatRoomRepository.findById(messageDTO.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));
        User sender = userRepository.findById(messageDTO.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        Message m = Message.builder()
                .chatRoom(room)
                .sender(sender)
                .content(messageDTO.getContent())
                .isRead(messageDTO.isRead())
                .build();
        Message saved = messageRepository.save(m);
        messageDTO.setId(saved.getId());
        messageDTO.setSentAt(saved.getSentAt());
        return messageDTO;
    }

    @Override
    public List<MessageDTO> getMessages(Long chatRoomId) {
        return messageRepository.findByChatRoomId(chatRoomId).stream()
                .map(m -> {
                    MessageDTO dto = new MessageDTO();
                    dto.setId(m.getId());
                    dto.setChatRoomId(m.getChatRoom().getId());
                    dto.setSenderId(m.getSender().getId());
                    dto.setContent(m.getContent());
                    dto.setSentAt(m.getSentAt());
                    dto.setRead(m.isRead());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
