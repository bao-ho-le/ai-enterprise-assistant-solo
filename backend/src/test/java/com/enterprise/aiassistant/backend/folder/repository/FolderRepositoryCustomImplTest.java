package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Cùng logic ABAC với DocumentRepositoryCustomImpl (list document trong 1 folder),
// test thẳng appendAccessScope() không cần EntityManager.
class FolderRepositoryCustomImplTest {

    private final FolderRepositoryCustomImpl repository = new FolderRepositoryCustomImpl();

    @Test
    void nullScopeMustNotBypassAuthorization() {

        StringBuilder jpql = new StringBuilder("SELECT d FROM Document d WHERE d.folder.id = :folderId");

        assertThrows(NullPointerException.class,
                () -> repository.appendAccessScope(jpql, new HashMap<>(), null));
    }

    @Test
    void adminOrSupervisorUnrestrictedScopeSkipsFiltering() {

        DocumentAccessScope unrestricted = new DocumentAccessScope(true, 1L, null, List.of());

        StringBuilder jpql = new StringBuilder("SELECT d FROM Document d WHERE d.folder.id = :folderId");
        Map<String, Object> params = new HashMap<>();

        repository.appendAccessScope(jpql, params, unrestricted);

        assertEquals("SELECT d FROM Document d WHERE d.folder.id = :folderId", jpql.toString());
        assertTrue(params.isEmpty());
    }

    @Test
    void employeeOrManagerRestrictedScopeAppliesAbacFilter() {

        DocumentAccessScope restricted = new DocumentAccessScope(
                false, 10L, 2L, List.of(101L));

        StringBuilder jpql = new StringBuilder("SELECT d FROM Document d WHERE d.folder.id = :folderId");
        Map<String, Object> params = new HashMap<>();

        repository.appendAccessScope(jpql, params, restricted);

        String sql = jpql.toString();
        assertTrue(sql.contains("d.owner.id = :scopeUserId"));
        assertTrue(sql.contains("d.department.id = :scopeDepartmentId"));
        assertTrue(sql.contains("d.id IN :scopeSharedDocumentIds"));

        assertEquals(10L, params.get("scopeUserId"));
        assertEquals(2L, params.get("scopeDepartmentId"));
        assertEquals(List.of(101L), params.get("scopeSharedDocumentIds"));
    }
}
