"use client";

import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Search, Trash2, UserMinus } from "lucide-react";
import Modal from "@/components/ui/Modal";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import ConfirmDialog from "@/features/admin/components/ConfirmDialog";
import Pagination from "@/features/document/components/Pagination";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission, roleBadgeClass } from "@/lib/permissions";
import {
  addAdminDepartmentMembers,
  assignAdminDepartmentManager,
  createAdminDepartment,
  deleteAdminDepartment,
  getAdminDepartmentDetail,
  getAdminDepartments,
  getAdminUsers,
  removeAdminDepartmentMember,
  updateAdminDepartment,
} from "@/services/adminService";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminDepartmentsPage() {
  const { user: currentUser } = useAuth();

  const [departments, setDepartments] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: "", description: "" });
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);

  const [candidates, setCandidates] = useState([]);
  const [memberToAdd, setMemberToAdd] = useState("");
  const [pendingAddMember, setPendingAddMember] = useState(null);
  const [pendingManagerChange, setPendingManagerChange] = useState(null);

  const canCreate = hasPermission(currentUser, "DEPARTMENT_CREATE");
  const canUpdate = hasPermission(currentUser, "DEPARTMENT_UPDATE");
  const canDelete = hasPermission(currentUser, "DEPARTMENT_DELETE");
  const canManageMembers = hasPermission(currentUser, "DEPARTMENT_MANAGE_MEMBERS");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminDepartments({ keyword: keyword.trim() || undefined, page, size: PAGE_SIZE })
      .then((data) => {
        setDepartments(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
        setTotalElements(data.totalElements ?? 0);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const openDetail = async (departmentId) => {
    setDetailLoading(true);
    try {
      const [data, users] = await Promise.all([
        getAdminDepartmentDetail(departmentId),
        getAdminUsers({ size: 200 }),
      ]);
      setDetail(data);
      setCandidates(users.content ?? []);
    } catch (e) {
      setToast({ type: "error", text: e.message });
    } finally {
      setDetailLoading(false);
    }
  };

  const runOnDetail = async (action, successText) => {
    try {
      const updated = await action();
      setDetail(updated);
      setToast({ type: "success", text: successText });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    }
  };

  const submitForm = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      if (editing) {
        await updateAdminDepartment(editing.departmentId, form);
      } else {
        await createAdminDepartment(form);
      }
      setFormOpen(false);
      setEditing(null);
      setForm({ name: "", description: "" });
      setToast({ type: "success", text: editing ? "Đã cập nhật" : "Đã tạo phòng ban" });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    } finally {
      setSaving(false);
    }
  };

  const memberOptions = candidates.filter(
    (candidate) => !detail?.members?.some((member) => member.userId === candidate.id)
  );

  return (
    <main className="flex flex-1 flex-col overflow-hidden mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      <div className="filter-toolbar mb-4 shrink-0">
        <div className="filter-toolbar-item filter-toolbar-item--search">
          <label className="label-text">Search</label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
            <input
              className="input-field"
              style={{ paddingLeft: "2.25rem" }}
              placeholder="Tên phòng ban…"
              value={keyword}
              onChange={(e) => {
                setPage(0);
                setKeyword(e.target.value);
              }}
            />
          </div>
        </div>
        {canCreate && (
          <div className="ml-auto">
            <button
              type="button"
              className="btn-primary"
              style={{ height: "2.25rem" }}
              onClick={() => {
                setEditing(null);
                setForm({ name: "", description: "" });
                setFormOpen(true);
              }}
            >
              <Plus className="h-4 w-4" />
              New Department
            </button>
          </div>
        )}
      </div>

      <div className="flex flex-1 min-h-0 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
        <div className="min-h-0 flex-1 overflow-auto">
          <table className="w-full min-w-[900px] border-collapse">
            <thead>
              <tr className="border-b border-border-default">
                {["Name", "Members", "Created", ""].map((h, i) => (
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
                colSpan={4}
                loading={loading}
                error={error}
                empty={!loading && !error && departments.length === 0}
                emptyText="Chưa có phòng ban nào"
                onRetry={load}
              />

              {!loading &&
                !error &&
                departments.map((d) => (
                  <tr
                    key={d.departmentId}
                    className="cursor-pointer border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                    onDoubleClick={() => openDetail(d.departmentId)}
                  >
                    <td className="px-4 py-2">
                      <span className="text-xs font-medium text-text-primary">{d.name}</span>
                      {d.description && (
                        <p className="mt-0.5 line-clamp-1 text-xs text-text-muted">{d.description}</p>
                      )}
                    </td>
                    <td className="px-4 py-2">
                      <span className="badge badge-neutral">{d.memberCount}</span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-2 text-xs text-text-secondary">
                      {formatDateTime(d.createdAt)}
                    </td>
                    <td
                      className="px-4 py-2 text-right"
                      onClick={(e) => e.stopPropagation()}
                      onDoubleClick={(e) => e.stopPropagation()}
                    >
                      {canUpdate && (
                        <button
                          type="button"
                          className="btn-ghost p-1.5"
                          aria-label={`Edit ${d.name}`}
                          onClick={() => {
                            setEditing(d);
                            setForm({ name: d.name, description: d.description ?? "" });
                            setFormOpen(true);
                          }}
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                      )}
                      {canDelete && (
                        <button
                          type="button"
                          className="btn-ghost p-1.5"
                          aria-label={`Delete ${d.name}`}
                          onClick={() => setPendingDelete(d)}
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
          shown={departments.length}
          onPrev={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
          itemLabel="departments"
          footerClassName="bg-bg-primary py-2"
        />
      </div>

      <Modal
        open={Boolean(detail) || detailLoading}
        onClose={() => setDetail(null)}
        title={detail?.department?.name ?? "Đang tải…"}
        maxWidth="max-w-2xl"
      >
        {detail && (
          <div className="flex flex-col gap-5">
            <div className="grid grid-cols-3 gap-3">
              {[
                { label: "Documents", value: detail.documentCount },
                { label: "AI requests", value: detail.aiRequestCount },
                { label: "AI tokens", value: detail.aiTotalTokens },
              ].map(({ label, value }) => (
                <div key={label} className="rounded-lg border border-border-subtle bg-bg-elevated p-3">
                  <p className="text-xs text-text-muted">{label}</p>
                  <p className="text-lg font-semibold text-text-primary">
                    {(value ?? 0).toLocaleString()}
                  </p>
                </div>
              ))}
            </div>

            <div>
              <label className="label-text">Manager</label>
              <select
                className="select-field"
                value={detail.manager?.userId ?? ""}
                disabled={!canUpdate}
                onChange={(e) => {
                  const value = e.target.value;
                  const selected = detail.members.find((m) => String(m.userId) === value);
                  setPendingManagerChange({
                    userId: value ? Number(value) : null,
                    fullName: selected?.fullName ?? null,
                  });
                }}
              >
                <option value="">—</option>
                {detail.members
                  .filter((member) => member.role === "MANAGER" || member.role === "ADMIN")
                  .map((member) => (
                    <option key={member.userId} value={member.userId}>
                      {member.fullName}
                    </option>
                  ))}
              </select>
              <p className="mt-1 text-xs text-text-muted">
                Chỉ thành viên có role MANAGER hoặc ADMIN mới được gán làm manager.
              </p>
            </div>

            {canManageMembers && (
              <div className="flex items-end gap-2">
                <div className="flex-1">
                  <label className="label-text">Thêm thành viên</label>
                  <select
                    className="select-field"
                    value={memberToAdd}
                    onChange={(e) => setMemberToAdd(e.target.value)}
                  >
                    <option value="">Chọn user…</option>
                    {memberOptions.map((candidate) => (
                      <option key={candidate.id} value={candidate.id}>
                        {candidate.fullName} ({candidate.role})
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  type="button"
                  className="btn-primary"
                  style={{ height: "2.25rem" }}
                  disabled={!memberToAdd}
                  onClick={() => {
                    const selected = memberOptions.find((c) => String(c.id) === memberToAdd);
                    setPendingAddMember({ userId: Number(memberToAdd), fullName: selected?.fullName ?? "" });
                  }}
                >
                  Thêm
                </button>
              </div>
            )}

            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-muted">
                Thành viên ({detail.members.length})
              </p>
              {detail.members.length === 0 ? (
                <p className="py-6 text-center text-sm text-text-muted">Chưa có thành viên</p>
              ) : (
                <ul className="flex max-h-64 flex-col gap-1 overflow-y-auto">
                  {detail.members.map((member) => (
                    <li
                      key={member.userId}
                      className="flex items-center justify-between rounded-lg border border-border-subtle px-3 py-2"
                    >
                      <div>
                        <p className="text-xs font-medium text-text-primary">{member.fullName}</p>
                        <p className="text-xs text-text-muted">{member.email}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`badge ${roleBadgeClass(member.role)}`}>{member.role}</span>
                        {canManageMembers && (
                          <button
                            type="button"
                            className="btn-ghost p-1.5"
                            aria-label={`Remove ${member.fullName}`}
                            onClick={() =>
                              runOnDetail(
                                () =>
                                  removeAdminDepartmentMember(
                                    detail.department.departmentId,
                                    member.userId
                                  ),
                                "Đã gỡ thành viên"
                              )
                            }
                          >
                            <UserMinus className="h-4 w-4 text-error" />
                          </button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </Modal>

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "Sửa phòng ban" : "Thêm phòng ban"}
        preventClose={saving}
      >
        <form className="flex flex-col gap-3" onSubmit={submitForm}>
          <div>
            <label className="label-text">Tên phòng ban</label>
            <input
              className="input-field"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </div>
          <div>
            <label className="label-text">Mô tả</label>
            <textarea
              className="textarea-field"
              rows={3}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <div className="mt-3 flex justify-end gap-2">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setFormOpen(false)}
              disabled={saving}
            >
              Huỷ
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu…" : "Lưu"}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Xoá phòng ban"
        message={`Xoá phòng ban "${pendingDelete?.name}"? Phòng ban phải không còn thành viên nào.`}
        confirmLabel="Xoá"
        onClose={() => setPendingDelete(null)}
        onConfirm={async () => {
          const target = pendingDelete;
          setPendingDelete(null);
          try {
            await deleteAdminDepartment(target.departmentId);
            setToast({ type: "success", text: "Đã xoá phòng ban" });
            load();
          } catch (e) {
            setToast({ type: "error", text: e.message });
          }
        }}
      />

      <ConfirmDialog
        open={Boolean(pendingAddMember)}
        title="Thêm thành viên"
        message={`Thêm "${pendingAddMember?.fullName}" vào phòng ban "${detail?.department?.name}"?`}
        confirmLabel="Thêm"
        onClose={() => setPendingAddMember(null)}
        onConfirm={async () => {
          const target = pendingAddMember;
          setPendingAddMember(null);
          await runOnDetail(async () => {
            const updated = await addAdminDepartmentMembers(detail.department.departmentId, [target.userId]);
            setMemberToAdd("");
            return updated;
          }, "Đã thêm thành viên");
        }}
      />

      <ConfirmDialog
        open={Boolean(pendingManagerChange)}
        title="Cập nhật manager"
        message={
          pendingManagerChange?.userId
            ? `Gán "${pendingManagerChange?.fullName}" làm manager của phòng ban "${detail?.department?.name}"?`
            : `Bỏ manager hiện tại của phòng ban "${detail?.department?.name}"?`
        }
        confirmLabel="Xác nhận"
        onClose={() => setPendingManagerChange(null)}
        onConfirm={async () => {
          const target = pendingManagerChange;
          setPendingManagerChange(null);
          await runOnDetail(async () => {
            await assignAdminDepartmentManager(detail.department.departmentId, target.userId);
            return getAdminDepartmentDetail(detail.department.departmentId);
          }, "Đã cập nhật manager");
        }}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
