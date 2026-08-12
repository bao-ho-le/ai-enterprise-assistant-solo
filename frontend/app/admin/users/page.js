"use client";

import { useCallback, useEffect, useState } from "react";
import { Plus, Search, Trash2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import ConfirmDialog from "@/features/admin/components/ConfirmDialog";
import Pagination from "@/features/document/components/Pagination";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission, ROLES, roleBadgeClass } from "@/lib/permissions";
import {
  assignAdminUserRole,
  changeAdminUserDepartment,
  createAdminUser,
  deleteAdminUser,
  getAdminDepartments,
  getAdminUsers,
  setAdminUserEnabled,
} from "@/services/adminService";
import { formatDateTime, getInitials } from "@/utils/format";

const PAGE_SIZE = 20;

const EMPTY_FORM = {
  username: "",
  email: "",
  fullName: "",
  password: "",
  role: "EMPLOYEE",
  departmentId: "",
};

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth();

  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);

  const [keyword, setKeyword] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [role, setRole] = useState("");
  const [enabled, setEnabled] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);

  const canCreate = hasPermission(currentUser, "USER_CREATE");
  const canAssignRole = hasPermission(currentUser, "USER_ASSIGN_ROLE");
  const canChangeDepartment = hasPermission(currentUser, "USER_CHANGE_DEPARTMENT");
  const canDelete = hasPermission(currentUser, "USER_DELETE");
  const canToggle =
    hasPermission(currentUser, "USER_ENABLE") || hasPermission(currentUser, "USER_DISABLE");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminUsers({
      keyword: keyword.trim() || undefined,
      departmentId: departmentId || undefined,
      role: role || undefined,
      enabled: enabled === "" ? undefined : enabled,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        setUsers(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
        setTotalElements(data.totalElements ?? 0);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword, departmentId, role, enabled, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    getAdminDepartments({ size: 100 })
      .then((data) => setDepartments(data.content ?? []))
      .catch(() => setDepartments([]));
  }, []);

  const run = async (action, successText) => {
    try {
      await action();
      setToast({ type: "success", text: successText });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    }
  };

  const submitCreate = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await createAdminUser({
        ...form,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
      });
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      setToast({ type: "success", text: "Đã tạo user" });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="flex flex-1 flex-col overflow-hidden mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      <div className="filter-toolbar mb-4 shrink-0">
        <div className="filter-toolbar-item filter-toolbar-item--search">
          <label className="label-text">Search</label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
            {/* Inline style, not `pl-9`: .input-field is unlayered CSS in
                globals.css and its `padding` shorthand beats a Tailwind utility. */}
            <input
              className="input-field"
              style={{ paddingLeft: "2.25rem" }}
              placeholder="Tên, username hoặc email…"
              value={keyword}
              onChange={(e) => {
                setPage(0);
                setKeyword(e.target.value);
              }}
            />
          </div>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--auto">
          <label className="label-text">Department</label>
          <select
            className="select-field"
            value={departmentId}
            onChange={(e) => {
              setPage(0);
              setDepartmentId(e.target.value);
            }}
          >
            <option value="">All</option>
            {departments.map((d) => (
              <option key={d.departmentId} value={d.departmentId}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--auto">
          <label className="label-text">Role</label>
          <select
            className="select-field"
            value={role}
            onChange={(e) => {
              setPage(0);
              setRole(e.target.value);
            }}
          >
            <option value="">All</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--auto">
          <label className="label-text">Status</label>
          <select
            className="select-field"
            value={enabled}
            onChange={(e) => {
              setPage(0);
              setEnabled(e.target.value);
            }}
          >
            <option value="">All</option>
            <option value="true">Active</option>
            <option value="false">Disabled</option>
          </select>
        </div>
        {canCreate && (
          <div className="ml-auto">
            <button
              type="button"
              className="btn-primary"
              style={{ height: "2.25rem" }}
              onClick={() => setCreateOpen(true)}
            >
              <Plus className="h-4 w-4" />
              New User
            </button>
          </div>
        )}
      </div>

      <div className="flex flex-1 min-h-0 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
        <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full min-w-[1000px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {["Name", "Email", "Department", "Role", "Status", "Created", ""].map((h, i) => (
                <th
                  key={h || i}
                  className="sticky top-0 z-10 bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary shadow-[inset_0_-1px_0_var(--border-default)]"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <AdminTableState
              colSpan={7}
              loading={loading}
              error={error}
              empty={!loading && !error && users.length === 0}
              emptyText="No users found"
              onRetry={load}
            />

            {!loading &&
              !error &&
              users.map((u) => (
                <tr
                  key={u.id}
                  className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                >
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-bg-elevated text-[10px] font-medium text-text-secondary">
                        {getInitials(u.fullName)}
                      </span>
                      <span className="text-xs font-medium text-text-primary">{u.fullName}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">{u.email}</td>
                  <td className="w-48 px-4 py-2">
                    {canChangeDepartment ? (
                      <select
                        className="select-field"
                        value={u.departmentId ?? ""}
                        aria-label={`Department of ${u.fullName}`}
                        onChange={(e) =>
                          run(
                            () =>
                              changeAdminUserDepartment(
                                u.id,
                                e.target.value ? Number(e.target.value) : null
                              ),
                            "Đã đổi phòng ban"
                          )
                        }
                      >
                        <option value="">—</option>
                        {departments.map((d) => (
                          <option key={d.departmentId} value={d.departmentId}>
                            {d.name}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <span className="badge badge-neutral">{u.departmentName ?? "—"}</span>
                    )}
                  </td>
                  <td className="w-36 px-4 py-2">
                    {canAssignRole && u.id !== currentUser?.id ? (
                      <select
                        className="select-field"
                        value={u.role}
                        aria-label={`Role of ${u.fullName}`}
                        onChange={(e) =>
                          run(() => assignAdminUserRole(u.id, e.target.value), "Đã đổi role")
                        }
                      >
                        {ROLES.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <span className={`badge ${roleBadgeClass(u.role)}`}>{u.role}</span>
                    )}
                  </td>
                  <td className="px-4 py-2">
                    {canToggle && u.id !== currentUser?.id ? (
                      <button
                        type="button"
                        className={`badge ${u.enabled ? "badge-success" : "badge-error"}`}
                        onClick={() =>
                          run(
                            () => setAdminUserEnabled(u.id, !u.enabled),
                            u.enabled ? "Đã khoá user" : "Đã mở khoá user"
                          )
                        }
                      >
                        {u.enabled ? "ACTIVE" : "DISABLED"}
                      </button>
                    ) : (
                      <span className={`badge ${u.enabled ? "badge-success" : "badge-error"}`}>
                        {u.enabled ? "ACTIVE" : "DISABLED"}
                      </span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-2 text-xs text-text-secondary">
                    {formatDateTime(u.createdAt)}
                  </td>
                  <td className="px-4 py-2 text-right">
                    {canDelete && u.id !== currentUser?.id && (
                      <button
                        type="button"
                        className="btn-ghost p-1.5"
                        aria-label={`Delete ${u.fullName}`}
                        onClick={() => setPendingDelete(u)}
                      >
                        <Trash2 className="h-4 w-4 text-error" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        </div>
        <Pagination
          number={page}
          totalPages={totalPages}
          totalElements={totalElements}
          shown={users.length}
          onPrev={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
          itemLabel="users"
          footerClassName="bg-bg-primary py-2"
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Thêm user" preventClose={saving}>
        <form className="flex flex-col gap-3" onSubmit={submitCreate}>
          {[
            { key: "username", label: "Username", type: "text" },
            { key: "fullName", label: "Họ tên", type: "text" },
            { key: "email", label: "Email", type: "email" },
            { key: "password", label: "Mật khẩu (tối thiểu 8 ký tự)", type: "password" },
          ].map(({ key, label, type }) => (
            <div key={key}>
              <label className="label-text">{label}</label>
              <input
                className="input-field"
                type={type}
                required
                value={form[key]}
                onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
              />
            </div>
          ))}
          <div>
            <label className="label-text">Role</label>
            <select
              className="select-field"
              value={form.role}
              onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label-text">Department</label>
            <select
              className="select-field"
              value={form.departmentId}
              onChange={(e) => setForm((f) => ({ ...f, departmentId: e.target.value }))}
            >
              <option value="">—</option>
              {departments.map((d) => (
                <option key={d.departmentId} value={d.departmentId}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
          <div className="mt-3 flex justify-end gap-2">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setCreateOpen(false)}
              disabled={saving}
            >
              Huỷ
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu…" : "Tạo user"}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Xoá user"
        message={`Xoá vĩnh viễn user "${pendingDelete?.fullName}"? Hành động này không thể hoàn tác.`}
        confirmLabel="Xoá"
        onClose={() => setPendingDelete(null)}
        onConfirm={async () => {
          const target = pendingDelete;
          setPendingDelete(null);
          await run(() => deleteAdminUser(target.id), "Đã xoá user");
        }}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
