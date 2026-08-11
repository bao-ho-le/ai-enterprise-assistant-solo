package com.enterprise.aiassistant.backend.ai.analytics.usage.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import org.springframework.stereotype.Component;

// Thu hẹp filter theo permission của người gọi. Client gửi userId/departmentId gì cũng bị ghi đè,
// nên không thể tự nới rộng phạm vi bằng cách sửa query string.
@Component
public class AIUsageScopeHelper {

    public void applyScope(AIUsageLogFilterRequest filter, UserPrincipal principal) {

        if (principal.hasPermission(Permission.AI_USAGE_READ_ALL)) {
            return;
        }

        if (principal.hasPermission(Permission.AI_USAGE_READ_DEPARTMENT)
                && principal.getDepartmentId() != null) {

            filter.setDepartmentId(principal.getDepartmentId());
            return;
        }

        if (principal.hasPermission(Permission.AI_USAGE_READ_SELF)) {
            filter.setUserId(principal.getId());
            filter.setDepartmentId(null);
            return;
        }

        throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
    }
}
