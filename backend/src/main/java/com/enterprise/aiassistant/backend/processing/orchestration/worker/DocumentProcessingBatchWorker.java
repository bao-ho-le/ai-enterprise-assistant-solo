package com.enterprise.aiassistant.backend.processing.orchestration.worker;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedBatchEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;


@Component
@Slf4j
public class DocumentProcessingBatchWorker {


    private final DocumentProcessingRetryExecutor documentProcessingRetryExecutor;

    private final Executor documentProcessingExecutor;


    public DocumentProcessingBatchWorker(
            DocumentProcessingRetryExecutor documentProcessingRetryExecutor,
            @Qualifier("documentProcessingExecutor")
            Executor documentProcessingExecutor
    ) {

        this.documentProcessingRetryExecutor =
                documentProcessingRetryExecutor;

        this.documentProcessingExecutor =
                documentProcessingExecutor;
    }


    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onDocumentVersionCreated(
            DocumentVersionCreatedBatchEvent event
    ) {

        event.versionIds()
                .forEach(versionId ->
                        documentProcessingExecutor.execute(
                                () -> submit(versionId)
                        )
                );
    }


    private void submit(Long versionId) {

        try {

            documentProcessingRetryExecutor
                    .processWithRetry(versionId);


        } catch (Exception exception) {

            log.error(
                    "{}. versionId={}",
                    ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                    versionId,
                    exception
            );
        }
    }
}
