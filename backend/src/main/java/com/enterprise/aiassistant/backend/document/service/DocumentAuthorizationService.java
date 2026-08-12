package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.helper.DocumentAuthorizationHelper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Cổng authorization duy nhất cho Document ở service layer. Controller không tự quyết định gì.
@Service
@RequiredArgsConstructor
public class DocumentAuthorizationService {

    private final CurrentUserService currentUserService;

    private final DocumentAccessRepository documentAccessRepository;

    private final DocumentAuthorizationHelper documentAuthorizationHelper;

    public boolean canRead(Document document) {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        return documentAuthorizationHelper.canRead(
                principal,
                document,
                hasExplicitAccess(principal, document)
        );
    }

    public void requireRead(Document document) {

        if (!canRead(document)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireDownload(Document document) {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        if (!documentAuthorizationHelper.canDownload(
                principal, document, hasExplicitAccess(principal, document))) {

            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireUpdate(Document document) {

        if (!documentAuthorizationHelper.canUpdate(currentUserService.getCurrentPrincipal(), document)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireDelete(Document document) {

        if (!documentAuthorizationHelper.canDelete(currentUserService.getCurrentPrincipal(), document)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireManageAccess(Document document) {

        if (!documentAuthorizationHelper.canManageAccess(currentUserService.getCurrentPrincipal(), document)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireCreate() {

        if (!documentAuthorizationHelper.canCreate(currentUserService.getCurrentPrincipal())) {
            throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
        }
    }

    // Dùng cho AI retrieval: bỏ các document mà user không có quyền đọc trước khi build context.
    @Transactional(readOnly = true)
    public Set<Long> filterReadableDocumentIds(Collection<Document> documents) {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        Set<Long> sharedDocumentIds = Set.copyOf(
                documentAccessRepository.findDocumentIdsSharedWithUser(principal.getId())
        );

        return documents.stream()
                .filter(document -> documentAuthorizationHelper.canRead(
                        principal,
                        document,
                        sharedDocumentIds.contains(document.getId())
                ))
                .map(Document::getId)
                .collect(Collectors.toSet());
    }

    // Scope để đẩy xuống repo, lọc bằng query
    @Transactional(readOnly = true)
    public DocumentAccessScope currentAccessScope() {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        // Không bị chặn việc Get nếu là role Admin hoặc Supervisor
        boolean unrestricted = principal.getRole() == Role.ADMIN
                || principal.getRole() == Role.SUPERVISOR;

        List<Long> sharedDocumentIds = unrestricted
                ? List.of()
                : documentAccessRepository.findDocumentIdsSharedWithUser(principal.getId());

        return new DocumentAccessScope(
                unrestricted,
                principal.getId(),
                principal.getDepartmentId(),
                sharedDocumentIds
        );
    }

    private boolean hasExplicitAccess(UserPrincipal principal, Document document) {

        return document.getId() != null
                && documentAccessRepository.existsByDocumentIdAndUserId(
                document.getId(), principal.getId());
    }
}
