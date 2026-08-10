package com.enterprise.aiassistant.backend.ai.chat.message.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message content is too long")
    private String content;


}
