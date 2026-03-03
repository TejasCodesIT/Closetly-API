package com.closetly.closetly_backend.chat.service;

import com.closetly.closetly_backend.chat.dto.MessageDTO;

import java.util.List;

public interface ChatService {
    MessageDTO sendMessage(MessageDTO messageDTO);

    List<MessageDTO> getMessages(Long chatRoomId);
}
