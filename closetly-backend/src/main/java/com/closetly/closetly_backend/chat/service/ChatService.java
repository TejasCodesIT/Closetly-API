package com.closetly.closetly_backend.chat.service;

import com.closetly.closetly_backend.chat.dto.ChatRoomDTO;
import com.closetly.closetly_backend.chat.dto.CreateChatRoomRequestDTO;
import com.closetly.closetly_backend.chat.dto.MessageDTO;

import java.util.List;

public interface ChatService {
    ChatRoomDTO createOrGetRoom(CreateChatRoomRequestDTO request, String authEmail);

    List<ChatRoomDTO> getMyRooms(String authEmail);

    List<MessageDTO> getMessages(Long chatRoomId, String authEmail);

    MessageDTO sendMessage(MessageDTO messageDTO, String authEmail);

    void assertUserCanAccessRoom(Long chatRoomId, Long authUserId);
}
