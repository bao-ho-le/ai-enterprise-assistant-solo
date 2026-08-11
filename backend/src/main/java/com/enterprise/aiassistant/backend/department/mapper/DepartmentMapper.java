package com.enterprise.aiassistant.backend.department.mapper;

import com.enterprise.aiassistant.backend.department.dto.request.CreateDepartmentRequest;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentDetailResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentMemberResponse;
import com.enterprise.aiassistant.backend.department.dto.response.DepartmentResponse;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DepartmentMapper {

    public Department toDepartment(CreateDepartmentRequest request) {

        return Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public DepartmentResponse toResponse(Department department, long memberCount) {

        return DepartmentResponse.builder()
                .departmentId(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .memberCount(memberCount)
                .createdAt(department.getCreatedAt())
                .build();
    }

    public DepartmentMemberResponse toMemberResponse(User user) {

        if (user == null) {
            return null;
        }

        return DepartmentMemberResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .build();
    }

    public DepartmentDetailResponse toDetailResponse(
            Department department,
            List<User> members,
            long documentCount,
            long aiRequestCount,
            long aiTotalTokens,
            BigDecimal aiEstimatedCost
    ) {

        return DepartmentDetailResponse.builder()
                .department(toResponse(department, members.size()))
                .manager(toMemberResponse(department.getManager()))
                .members(members.stream().map(this::toMemberResponse).toList())
                .documentCount(documentCount)
                .aiRequestCount(aiRequestCount)
                .aiTotalTokens(aiTotalTokens)
                .aiEstimatedCost(aiEstimatedCost)
                .build();
    }
}
