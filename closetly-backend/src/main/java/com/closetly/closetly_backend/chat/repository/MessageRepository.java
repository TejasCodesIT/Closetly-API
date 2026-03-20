package com.closetly.closetly_backend.chat.repository;

import com.closetly.closetly_backend.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoom_Id(Long chatRoomId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chatRoom.id = :roomId")
    List<Message> findByChatRoomIdWithSender(Long roomId);
     Optional<Message> findTopByChatRoomIdOrderBySentAtDesc(Long chatRoomId);
}
    

