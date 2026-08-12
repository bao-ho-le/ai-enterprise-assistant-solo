package com.enterprise.aiassistant.backend.department.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DepartmentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.UserException;
import com.enterprise.aiassistant.backend.department.dto.request.AddDepartmentMembersRequest;
import com.enterprise.aiassistant.backend.department.dto.request.AssignManagerRequest;
import com.enterprise.aiassistant.backend.department.dto.request.CreateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.request.UpdateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentDetailResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentResponse;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.helper.DepartmentHelper;
import com.enterprise.aiassistant.backend.department.mapper.DepartmentMapper;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    private final UserRepository userRepository;

    private final DocumentRepository documentRepository;

    private final AIUsageLogRepository aiUsageLogRepository;

    private final DepartmentHelper departmentHelper;

    private final DepartmentMapper departmentMapper;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getDepartments(String keyword, Pageable pageable) {

        currentUserService.requirePermission(Permission.DEPARTMENT_READ);

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return departmentRepository.searchByName(safeKeyword, pageable)
                .map(department -> departmentMapper.toResponse(
                        department,
                        userRepository.countByDepartmentId(department.getId())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDetailResponse getDepartmentDetail(Long departmentId) {

        departmentHelper.validateDepartmentId(departmentId);

        currentUserService.requirePermission(Permission.DEPARTMENT_READ);

        Department department = getDepartmentOrThrow(departmentId);

        requireDepartmentVisible(departmentId);

        return buildDetail(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDetailResponse getMyDepartment() {

        currentUserService.requirePermission(Permission.DEPARTMENT_READ);

        Long departmentId = currentUserService.getCurrentDepartmentId();

        if (departmentId == null) {
            throw new DepartmentException(ErrorCode.USER_HAS_NO_DEPARTMENT);
        }

        return buildDetail(getDepartmentOrThrow(departmentId));
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {

        departmentHelper.validateCreateRequest(request);

        currentUserService.requirePermission(Permission.DEPARTMENT_CREATE);

        if (departmentRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_ALREADY_EXISTS);
        }

        Department department = departmentMapper.toDepartment(request);
        department.setName(request.getName().trim());

        departmentRepository.save(department);

        return departmentMapper.toResponse(department, 0);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request) {

        departmentHelper.validateUpdateRequest(departmentId, request);

        currentUserService.requirePermission(Permission.DEPARTMENT_UPDATE);

        Department department = getDepartmentOrThrow(departmentId);

        if (request.getName() != null) {

            String name = request.getName().trim();

            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, departmentId)) {
                throw new DepartmentException(ErrorCode.DEPARTMENT_ALREADY_EXISTS);
            }

            department.setName(name);
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        departmentRepository.save(department);

        return departmentMapper.toResponse(
                department,
                userRepository.countByDepartmentId(departmentId)
        );
    }

    @Override
    @Transactional
    public void deleteDepartment(Long departmentId) {

        departmentHelper.validateDepartmentId(departmentId);

        currentUserService.requirePermission(Permission.DEPARTMENT_DELETE);

        Department department = getDepartmentOrThrow(departmentId);

        departmentHelper.validateDepartmentIsEmpty(
                userRepository.findByDepartmentIdOrderByFullNameAsc(departmentId)
        );

        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    public DepartmentResponse assignManager(Long departmentId, AssignManagerRequest request) {

        departmentHelper.validateDepartmentId(departmentId);

        currentUserService.requirePermission(Permission.DEPARTMENT_UPDATE);

        Department department = getDepartmentOrThrow(departmentId);

        if (request == null || request.getManagerId() == null) {
            department.setManager(null);
            departmentRepository.save(department);
            return departmentMapper.toResponse(
                    department,
                    userRepository.countByDepartmentId(departmentId)
            );
        }

        departmentHelper.validateUserId(request.getManagerId());

        User candidate = userRepository.findById(request.getManagerId())
                .orElseThrow(() -> new UserException(ErrorCode.TARGET_USER_NOT_FOUND));

        departmentHelper.validateManagerCandidate(department, candidate);

        department.setManager(candidate);
        departmentRepository.save(department);

        return departmentMapper.toResponse(
                department,
                userRepository.countByDepartmentId(departmentId)
        );
    }

    @Override
    @Transactional
    public DepartmentDetailResponse addMembers(Long departmentId, AddDepartmentMembersRequest request) {

        departmentHelper.validateAddMembersRequest(departmentId, request);

        currentUserService.requirePermission(Permission.DEPARTMENT_MANAGE_MEMBERS);

        Department department = getDepartmentOrThrow(departmentId);

        requireManageableDepartment(departmentId);

        List<User> users = userRepository.findByIdIn(request.getUserIds().stream().distinct().toList());

        if (users.size() != request.getUserIds().stream().distinct().count()) {
            throw new UserException(ErrorCode.TARGET_USER_NOT_FOUND);
        }

        // Một user chỉ thuộc đúng 1 department tại một thời điểm — set đè department cũ.
        users.forEach(user -> user.setDepartment(department));

        userRepository.saveAll(users);

        return buildDetail(department);
    }

    @Override
    @Transactional
    public DepartmentDetailResponse removeMember(Long departmentId, Long userId) {

        departmentHelper.validateDepartmentId(departmentId);
        departmentHelper.validateUserId(userId);

        currentUserService.requirePermission(Permission.DEPARTMENT_MANAGE_MEMBERS);

        Department department = getDepartmentOrThrow(departmentId);

        requireManageableDepartment(departmentId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.TARGET_USER_NOT_FOUND));

        if (user.getDepartment() == null || !user.getDepartment().getId().equals(departmentId)) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_MANAGER_NOT_MEMBER);
        }

        user.setDepartment(null);
        userRepository.save(user);

        // Manager rời department thì department không còn manager.
        if (department.getManager() != null && department.getManager().getId().equals(userId)) {
            department.setManager(null);
            departmentRepository.save(department);
        }

        return buildDetail(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentOrThrow(Long departmentId) {

        departmentHelper.validateDepartmentId(departmentId);

        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    // Helper

    private DepartmentDetailResponse buildDetail(Department department) {

        Long departmentId = department.getId();

        return departmentMapper.toDetailResponse(
                department,
                userRepository.findByDepartmentIdOrderByFullNameAsc(departmentId),
                documentRepository.countByDepartmentIdAndStatus(departmentId, DocumentStatus.ACTIVE),
                aiUsageLogRepository.countByDepartmentId(departmentId),
                aiUsageLogRepository.sumTotalTokensByDepartmentId(departmentId),
                aiUsageLogRepository.sumEstimatedCostByDepartmentId(departmentId)
        );
    }

    // ADMIN và SUPERVISOR xem được mọi department; role còn lại chỉ xem department của mình.
    private void requireDepartmentVisible(Long departmentId) {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        if (principal.getRole() == Role.ADMIN || principal.getRole() == Role.SUPERVISOR) {
            return;
        }

        if (!departmentId.equals(principal.getDepartmentId())) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Manager chỉ quản lý được members của chính department mình.
    private void requireManageableDepartment(Long departmentId) {

        UserPrincipal principal = currentUserService.getCurrentPrincipal();

        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        if (!departmentId.equals(principal.getDepartmentId())) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }
}
