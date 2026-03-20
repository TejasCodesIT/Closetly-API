package com.closetly.closetly_backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class MessageDTO {
    private Long id;

    @NotNull(message = "chatRoomId is required")
    private Long chatRoomId;

    // Optional in payload; server can derive it from authenticated user.
    private Long senderId;

    private String senderName;

    @NotBlank(message = "content is required")
    private String content;

    // The backend derives this for persisted messages; accept any client-provided value.
    // String avoids strict `LocalDateTime` parsing issues (e.g. ISO strings with `Z`).
    private String sentAt;

    @JsonProperty("isRead")
    @JsonAlias({"read"})
    private boolean isRead;
}
