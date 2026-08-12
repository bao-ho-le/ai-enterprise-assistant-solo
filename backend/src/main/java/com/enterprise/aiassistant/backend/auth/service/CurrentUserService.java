package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.UserException;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Điểm vào duy nhất để service layer lấy identity của người đang gọi.
// Không controller nào được tự truyền userId xuống service.
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public UserPrincipal getCurrentPrincipal() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthorizationException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        return principal;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {

        Long userId = getCurrentPrincipal().getId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    public Long getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }

    public Long getCurrentDepartmentId() {
        return getCurrentPrincipal().getDepartmentId();
    }

    public boolean hasPermission(Permission permission) {
        return getCurrentPrincipal().hasPermission(permission);
    }

    // Dùng khi thao tác không thuộc về resource cụ thể (RBAC thuần), ABAC check nằm ở các policy riêng.
    public void requirePermission(Permission permission) {
        if (!hasPermission(permission)) {
            throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
