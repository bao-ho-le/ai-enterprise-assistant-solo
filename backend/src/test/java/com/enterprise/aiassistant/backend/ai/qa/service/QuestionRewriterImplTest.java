package com.enterprise.aiassistant.backend.ai.qa.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Case 1/2/3/4/8/9/10 của task: câu độc lập giữ nguyên, câu phụ thuộc context được rewrite
// bằng memory, LLM lỗi/null/blank đều fallback về nguyên văn userMessage.
class QuestionRewriterImplTest {

    private static final Long CONVERSATION_ID = 1L;

    private ConversationMemoryService conversationMemoryService;
    private LLMService llmService;
    private QuestionRewriterImpl questionRewriter;

    private AIConversation conversation;

    @BeforeEach
    void setUp() {
        conversationMemoryService = mock(ConversationMemoryService.class);
        llmService = mock(LLMService.class);

        questionRewriter = new QuestionRewriterImpl(
                conversationMemoryService,
                new PromptBuilderService(),
                llmService,
                new AIConversationHelper(),
                new AIMessageHelper()
        );

        conversation = AIConversation.builder().id(CONVERSATION_ID).build();
        when(conversationMemoryService.buildMemoryContext(CONVERSATION_ID))
                .thenReturn("USER:\nTài liệu này có đề cập đến testing không?\n\nASSISTANT:\nCó, tài liệu đề cập đến Unit Test, Integration Test, E2E Test và Slice Test.\n\n");
    }

    @Test
    void rewrite_contextDependentMessage_returnsStandaloneQuestionFromLlm() {
        when(llmService.generate(any(LLMRequest.class))).thenReturn(
                LLMResponse.builder().content("Giải thích chi tiết về testing được đề cập trong tài liệu.").build()
        );

        String rewritten = questionRewriter.rewrite(conversation, "Nói rõ về nó.");

        assertThat(rewritten).isEqualTo("Giải thích chi tiết về testing được đề cập trong tài liệu.");
    }

    @Test
    void rewrite_selfContainedMessage_llmReturnsUnchanged() {
        when(llmService.generate(any(LLMRequest.class))).thenReturn(
                LLMResponse.builder().content("Trong tài liệu, Spring Boot là gì?").build()
        );

        String rewritten = questionRewriter.rewrite(conversation, "Trong tài liệu, Spring Boot là gì?");

        assertThat(rewritten).isEqualTo("Trong tài liệu, Spring Boot là gì?");
    }

    @Test
    void rewrite_stripsWrappingQuotesFromLlmOutput() {
        when(llmService.generate(any(LLMRequest.class))).thenReturn(
                LLMResponse.builder().content("\"Giải thích chi tiết về Integration Test.\"").build()
        );

        String rewritten = questionRewriter.rewrite(conversation, "Giải thích kỹ loại thứ hai.");

        assertThat(rewritten).isEqualTo("Giải thích chi tiết về Integration Test.");
    }

    @Test
    void rewrite_llmException_fallsBackToOriginalMessage() {
        when(llmService.generate(any(LLMRequest.class)))
                .thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        String rewritten = questionRewriter.rewrite(conversation, "Nói rõ về nó.");

        assertThat(rewritten).isEqualTo("Nói rõ về nó.");
    }

    @Test
    void rewrite_llmReturnsNull_fallsBackToOriginalMessage() {
        when(llmService.generate(any(LLMRequest.class))).thenReturn(LLMResponse.builder().content(null).build());

        String rewritten = questionRewriter.rewrite(conversation, "Nói rõ về nó.");

        assertThat(rewritten).isEqualTo("Nói rõ về nó.");
    }

    @Test
    void rewrite_llmReturnsBlank_fallsBackToOriginalMessage() {
        when(llmService.generate(any(LLMRequest.class))).thenReturn(LLMResponse.builder().content("   ").build());

        String rewritten = questionRewriter.rewrite(conversation, "Nói rõ về nó.");

        assertThat(rewritten).isEqualTo("Nói rõ về nó.");
    }
}
