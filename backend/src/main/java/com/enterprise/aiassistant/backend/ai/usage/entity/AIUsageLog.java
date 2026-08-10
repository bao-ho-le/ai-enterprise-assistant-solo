package com.enterprise.aiassistant.backend.ai.usage.entity;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_usage_logs",
        indexes = {
                @Index(name = "idx_ai_usage_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_usage_conversation_type", columnList = "conversation_type"),
                @Index(name = "idx_ai_usage_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_conversation_id")
    private Long aiConversationId;

    @Column(name = "ai_message_id")
    private Long aiMessageId;

    @Column(name = "generation_id")
    private Long generationId;

    @Column(name = "conversation_type", nullable = false, length = 50)
    private ConversationType conversationType;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AIUsageStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}