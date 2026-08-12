package com.enterprise.aiassistant.backend.document.dto;

import java.util.List;

// Scope ABAC được nén lại để đẩy xuống query: thay vì load hết document rồi lọc trong bộ nhớ.
// unrestricted = ADMIN/SUPERVISOR (nhìn thấy toàn hệ thống).
public record DocumentAccessScope(
        boolean unrestricted,
        Long userId,
        Long departmentId,
        List<Long> sharedDocumentIds
) {
}
