"use client";

import { useCallback, useEffect, useState } from "react";
import { Search, ShieldOff } from "lucide-react";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import ConfirmDialog from "@/features/admin/components/ConfirmDialog";
import Pagination from "@/features/document/components/Pagination";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission } from "@/lib/permissions";
import {
  getAdminDepartments,
  getAdminSharedDocuments,
  getAdminUsers,
  revokeAdminDocumentAccess,
} from "@/services/adminService";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminSharedDocumentsPage() {
  const { user: currentUser } = useAuth();

  const [shares, setShares] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [users, setUsers] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);

  const [keyword, setKeyword] = useState("");
  const [ownerId, setOwnerId] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [sharedUserId, setSharedUserId] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);
  const [pendingRevoke, setPendingRevoke] = useState(null);

  const canRevoke = hasPermission(currentUser, "DOCUMENT_MANAGE_ACCESS");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminSharedDocuments({
      keyword: keyword.trim() || undefined,
      ownerId: ownerId || undefined,
      departmentId: departmentId || undefined,
      sharedUserId: sharedUserId || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        setShares(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
        setTotalElements(data.totalElements ?? 0);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword, ownerId, departmentId, sharedUserId, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    getAdminDepartments({ size: 100 })
      .then((data) => setDepartments(data.content ?? []))
      .catch(() => setDepartments([]));
    getAdminUsers({ size: 200 })
      .then((data) => setUsers(data.content ?? []))
      .catch(() => setUsers([]));
  }, []);

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
              placeholder="Tên tài liệu…"
              value={keyword}
              onChange={(e) => {
                setPage(0);
                setKeyword(e.target.value);
              }}
            />
          </div>
        </div>
        {[
          {
            label: "Owner",
            value: ownerId,
            setValue: setOwnerId,
            options: users.map((u) => ({ id: u.id, name: u.fullName })),
          },
          {
            label: "Department",
            value: departmentId,
            setValue: setDepartmentId,
            options: departments.map((d) => ({ id: d.departmentId, name: d.name })),
          },
          {
            label: "Shared with",
            value: sharedUserId,
            setValue: setSharedUserId,
            options: users.map((u) => ({ id: u.id, name: u.fullName })),
          },
        ].map(({ label, value, setValue, options }) => (
          <div key={label} className="filter-toolbar-item filter-toolbar-item--auto">
            <label className="label-text">{label}</label>
            <select
              className="select-field"
              value={value}
              onChange={(e) => {
                setPage(0);
                setValue(e.target.value);
              }}
            >
              <option value="">All</option>
              {options.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.name}
                </option>
              ))}
            </select>
          </div>
        ))}
      </div>

      <div className="flex flex-1 min-h-0 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
        <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full min-w-[1000px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {["Document", "Owner", "Department", "Shared With", "Shared By", "Shared At", ""].map(
                (h, i) => (
                  <th
                    key={h || i}
                    className="sticky top-0 z-10 bg-bg-primary whitespace-nowrap px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary shadow-[inset_0_-1px_0_var(--border-default)]"
                  >
                    {h}
                  </th>
                )
              )}
            </tr>
          </thead>
          <tbody>
            <AdminTableState
              colSpan={7}
              loading={loading}
              error={error}
              empty={!loading && !error && shares.length === 0}
              emptyText="Chưa có tài liệu nào được chia sẻ"
              onRetry={load}
            />

            {!loading &&
              !error &&
              shares.map((share) => (
                <tr
                  key={share.documentAccessId}
                  className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                >
                  <td className="px-4 py-2 text-xs font-medium text-text-primary">
                    {share.documentTitle}
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">{share.ownerName ?? "—"}</td>
                  <td className="px-4 py-2">
                    <span className="badge badge-neutral">{share.departmentName ?? "—"}</span>
                  </td>
                  <td className="px-4 py-2">
                    <p className="text-xs font-medium text-text-primary">{share.sharedUserName}</p>
                    <p className="text-xs text-text-muted">{share.sharedUserEmail}</p>
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">
                    {share.grantedByName ?? "—"}
                  </td>
                  <td className="whitespace-nowrap px-4 py-2 text-xs text-text-secondary">
                    {formatDateTime(share.sharedAt)}
                  </td>
                  <td className="px-4 py-2 text-right">
                    {canRevoke && (
                      <button
                        type="button"
                        className="btn-ghost p-1.5"
                        aria-label={`Revoke access of ${share.sharedUserName}`}
                        onClick={() => setPendingRevoke(share)}
                      >
                        <ShieldOff className="h-4 w-4 text-error" />
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
          shown={shares.length}
          onPrev={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
          itemLabel="shares"
          footerClassName="bg-bg-primary py-2"
        />
      </div>

      <ConfirmDialog
        open={Boolean(pendingRevoke)}
        title="Thu hồi quyền truy cập"
        message={`Thu hồi quyền đọc "${pendingRevoke?.documentTitle}" của ${pendingRevoke?.sharedUserName}?`}
        confirmLabel="Thu hồi"
        onClose={() => setPendingRevoke(null)}
        onConfirm={async () => {
          const target = pendingRevoke;
          setPendingRevoke(null);
          try {
            await revokeAdminDocumentAccess(target.documentId, target.sharedUserId);
            setToast({ type: "success", text: "Đã thu hồi quyền truy cập" });
            load();
          } catch (e) {
            setToast({ type: "error", text: e.message });
          }
        }}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
