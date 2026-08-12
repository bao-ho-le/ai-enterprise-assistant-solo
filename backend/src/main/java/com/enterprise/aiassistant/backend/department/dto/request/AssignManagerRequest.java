package com.enterprise.aiassistant.backend.department.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignManagerRequest {

    // null nghĩa là gỡ manager hiện tại khỏi department.
    private Long managerId;
}
