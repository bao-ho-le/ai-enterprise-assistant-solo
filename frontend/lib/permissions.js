// Permission-aware UI. Backend vẫn enforce lại toàn bộ ở service layer — đây chỉ để ẩn/hiện.

export const ROLES = ["ADMIN", "MANAGER", "EMPLOYEE", "SUPERVISOR"];

export function hasPermission(user, permission) {
  return Boolean(user?.permissions?.includes(permission));
}

export function hasAnyPermission(user, ...permissions) {
  return permissions.some((permission) => hasPermission(user, permission));
}

export function isAdmin(user) {
  return user?.role === "ADMIN";
}

export function roleBadgeClass(role) {
  if (role === "ADMIN") return "badge-error";
  if (role === "MANAGER") return "badge-warning";
  if (role === "SUPERVISOR") return "badge-neutral";
  return "badge-success";
}
