package com.enterprise.aiassistant.backend.ai.knowledge.generation.entity;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "generations",
        indexes = {
                @Index(name = "idx_generation_conversation_id", columnList = "ai_conversation_id"),
                @Index(name = "idx_generation_content_id", columnList = "generated_content_id"),
                @Index(name = "idx_generation_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_conversation_id", nullable = false)
    private AIConversation aiConversation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_content_id", unique = true)
    private GeneratedContent generatedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", nullable = false)
    private GeneratedType generatedType;

    // LƯU Ý: Hiện tại phần title này chỉ lưu vào database, chứ chưa có nơi nào gọi để dùng
    // Phần hiển thị title trên lịch sử conversation tại frontend là dùng title của conversation đang
    // được khởi tạo mặc định bên trong /conversation/helper
    @Column(length = 500)
    private String title;

    @Column(name = "user_prompt", columnDefinition = "text")
    private String userPrompt;

    // No JsonNode/@Convert infra in this project; stored as raw JSON text like other text columns.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", columnDefinition = "jsonb")
    private JsonNode inputData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
