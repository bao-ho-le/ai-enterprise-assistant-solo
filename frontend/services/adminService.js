import { apiClient } from "@/lib/apiClient";

// Mọi endpoint /admin/** đều bị backend chặn ROLE_ADMIN — frontend chỉ ẩn UI, không phải hàng rào.

// ===================== Users =====================

// GET /admin/users -> Page<UserResponse>
export function getAdminUsers(params, signal) {
  return apiClient.get("/admin/users", { params, signal });
}

export function getAdminUser(userId, signal) {
  return apiClient.get(`/admin/users/${userId}`, { signal });
}

export function createAdminUser(data) {
  return apiClient.postJson("/admin/users", data);
}

export function updateAdminUser(userId, data) {
  return apiClient.putJson(`/admin/users/${userId}`, data);
}

export function setAdminUserEnabled(userId, enabled) {
  return apiClient.putJson(`/admin/users/${userId}/${enabled ? "enable" : "disable"}`, {});
}

export function assignAdminUserRole(userId, role) {
  return apiClient.putJson(`/admin/users/${userId}/role`, { role });
}

export function changeAdminUserDepartment(userId, departmentId) {
  return apiClient.putJson(`/admin/users/${userId}/department`, { departmentId });
}

export function deleteAdminUser(userId) {
  return apiClient.del(`/admin/users/${userId}`);
}

// ===================== Departments =====================

export function getAdminDepartments(params, signal) {
  return apiClient.get("/admin/departments", { params, signal });
}

export function getAdminDepartmentDetail(departmentId, signal) {
  return apiClient.get(`/admin/departments/${departmentId}`, { signal });
}

export function createAdminDepartment(data) {
  return apiClient.postJson("/admin/departments", data);
}

export function updateAdminDepartment(departmentId, data) {
  return apiClient.putJson(`/admin/departments/${departmentId}`, data);
}

export function deleteAdminDepartment(departmentId) {
  return apiClient.del(`/admin/departments/${departmentId}`);
}

export function assignAdminDepartmentManager(departmentId, managerId) {
  return apiClient.putJson(`/admin/departments/${departmentId}/manager`, { managerId });
}

export function addAdminDepartmentMembers(departmentId, userIds) {
  return apiClient.postJson(`/admin/departments/${departmentId}/members`, { userIds });
}

export function removeAdminDepartmentMember(departmentId, userId) {
  return apiClient.del(`/admin/departments/${departmentId}/members/${userId}`);
}

// ===================== Documents =====================

export function getAdminDocuments(params, signal) {
  return apiClient.get("/admin/documents", { params, signal });
}

export function getAdminDocumentDetail(documentId, signal) {
  return apiClient.get(`/admin/documents/${documentId}`, { signal });
}

export function getAdminDocumentShares(documentId, signal) {
  return apiClient.get(`/admin/documents/${documentId}/shares`, { signal });
}

export function deleteAdminDocument(documentId) {
  return apiClient.del(`/admin/documents/${documentId}`);
}

export function restoreAdminDocument(documentId) {
  return apiClient.postJson(`/admin/documents/${documentId}/restore`, {});
}

export function downloadAdminDocument(documentId, versionId) {
  return apiClient.getRaw(`/admin/documents/${documentId}/${versionId}/download`);
}

// ===================== Shared documents =====================

export function getAdminSharedDocuments(params, signal) {
  return apiClient.get("/admin/shared-documents", { params, signal });
}

export function revokeAdminDocumentAccess(documentId, targetUserId) {
  return apiClient.del(`/admin/shared-documents/${documentId}/users/${targetUserId}`);
}

// ===================== Roles & permissions =====================

export function getAdminRoles(signal) {
  return apiClient.get("/admin/roles", { signal });
}

export function getAdminPermissionCatalog(signal) {
  return apiClient.get("/admin/roles/permissions", { signal });
}

export function updateAdminRolePermissions(role, permissions) {
  return apiClient.putJson(`/admin/roles/${role}/permissions`, { permissions });
}

// ===================== AI usage =====================

export function getAdminUsageOverview(params, signal) {
  return apiClient.get("/admin/ai-usage/overview", { params, signal });
}

export function getAdminUsageLogs(params, signal) {
  return apiClient.get("/admin/ai-usage", { params, signal });
}

// ===================== Trash =====================

export function getAdminTrash(params, signal) {
  return apiClient.get("/admin/trash", { params, signal });
}

export function restoreTrashDocument(documentId) {
  return apiClient.postJson(`/admin/trash/documents/${documentId}/restore`, {});
}

export function restoreTrashFolder(folderId) {
  return apiClient.postJson(`/admin/trash/folders/${folderId}/restore`, {});
}

export function permanentlyDeleteTrashFolder(folderId) {
  return apiClient.del(`/admin/trash/folders/${folderId}`);
}
