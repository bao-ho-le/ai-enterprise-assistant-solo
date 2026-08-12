package com.enterprise.aiassistant.backend.admin.user.service;

import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminCreateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminUpdateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AssignRoleRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.ChangeDepartmentRequest;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.UserException;
import com.enterprise.aiassistant.backend.department.service.DepartmentService;
import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.mapper.UserMapper;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Admin use-case cho User. Entity/repository vẫn nằm ở module user/, ở đây chỉ orchestrate.
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final DepartmentService departmentService;

    private final CurrentUserService currentUserService;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(
            String keyword,
            Role role,
            Long departmentId,
            Boolean enabled,
            Pageable pageable
    ) {

        currentUserService.requirePermission(Permission.USER_READ);

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return userRepository.searchUsers(safeKeyword, role, departmentId, enabled, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserDetail(Long userId) {

        currentUserService.requirePermission(Permission.USER_READ);

        return userMapper.toResponse(getUserOrThrow(userId));
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {

        validateCreateRequest(request);

        currentUserService.requirePermission(Permission.USER_CREATE);

        if (Boolean.TRUE.equals(userRepository.existsByUsername(request.getUsername().trim()))) {
            throw new AuthenticationException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail().trim()))) {
            throw new AuthenticationException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .fullName(request.getFullName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        if (request.getDepartmentId() != null) {
            user.setDepartment(departmentService.getDepartmentOrThrow(request.getDepartmentId()));
        }

        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request) {

        validateUpdateRequest(userId, request);

        currentUserService.requirePermission(Permission.USER_UPDATE);

        User user = getUserOrThrow(userId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmailAndIdNot(request.getEmail().trim(), userId)) {
                throw new AuthenticationException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            user.setEmail(request.getEmail().trim());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {

        validateUserId(userId);

        currentUserService.requirePermission(enabled ? Permission.USER_ENABLE : Permission.USER_DISABLE);

        validateNotSelf(userId);

        User user = getUserOrThrow(userId);
        user.setEnabled(enabled);
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse assignRole(Long userId, AssignRoleRequest request) {

        validateUserId(userId);

        if (request == null || request.getRole() == null) {
            throw new UserException(ErrorCode.ROLE_REQUIRED);
        }

        currentUserService.requirePermission(Permission.USER_ASSIGN_ROLE);

        // Chặn admin tự hạ quyền chính mình rồi khoá cả hệ thống ra ngoài.
        validateNotSelf(userId);

        User user = getUserOrThrow(userId);
        user.setRole(request.getRole());
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse changeDepartment(Long userId, ChangeDepartmentRequest request) {

        validateUserId(userId);

        if (request == null) {
            throw new UserException(ErrorCode.USER_REQUEST_REQUIRED);
        }

        currentUserService.requirePermission(Permission.USER_CHANGE_DEPARTMENT);

        User user = getUserOrThrow(userId);

        user.setDepartment(
                request.getDepartmentId() == null
                        ? null
                        : departmentService.getDepartmentOrThrow(request.getDepartmentId())
        );

        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {

        validateUserId(userId);

        currentUserService.requirePermission(Permission.USER_DELETE);

        validateNotSelf(userId);

        userRepository.delete(getUserOrThrow(userId));
    }

    // Helper

    private User getUserOrThrow(Long userId) {

        validateUserId(userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.TARGET_USER_NOT_FOUND));
    }

    private void validateUserId(Long userId) {

        if (userId == null) {
            throw new UserException(ErrorCode.USER_ID_REQUIRED);
        }

        if (userId <= 0) {
            throw new UserException(ErrorCode.USER_ID_INVALID);
        }
    }

    private void validateNotSelf(Long userId) {

        if (userId.equals(currentUserService.getCurrentUserId())) {
            throw new UserException(ErrorCode.USER_CANNOT_MODIFY_SELF);
        }
    }

    private void validateCreateRequest(AdminCreateUserRequest request) {

        if (request == null) {
            throw new UserException(ErrorCode.USER_REQUEST_REQUIRED);
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new UserException(ErrorCode.USERNAME_REQUIRED);
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new UserException(ErrorCode.USER_EMAIL_REQUIRED);
        }

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new UserException(ErrorCode.USER_FULL_NAME_REQUIRED);
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new UserException(ErrorCode.USER_PASSWORD_REQUIRED);
        }

        if (request.getPassword().length() < 8) {
            throw new UserException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }

        if (request.getRole() == null) {
            throw new UserException(ErrorCode.ROLE_REQUIRED);
        }
    }

    private void validateUpdateRequest(Long userId, AdminUpdateUserRequest request) {

        validateUserId(userId);

        if (request == null) {
            throw new UserException(ErrorCode.USER_REQUEST_REQUIRED);
        }

        if (request.getFullName() != null && request.getFullName().isBlank()) {
            throw new UserException(ErrorCode.USER_FULL_NAME_REQUIRED);
        }

        if (request.getEmail() != null && request.getEmail().isBlank()) {
            throw new UserException(ErrorCode.USER_EMAIL_REQUIRED);
        }
    }
}
