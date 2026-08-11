import { apiClient } from "@/lib/apiClient";

// GET /departments -> Page<DepartmentResponse>  (read-only cho user thường)
export function getDepartments(params, signal) {
  return apiClient.get("/departments", { params, signal });
}

// GET /departments/me -> DepartmentDetailResponse
export function getMyDepartment(signal) {
  return apiClient.get("/departments/me", { signal });
}

// GET /departments/{departmentId} -> DepartmentDetailResponse
export function getDepartmentDetail(departmentId, signal) {
  return apiClient.get(`/departments/${departmentId}`, { signal });
}
