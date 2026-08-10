package com.enterprise.aiassistant.backend.ai.memory.repository;

import com.enterprise.aiassistant.backend.ai.memory.entity.ConversationMemory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, Long> {

    Optional<ConversationMemory> findByConversationId(Long conversationId);

    // Hai message cùng conversation gửi gần như đồng thời sẽ cùng đọc/ghi một row memory,
    // khoá row lúc đọc để lượt sau nối tiếp lượt trước thay vì ghi đè mất dữ liệu.
    // ponytail: khoá giữ tới hết transaction (gồm cả lần gọi LLM khi nén memory);
    // nếu chat đông user cùng conversation thì tách memory update ra transaction riêng.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT memory FROM ConversationMemory memory WHERE memory.conversation.id = :conversationId")
    Optional<ConversationMemory> findByConversationIdForUpdate(@Param("conversationId") Long conversationId);

    void deleteByConversationId(Long conversationId);
}
