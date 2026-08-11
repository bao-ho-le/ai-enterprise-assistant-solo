package com.enterprise.aiassistant.backend.admin.department.controller;

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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Chỉ expose use-case admin, logic nghiệp vụ vẫn nằm trong DepartmentService của module department/.
@RestController
@RequestMapping("${api.prefix}/admin/departments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public Page<DepartmentResponse> getDepartments(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return departmentService.getDepartments(keyword, pageable);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentDetailResponse> getDepartmentDetail(@PathVariable Long departmentId) {
        return ResponseEntity.ok(departmentService.getDepartmentDetail(departmentId));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @RequestBody CreateDepartmentRequest request
    ) {
        return ResponseEntity.ok(departmentService.createDepartment(request));
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody UpdateDepartmentRequest request
    ) {
        return ResponseEntity.ok(departmentService.updateDepartment(departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{departmentId}/manager")
    public ResponseEntity<DepartmentResponse> assignManager(
            @PathVariable Long departmentId,
            @RequestBody AssignManagerRequest request
    ) {
        return ResponseEntity.ok(departmentService.assignManager(departmentId, request));
    }

    @PostMapping("/{departmentId}/members")
    public ResponseEntity<DepartmentDetailResponse> addMembers(
            @PathVariable Long departmentId,
            @RequestBody AddDepartmentMembersRequest request
    ) {
        return ResponseEntity.ok(departmentService.addMembers(departmentId, request));
    }

    @DeleteMapping("/{departmentId}/members/{userId}")
    public ResponseEntity<DepartmentDetailResponse> removeMember(
            @PathVariable Long departmentId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(departmentService.removeMember(departmentId, userId));
    }
}
