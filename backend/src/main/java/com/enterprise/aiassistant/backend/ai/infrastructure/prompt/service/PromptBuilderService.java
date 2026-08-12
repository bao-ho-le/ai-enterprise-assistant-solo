package com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.FormGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import org.springframework.stereotype.Service;

import java.util.List;

// Builds the raw prompt text fed to LLMService.generate(...). One place for prompt
// shape so swapping FakeLLMService for a real model doesn't touch the handlers/QA flow.
@Service
public class PromptBuilderService {

    // Shared "plain text only, no Markdown/HTML/XML/rich text" core that every
    // generation type's Base Instructions build on top of — one place for the
    // constraint instead of copy-pasting it per generation type. The frontend only
    // ever renders raw text (e.g. GeneratedContentPreview.js: `whitespace-pre-wrap`,
    // no Markdown renderer), so unrendered Markdown syntax would otherwise leak
    // through as literal "#"/"**"/etc. characters.
    private static final String PLAIN_TEXT_INSTRUCTIONS = """
            Output plain text only. Do not use Markdown, HTML, XML, or any other rich-text \
            formatting anywhere in the response: no "#", "##", "###", "**", "_", Markdown \
            bullet markers ("-", "*"), Markdown numbered lists, block quotes (">"), code \
            blocks, or any other Markdown/formatting characters.
            
            Do not add unnecessary whitespace or blank lines. Break lines sensibly between \
            ideas/paragraphs so the text reads well as plain text.""";

    // Standing instructions appended to every email regardless of length/audience/tone.
    private static final String EMAIL_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a professional email. Use a normal plain-text email format — greeting, \
            body, closing, and signature — written as plain sentences and paragraphs, never \
            as Markdown headings or lists.""";

    // Standing instructions appended to every report regardless of length/audience/tone.
    // Forces plain-text output (no Markdown) with numbered headings so every report
    // renders consistently on the frontend.
    private static final String REPORT_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a professional business report. Number every section heading as a \
            plain line, for example "1. Executive Summary" then "2. Background", \
            continuing sequentially through the last section. A heading is only the \
            number, a period, a space, and the title — never prefix it with "#", "##", \
            "###", or any other symbol, and never bold or underline it.
            
            Leave exactly one blank line between the report title, each heading, and each \
            section's content — no extra blank lines, no trailing whitespace. Keep the \
            information coherent, do not repeat the same point in multiple sections, and \
            always end with a numbered "Conclusion" section.""";

    // Standing instructions appended to every summary regardless of length/audience/style.
    private static final String SUMMARY_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a document summary. If presented as bullet points, use a plain-text \
            bullet marker ("•") or an ordinary number followed by a period — never a \
            Markdown "-" or "*" marker. If presented under headings, write each heading as \
            a plain line of text — never prefixed with "#", "##", "###", or any other \
            symbol.""";

    // Standing instructions appended to every Document QA answer.
    private static final String DOCUMENT_QA_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            Do not use headings, code blocks, or tables of any kind, Markdown or \
            otherwise. If a list is needed, use plain numbering ("1.", "2.") or a plain \
            bullet ("•") — never a Markdown list marker.
            
            Answer concisely and clearly, based strictly on the provided document \
            excerpts. If the excerpts do not contain enough information to answer, say so \
            plainly instead of guessing or inventing information.
            
            Answer in the same language the question was asked in. If that language is \
            Vietnamese, write entirely in standard Vietnamese with full diacritics (tone \
            marks and vowel marks) on every word — never unaccented Vietnamese ("khong \
            dau") — and do not mix in English words or phrases unless there is no natural \
            Vietnamese equivalent.

            Conversation memory is contextual information from earlier turns. Use it only \
            to understand references and conversational context. Do not treat memory as \
            authoritative document evidence. When answering questions about document \
            facts, rely on the provided document excerpts; if memory and the excerpts \
            disagree, the excerpts win.""";

    // Độ dài mục tiêu của memory summary sau khi nén (ký tự), khớp với ngưỡng nén
    // trong ConversationMemoryHelper.
    private static final int MEMORY_SUMMARY_TARGET_CHARACTERS = 6000;

    // Standing instructions appended to every Conversation Summary answer — trả lời
    // user dựa trên conversation memory, không phải nén memory nội bộ.
    private static final String CONVERSATION_SUMMARY_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """


            Do not use headings, code blocks, or tables of any kind, Markdown or \
            otherwise. If a list is needed, use plain numbering ("1.", "2.") or a plain \
            bullet ("•") — never a Markdown list marker.

            Summarize only what the conversation memory actually contains: do not add \
            information, do not invent turns that are not there, and do not answer \
            questions about attached documents. Follow the length and shape the user asked \
            for — if they asked for notes, produce something they can paste straight into \
            their notes. If the memory does not contain enough information to answer, say \
            so plainly.

            Answer in the same language the message was written in. If that language is \
            Vietnamese, write entirely in standard Vietnamese with full diacritics (tone \
            marks and vowel marks) on every word — never unaccented Vietnamese ("khong \
            dau") — and do not mix in English words or phrases unless there is no natural \
            Vietnamese equivalent.""";

    // Standing instructions appended to every General Chat answer. Same plain-text
    // constraint as the other flows — the chat UI renders raw text, not Markdown.
    private static final String GENERAL_CHAT_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """


            Do not use headings, code blocks, or tables of any kind, Markdown or \
            otherwise. If a list is needed, use plain numbering ("1.", "2.") or a plain \
            bullet ("•") — never a Markdown list marker.

            Answer in the same language the message was written in. If that language is \
            Vietnamese, write entirely in standard Vietnamese with full diacritics (tone \
            marks and vowel marks) on every word — never unaccented Vietnamese ("khong \
            dau") — and do not mix in English words or phrases unless there is no natural \
            Vietnamese equivalent.""";

    public String buildEmailPrompt(EmailGenerationInput input) {
        String tone = valueOrDefault(input.getTone(), "Professional");
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Draft a professional email.
                Recipient: %s
                Purpose: %s
                Additional context: %s
                Sender: %s
                
                Language: %s. %s
                Tone: %s. %s
                Length: %s. %s
                Audience: %s. %s
                
                %s
                """.formatted(
                valueOrDefault(input.getRecipient(), "the recipient"),
                input.getPurpose(),
                valueOrDefault(input.getOptionalContext(), "None"),
                valueOrDefault(input.getSender(), "[Your Name]"),
                language, languageInstruction(language),
                tone, toneInstruction(tone),
                length, emailLengthInstruction(length),
                audience, audienceInstruction(audience),
                EMAIL_BASE_INSTRUCTIONS
        );
    }

    public String buildReportPrompt(ReportGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Write a business report titled "%s".
                Instructions: %s
                Reporting period: %s
                
                Language: %s. %s
                Length: %s. %s
                Audience: %s. %s
                
                %s
                
                Source documents:
                %s
                """.formatted(
                input.getTitle(),
                valueOrDefault(input.getInstructions(), "None"),
                dateRange(input.getFromDate(), input.getToDate()),
                language, languageInstruction(language),
                length, reportLengthInstruction(length),
                audience, audienceInstruction(audience),
                REPORT_BASE_INSTRUCTIONS,
                valueOrDefault(documentContext, "No source documents attached.")
        );
    }

    public String buildSummaryPrompt(SummaryGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String style = valueOrDefault(input.getStyle(), "PARAGRAPH");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Summarize the attached documents.
                Instructions: %s
                Audience: %s
                Include a dedicated action items section: %s
                
                Language: %s. %s
                Length: %s. %s
                Presentation style: %s. %s
                
                %s
                
                Source documents:
                %s
                """.formatted(
                valueOrDefault(input.getInstructions(), "None"),
                valueOrDefault(input.getAudience(), "General audience"),
                Boolean.TRUE.equals(input.getIncludeActionItems()) ? "Yes" : "No",
                language, languageInstruction(language),
                length, summaryLengthInstruction(length),
                style, summaryStyleInstruction(style),
                SUMMARY_BASE_INSTRUCTIONS,
                valueOrDefault(documentContext, "No source documents attached.")
        );
    }

    public String buildFormPrompt(FormGenerationInput input) {
        return """
                Design a form for the following purpose: %s
                Desired fields: %s
                """.formatted(
                input.getPurpose(),
                valueOrDefault(input.getFields(), "Infer reasonable fields from the purpose.")
        );
    }

    public String buildDocumentQaPrompt(
            String question,
            List<String> contextChunks,
            String conversationMemory
    ) {

        String context = contextChunks == null || contextChunks.isEmpty()
                ? "No relevant passages were found."
                : String.join("\n---\n", contextChunks);

        return """
                Answer the following question using only the provided document excerpts. Question: %s
                
                %s

                Conversation memory:
                %s

                Document excerpts:
                %s
                """.formatted(
                question,
                DOCUMENT_QA_BASE_INSTRUCTIONS,
                valueOrDefault(conversationMemory, "No previous conversation context."),
                context
        );
    }

    // Nén memory nội bộ (không phải câu trả lời cho user): gộp summarizedContext cũ +
    // pendingContext thành một summarizedContext mới ngắn hơn.
    public String buildConversationMemorySummaryPrompt(
            String summarizedContext,
            String pendingContext
    ) {

        return """
                Compress the conversation context below into a single memory summary that will be \
                reused as context for the next turns of this same conversation.

                Keep: the topics being discussed, decisions already made, the user's requirements and \
                preferences, important entities/terms/numbers, and anything needed to understand \
                follow-up questions and references ("that number", "the previous one", ...).

                Drop: greetings, small talk, repetition, and wording that carries no information.

                Write at most %d characters, shorter if the important information still fits. Write \
                compact plain prose in the language of the conversation. Do not use Markdown or any \
                other rich-text formatting.

                Do not add information that is not present below. Do not answer any question found in \
                the context. Output only the memory summary and nothing else.

                Previous memory summary:
                %s

                New conversation turns since that summary:
                %s
                """.formatted(
                MEMORY_SUMMARY_TARGET_CHARACTERS,
                valueOrDefault(summarizedContext, "None."),
                valueOrDefault(pendingContext, "None.")
        );
    }

    // Trả lời user khi họ hỏi về chính cuộc hội thoại (intent CONVERSATION_SUMMARY).
    // Khác memory summary: đây là output user đọc/copy được, độ dài theo yêu cầu của user.
    public String buildConversationSummaryPrompt(
            String message,
            String conversationMemory
    ) {

        return """
                The user is asking about the current conversation itself, not about any attached \
                document. Answer their request using only the conversation memory below.

                %s

                Conversation memory:
                %s

                User message:
                %s
                """.formatted(
                CONVERSATION_SUMMARY_BASE_INSTRUCTIONS,
                valueOrDefault(conversationMemory, "No previous conversation context."),
                message
        );
    }

    public String buildGeneralChatPrompt(String message) {

        return """
                Reply to the user's message as a helpful assistant. The user is not asking about any \
                attached document, so answer from general knowledge.

                %s

                User message:
                %s
                """.formatted(GENERAL_CHAT_BASE_INSTRUCTIONS, message);
    }

    // Chạy trước Document QA retrieval, không phải câu trả lời cho user: output đi thẳng
    // vào embedding + Qdrant nên không cần PLAIN_TEXT_INSTRUCTIONS, chỉ cần đúng 1 câu hỏi.
    public String buildQuestionRewritePrompt(
            String userMessage,
            String conversationMemory
    ) {

        return """
                You are a question rewriting component for a document question-answering system.

                Rewrite the current user message into a standalone question that can be understood \
                without the previous conversation. Use the conversation context only to resolve \
                references and omitted subjects — pronouns like "it", "that", "nó", "cái đó", "phần \
                đó", ordinal references like "the second one", "loại thứ hai", or an implied subject.

                If the current message is already self-contained, return it unchanged.

                Do not answer the question. Do not add facts that are not supported by the \
                conversation context. Do not change the user's intent or the language they wrote in.

                Output only the rewritten question and nothing else — no explanation, no Markdown, \
                no surrounding quotation marks.

                Conversation context:
                %s

                Current user message:
                %s
                """.formatted(
                valueOrDefault(conversationMemory, "No previous conversation context."),
                userMessage
        );
    }

    // Routing-only prompt: the model classifies the message, it never answers it.
    // Output is parsed straight into the Intent enum, so no extra text is allowed.
    public String buildIntentClassificationPrompt(String message) {

        return """
                Classify the user's message into exactly one intent.

                DOCUMENT_QA: The user is asking a question that should be answered using the attached documents.
                SUMMARY: The user wants to summarize, condense, overview, or extract the key points from the attached documents.
                CONVERSATION_SUMMARY: The user wants a summary or an overview of the current conversation itself — what has been discussed or asked so far in this chat, the main points exchanged, or notes about this conversation. Not about the attached documents.
                GENERAL_CHAT: The user is having a normal conversation or asking something that does not require information from the attached documents.

                SUMMARY is about the documents. CONVERSATION_SUMMARY is about this chat. If the message mentions "this conversation", "we", "so far", "just now" ("nãy giờ", "cuộc hội thoại này", "chúng ta vừa trao đổi"), choose CONVERSATION_SUMMARY.

                Reply with exactly one of these values and nothing else: DOCUMENT_QA, SUMMARY, GENERAL_CHAT, CONVERSATION_SUMMARY.
                Do not answer the message. Do not explain. Do not add quotes, punctuation, or any other text.

                User message:
                %s
                """.formatted(message);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String dateRange(String fromDate, String toDate) {
        if ((fromDate == null || fromDate.isBlank()) && (toDate == null || toDate.isBlank())) {
            return "Not specified";
        }
        return valueOrDefault(fromDate, "…") + " to " + valueOrDefault(toDate, "…");
    }

    // Shared by email/report/summary prompts. Models sometimes drop Vietnamese tone marks
    // ("Kinh gui" instead of "Kính gửi") on longer generations, so Vietnamese gets an
    // explicit, unambiguous nudge instead of relying on the bare language name.
    private String languageInstruction(String language) {
        return switch (language.trim()) {
            case "Vietnamese" -> "Write entirely in standard Vietnamese with full diacritics " +
                    "(tone marks and vowel marks) on every word — never write unaccented " +
                    "Vietnamese (\"khong dau\"). Do not mix in English words or phrases unless " +
                    "there is no natural Vietnamese equivalent. Use natural, native-sounding " +
                    "Vietnamese phrasing throughout, including in headings and closings.";
            default -> "Write entirely in " + language + ".";
        };
    }

    // Email-only — Write Report has no Tone field.
    private String toneInstruction(String tone) {
        return switch (tone.trim()) {
            case "Formal" -> "Use formal, dignified language.";
            case "Friendly" -> "Sound warm and friendly while staying professional.";
            case "Persuasive" -> "Focus on persuading the reader through strong arguments and clear benefits.";
            case "Apologetic" -> "Show genuine empathy, a sincere apology, and a clear path to resolution.";
            default -> "Sound objective, clear, and trustworthy (professional tone).";
        };
    }

    // Shared by email and report prompts — audience options overlap; unrecognized/blank
    // values (including plain "General Audience") fall back to the general-audience case.
    private String audienceInstruction(String audience) {
        return switch (audience.trim()) {
            case "Internal Team" ->
                    "Write for internal colleagues: you may use internal terminology and assume shared context.";
            case "Business Partner" -> "Write for a business partner: emphasize collaboration and shared goals.";
            case "Customer" ->
                    "Write for a customer: be polite, explain things clearly, and highlight benefits and next steps.";
            case "Executive", "Executive Leadership" ->
                    "Write for executives: focus on decisions, business impact, risks, and recommendations.";
            case "Board of Directors" ->
                    "Write for the board of directors: focus on governance-level decisions, business impact, risks, and recommendations.";
            default -> "Write for a general audience: use plain, accessible language and avoid jargon.";
        };
    }

    private String emailLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> "About 5-8 sentences. Focus only on the essential information for a concise email.";
            case "Long" ->
                    "About 18-25 sentences across 4-6 paragraphs, with full explanations, supporting information, and next steps where relevant.";
            default ->
                    "About 10-15 sentences across 2-3 paragraphs, with enough context, explanation, and a clear closing.";
        };
    }

    private String reportLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> """
                    Keep the report concise with exactly these 3 numbered sections in order:
                    1. Executive Summary (1 short paragraph)
                    2. Key Findings (2-3 short paragraphs)
                    3. Conclusion (1 short paragraph)
                    
                    Target approximately 300-500 words in total.
                    Focus only on the most important information and avoid unnecessary detail.
                    """;

            case "Long" -> """
                    Produce a comprehensive report with exactly these 8 numbered sections in order:
                    1. Executive Summary (1-2 paragraphs)
                    2. Background (2-3 paragraphs)
                    3. Objectives (1-2 paragraphs)
                    4. Analysis (4-6 paragraphs)
                    5. Key Findings (3-4 paragraphs)
                    6. Risks & Challenges (2-3 paragraphs)
                    7. Recommendations (2-3 paragraphs)
                    8. Conclusion (1-2 paragraphs)
                    
                    Target approximately 1200-1800 words in total.
                    Each section should provide sufficient detail while remaining focused and avoiding repetition.
                    """;

            default -> """
                    Produce a well-balanced report with exactly these 5 numbered sections in order:
                    1. Executive Summary (1 paragraph)
                    2. Background (2 paragraphs)
                    3. Analysis (3-4 paragraphs)
                    4. Key Findings (2-3 paragraphs)
                    5. Recommendations (2 paragraphs)
                    
                    Target approximately 700-1000 words in total.
                    Maintain a good balance between readability and detail.
                    """;
        };
    }

    private String summaryLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> "About 100-150 words, covering only the most important points.";
            case "Long" -> "About 400-600 words, covering all the important content in full.";
            default -> "About 200-350 words, covering the main content.";
        };
    }

    private String summaryStyleInstruction(String style) {
        return switch (style.trim().toUpperCase()) {
            case "BULLET_POINTS" ->
                    "Present the summary as bullet points; each bullet should cover exactly one main idea.";
            case "STRUCTURED" -> "Present the summary under numbered headings, for example " +
                    "\"1. Overview\" then \"2. Key Findings\", \"3. Important Details\", \"4. Conclusion\", " +
                    "continuing sequentially (5., 6., ...) if there are more headings. Each heading is " +
                    "only the number, a period, a space, and the title — plain text, never \"#\", \"##\", " +
                    "\"###\", or any other Markdown symbol.";
            default -> "Present the summary as natural, well-connected paragraphs.";
        };
    }
}
