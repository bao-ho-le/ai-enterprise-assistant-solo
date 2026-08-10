package com.enterprise.aiassistant.backend.ai.usage.dto.request;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AIUsageLogFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    private ConversationType conversationType;

    @Size(max = 100, message = "Tên model không được vượt quá 100 ký tự")
    private String model;

    private AIUsageStatus status;


    @AssertTrue(message = "fromDate phải nhỏ hơn hoặc bằng toDate")
    public boolean isValidDateRange() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
