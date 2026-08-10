package com.enterprise.aiassistant.backend.processing.orchestration.helper;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingHelper {

    private final DocumentVersionRepository documentVersionRepository;

    private final AIUsageLogService aiUsageLogService;

    // Chặn các trường hợp document đã được xử lí thành công
    public void validateStatus(DocumentVersion version) {

        if (version.getStatus() == VersionStatus.READY) {

            throw new BusinessException(
                    ErrorCode.DOCUMENT_VERSION_INVALID_STATUS
            );
        }
    }

    // Dùng transaction riêng để trạng thái FAILED luôn được lưu,
    // kể cả khi transaction xử lý chính bị rollback.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(
            Long versionId,
            Exception exception,
            ProcessingStep failedStep
    ) {
        documentVersionRepository.findById(versionId).ifPresentOrElse(
                version -> {
                    version.setStatus(VersionStatus.FAILED);
                    version.setProcessingStep(failedStep);
                    version.setErrorMessage(exception.getMessage());
                },
                () -> log.warn(
                        "Cannot mark version FAILED, it no longer exists. versionId={}",
                        versionId
                )
        );
    }

    // Chỉ đánh dấu document là FAILED nếu nó chưa xử lý thành công (READY).
    // Nếu đã READY thì bỏ qua, tránh ghi đè kết quả thành công.
    public void handleFailureUnlessAlreadySucceeded(
            Long versionId,
            DocumentVersion version,
            Exception exception
    ) {
        boolean alreadySucceeded =
                version != null
                        && version.getStatus() == VersionStatus.READY;

        if (!alreadySucceeded) {
            ProcessingStep failedStep =
                    version != null
                            ? version.getProcessingStep()
                            : null;

            handleFailed(versionId, exception, failedStep);
        }
    }

    public void logUsage(String model, Integer inputTokens, AIUsageStatus status, String errorMessage) {
        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationType(ConversationType.DOCUMENT_INDEXING)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(0)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

}
