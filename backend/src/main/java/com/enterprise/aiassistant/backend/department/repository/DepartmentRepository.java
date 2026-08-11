package com.enterprise.aiassistant.backend.department.repository;

import com.enterprise.aiassistant.backend.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long departmentId);

    @Query("""
            SELECT d FROM Department d
            WHERE CAST(:keyword AS string) IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
            ORDER BY d.name ASC
            """)
    Page<Department> searchByName(@Param("keyword") String keyword, Pageable pageable);
}
