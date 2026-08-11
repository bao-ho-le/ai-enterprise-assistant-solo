package com.enterprise.aiassistant.backend.user.repository;

import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long userId);

    List<User> findByDepartmentIdOrderByFullNameAsc(Long departmentId);

    List<User> findByIdIn(List<Long> userIds);

    long countByDepartmentId(Long departmentId);

    long countByEnabled(boolean enabled);

    // Admin user list: keyword khớp username/email/fullName, các filter còn lại bỏ qua khi null.
    @Query("""
            SELECT u FROM User u
            WHERE (CAST(:keyword AS string) IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (CAST(:role AS string) IS NULL OR u.role = :role)
              AND (CAST(:departmentId AS string) IS NULL OR u.department.id = :departmentId)
              AND (CAST(:enabled AS string) IS NULL OR u.enabled = :enabled)
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("departmentId") Long departmentId,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
