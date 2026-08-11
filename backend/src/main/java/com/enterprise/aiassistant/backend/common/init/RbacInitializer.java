package com.enterprise.aiassistant.backend.common.init;

import com.enterprise.aiassistant.backend.user.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Chuẩn hoá dữ liệu cũ + seed role/permission. Chạy trước mọi initializer khác vì
// role cũ ("USER") sẽ làm Hibernate nổ khi map enum Role mới.
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RbacInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private final RolePermissionService rolePermissionService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        migrateLegacyRoles();

        rolePermissionService.seedDefaultRolePermissionsIfMissing();

        backfillConversationOwners();
    }

    // Role cũ USER (và mọi giá trị không còn nằm trong enum) được ánh xạ về EMPLOYEE.
    private void migrateLegacyRoles() {

        // ddl-auto=update không tự sửa lại check constraint cũ khi enum Role đổi,
        // nên constraint có thể còn thiếu giá trị mới (vd EMPLOYEE) và chặn UPDATE bên dưới.
        // Drop rồi tạo lại theo đúng enum hiện tại trước khi migrate dữ liệu.
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");

        int updated = jdbcTemplate.update("""
                UPDATE users
                SET role = 'EMPLOYEE'
                WHERE role NOT IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'SUPERVISOR')
                """);

        if (updated > 0) {
            log.info("Migrated {} legacy user role(s) to EMPLOYEE", updated);
        }

        jdbcTemplate.execute("""
                ALTER TABLE users ADD CONSTRAINT users_role_check
                CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'SUPERVISOR'))
                """);
    }

    // Conversation tạo trước khi có ownership sẽ vô chủ và không ai đọc được nữa,
    // nên gán về admin đầu tiên (fallback: user cũ nhất) để dữ liệu cũ không bị mất.
    private void backfillConversationOwners() {

        Long fallbackOwnerId = jdbcTemplate.query(
                """
                        SELECT id FROM users
                        ORDER BY CASE WHEN role = 'ADMIN' THEN 0 ELSE 1 END, id
                        LIMIT 1
                        """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null
        );

        if (fallbackOwnerId == null) {
            return;
        }

        int updated = jdbcTemplate.update(
                "UPDATE ai_conversations SET user_id = ? WHERE user_id IS NULL",
                fallbackOwnerId
        );

        if (updated > 0) {
            log.info("Backfilled owner for {} legacy conversation(s)", updated);
        }
    }
}
