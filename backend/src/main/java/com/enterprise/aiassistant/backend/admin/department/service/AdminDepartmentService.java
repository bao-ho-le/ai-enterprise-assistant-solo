package com.enterprise.aiassistant.backend.admin.department.service;

import com.enterprise.aiassistant.backend.department.dto.request.AddDepartmentMembersRequest;
import com.enterprise.aiassistant.backend.department.dto.request.AssignManagerRequest;
import com.enterprise.aiassistant.backend.department.dto.request.CreateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.request.UpdateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentDetailResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentResponse;
import com.enterprise.aiassistant.backend.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Admin use-case cho Department. Permission + validate đã nằm trong DepartmentService của
// module department/, ở đây chỉ orchestrate cho controller.
@Service
@RequiredArgsConstructor
public class AdminDepartmentService {

    private final DepartmentService departmentService;

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getDepartments(String keyword, Pageable pageable) {
        return departmentService.getDepartments(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public DepartmentDetailResponse getDepartmentDetail(Long departmentId) {
        return departmentService.getDepartmentDetail(departmentId);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request) {
        return departmentService.updateDepartment(departmentId, request);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        departmentService.deleteDepartment(departmentId);
    }

    @Transactional
    public DepartmentResponse assignManager(Long departmentId, AssignManagerRequest request) {
        return departmentService.assignManager(departmentId, request);
    }

    @Transactional
    public DepartmentDetailResponse addMembers(Long departmentId, AddDepartmentMembersRequest request) {
        return departmentService.addMembers(departmentId, request);
    }

    @Transactional
    public DepartmentDetailResponse removeMember(Long departmentId, Long userId) {
        return departmentService.removeMember(departmentId, userId);
    }
}
