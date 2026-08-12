package com.enterprise.aiassistant.backend.department.controller;

import com.enterprise.aiassistant.backend.department.dto.response.DepartmentDetailResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentResponse;
import com.enterprise.aiassistant.backend.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// API cho user thường: xem department của mình và danh sách department (read-only).
// Mọi thao tác quản trị nằm ở /admin/departments.
@RestController
@RequestMapping("${api.prefix}/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<Page<DepartmentResponse>> getDepartments(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(departmentService.getDepartments(keyword, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<DepartmentDetailResponse> getMyDepartment() {
        return ResponseEntity.ok(departmentService.getMyDepartment());
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentDetailResponse> getDepartmentDetail(
            @PathVariable Long departmentId
    ) {
        return ResponseEntity.ok(departmentService.getDepartmentDetail(departmentId));
    }
}
