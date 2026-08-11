package com.enterprise.aiassistant.backend.department.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DepartmentResponse {

    private Long departmentId;

    private String name;

    private String description;

    private Long managerId;

    private String managerName;

    private long memberCount;

    private LocalDateTime createdAt;
}
