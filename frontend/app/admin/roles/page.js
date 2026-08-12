"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Loader2, ShieldCheck } from "lucide-react";
import Toast from "@/components/ui/Toast";
import { roleBadgeClass } from "@/lib/permissions";
import {
  getAdminPermissionCatalog,
  getAdminRoles,
  updateAdminRolePermissions,
} from "@/services/adminService";

// Nhóm permission theo tiền tố để bảng checkbox đọc được, không cần cấu hình thêm.
function groupOf(permission) {
  if (permission.startsWith("AI_USAGE_")) return "AI Usage";
  if (permission.startsWith("AI_")) return "AI";
  return permission.split("_")[0];
}

export default function AdminRolesPage() {
  const [roles, setRoles] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [selectedRole, setSelectedRole] = useState(null);
  const [draft, setDraft] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    Promise.all([getAdminRoles(), getAdminPermissionCatalog()])
      .then(([roleData, catalogData]) => {
        setRoles(roleData);
        setCatalog(catalogData);
        setSelectedRole((current) => current ?? roleData[0]?.role ?? null);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  useEffect(() => {
    const found = roles.find((r) => r.role === selectedRole);
    setDraft(found?.permissions ?? []);
  }, [roles, selectedRole]);

  const toggle = (permission) =>
    setDraft((current) =>
      current.includes(permission)
        ? current.filter((p) => p !== permission)
        : [...current, permission]
    );

  const save = async () => {
    setSaving(true);
    try {
      await updateAdminRolePermissions(selectedRole, draft);
      setToast({ type: "success", text: "Đã cập nhật permission" });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    } finally {
      setSaving(false);
    }
  };

  const groups = catalog.reduce((acc, permission) => {
    const group = groupOf(permission);
    (acc[group] ??= []).push(permission);
    return acc;
  }, {});

  const current = roles.find((r) => r.role === selectedRole);
  const dirty =
    current &&
    (draft.length !== current.permissions.length ||
      draft.some((p) => !current.permissions.includes(p)));

  return (
    <main className="flex-1 overflow-y-auto mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      {loading && (
        <div className="flex items-center justify-center py-24">
          <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
        </div>
      )}

      {!loading && error && (
        <div className="card flex flex-col items-center gap-3 p-10 text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-error">{error}</p>
          <button type="button" className="btn-secondary" onClick={load}>
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[16rem_1fr]">
          <aside className="flex flex-col gap-2 self-start lg:sticky lg:top-0">
            {roles.map((role) => (
              <button
                key={role.role}
                type="button"
                onClick={() => setSelectedRole(role.role)}
                className={`flex items-center justify-between rounded-lg border px-3 py-2.5 text-left transition-colors ${
                  selectedRole === role.role
                    ? "border-accent bg-bg-elevated"
                    : "border-border-subtle hover:bg-bg-elevated"
                }`}
              >
                <span className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-text-muted" />
                  <span className={`badge ${roleBadgeClass(role.role)}`}>{role.role}</span>
                </span>
                <span className="text-xs text-text-muted">{role.permissions.length}</span>
              </button>
            ))}
          </aside>

          <section className="card p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-text-primary">
                Permissions của {selectedRole}
              </h2>
              {selectedRole !== "ADMIN" && (
                <button type="button" className="btn-primary" disabled={!dirty || saving} onClick={save}>
                  {saving ? "Đang lưu…" : "Lưu thay đổi"}
                </button>
              )}
            </div>

            {selectedRole === "ADMIN" && (
              <p className="mb-4 rounded-lg border border-border-subtle bg-bg-elevated px-3 py-2 text-xs text-text-muted">
                Role ADMIN luôn phải giữ đủ permission — backend sẽ từ chối nếu bị thu hồi.
              </p>
            )}

            <div className="flex flex-col gap-5">
              {Object.entries(groups).map(([group, permissions]) => (
                <div key={group}>
                  <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-muted">
                    {group}
                  </p>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                    {permissions.map((permission) => (
                      <label
                        key={permission}
                        className="flex cursor-pointer items-center gap-2 rounded-lg border border-border-subtle px-3 py-2 text-xs text-text-secondary hover:bg-bg-elevated"
                      >
                        <input
                          type="checkbox"
                          checked={draft.includes(permission)}
                          onChange={() => toggle(permission)}
                        />
                        {permission}
                      </label>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
