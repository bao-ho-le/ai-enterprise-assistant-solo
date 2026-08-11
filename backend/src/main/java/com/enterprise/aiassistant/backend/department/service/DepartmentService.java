package com.enterprise.aiassistant.backend.department.service;

import com.enterprise.aiassistant.backend.department.dto.request.AddDepartmentMembersRequest;
import com.enterprise.aiassistant.backend.department.dto.request.AssignManagerRequest;
import com.enterprise.aiassistant.backend.department.dto.request.CreateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.request.UpdateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentDetailResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentResponse;
import com.enterprise.aiassistant.backend.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {

    Page<DepartmentResponse> getDepartments(String keyword, Pageable pageable);

    DepartmentDetailResponse getDepartmentDetail(Long departmentId);

    // Department của chính người đang đăng nhập.
    DepartmentDetailResponse getMyDepartment();

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request);

    void deleteDepartment(Long departmentId);

    DepartmentResponse assignManager(Long departmentId, AssignManagerRequest request);

    DepartmentDetailResponse addMembers(Long departmentId, AddDepartmentMembersRequest request);

    DepartmentDetailResponse removeMember(Long departmentId, Long userId);

    // Cho module khác lấy entity đã validate tồn tại, tránh mỗi nơi tự findById + throw.
    Department getDepartmentOrThrow(Long departmentId);
}
