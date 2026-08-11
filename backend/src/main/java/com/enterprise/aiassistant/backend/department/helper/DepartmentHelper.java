package com.enterprise.aiassistant.backend.department.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DepartmentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.UserException;
import com.enterprise.aiassistant.backend.department.dto.request.AddDepartmentMembersRequest;
import com.enterprise.aiassistant.backend.department.dto.request.CreateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.request.UpdateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.user.entity.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepartmentHelper {

    private static final int MAX_NAME_LENGTH = 255;

    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    public void validateDepartmentId(Long departmentId) {

        if (departmentId == null) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_ID_REQUIRED);
        }

        if (departmentId <= 0) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_ID_INVALID);
        }
    }

    public void validateUserId(Long userId) {

        if (userId == null) {
            throw new UserException(ErrorCode.USER_ID_REQUIRED);
        }

        if (userId <= 0) {
            throw new UserException(ErrorCode.USER_ID_INVALID);
        }
    }

    public void validateCreateRequest(CreateDepartmentRequest request) {

        if (request == null) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_REQUEST_REQUIRED);
        }

        validateName(request.getName());
        validateDescription(request.getDescription());
    }

    public void validateUpdateRequest(Long departmentId, UpdateDepartmentRequest request) {

        validateDepartmentId(departmentId);

        if (request == null) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_REQUEST_REQUIRED);
        }

        if (request.getName() != null) {
            validateName(request.getName());
        }

        validateDescription(request.getDescription());
    }

    public void validateAddMembersRequest(Long departmentId, AddDepartmentMembersRequest request) {

        validateDepartmentId(departmentId);

        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new UserException(ErrorCode.USER_ID_REQUIRED);
        }

        request.getUserIds().forEach(this::validateUserId);
    }

    // Manager phải là member của chính department đó và có role đủ thẩm quyền.
    public void validateManagerCandidate(Department department, User candidate) {

        if (candidate.getRole() != Role.MANAGER && candidate.getRole() != Role.ADMIN) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_MANAGER_ROLE_INVALID);
        }

        boolean sameDepartment = candidate.getDepartment() != null
                && candidate.getDepartment().getId().equals(department.getId());

        if (!sameDepartment) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_MANAGER_NOT_MEMBER);
        }
    }

    public void validateDepartmentIsEmpty(List<User> members) {

        if (!members.isEmpty()) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_NOT_EMPTY);
        }
    }

    private void validateName(String name) {

        if (name == null || name.isBlank()) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_NAME_REQUIRED);
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_NAME_TOO_LONG);
        }
    }

    private void validateDescription(String description) {

        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_DESCRIPTION_TOO_LONG);
        }
    }
}
