"use client";

import { useCallback, useEffect, useState } from "react";
import { Download, Search, Share2, Trash2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import ConfirmDialog from "@/features/admin/components/ConfirmDialog";
import Pagination from "@/features/document/components/Pagination";
import { documentTypeLabel, extensionIcon, versionStatusBadge } from "@/constants/document";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission } from "@/lib/permissions";
import {
  deleteAdminDocument,
  downloadAdminDocument,
  getAdminDepartments,
  getAdminDocumentDetail,
  getAdminDocumentShares,
  getAdminDocuments,
  getAdminUsers,
} from "@/services/adminService";
import { formatBytes, formatDateTime, saveBlob } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminDocumentsPage() {
  const { user: currentUser } = useAuth();

  const [documents, setDocuments] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [owners, setOwners] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);

  const [keyword, setKeyword] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [ownerId, setOwnerId] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const [shares, setShares] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);

  const canDelete = hasPermission(currentUser, "DOCUMENT_DELETE");
  const canDownload = hasPermission(currentUser, "DOCUMENT_DOWNLOAD");
  const canManageAccess = hasPermission(currentUser, "DOCUMENT_MANAGE_ACCESS");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminDocuments({
      keyword: keyword.trim() || undefined,
      departmentId: departmentId || undefined,
      ownerId: ownerId || undefined,
      documentStatus: "ACTIVE",
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        setDocuments(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
        setTotalElements(data.totalElements ?? 0);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword, departmentId, ownerId, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    getAdminDepartments({ size: 100 })
      .then((data) => setDepartments(data.content ?? []))
      .catch(() => setDepartments([]));
    getAdminUsers({ size: 200 })
      .then((data) => setOwners(data.content ?? []))
      .catch(() => setOwners([]));
  }, []);

  const openShares = async (documentId) => {
    try {
      setShares({ documentId, items: null });
      setShares({ documentId, items: await getAdminDocumentShares(documentId) });
    } catch (e) {
      setShares(null);
      setToast({ type: "error", text: e.message });
    }
  };

  const download = async (documentId) => {
    try {
      const detail = await getAdminDocumentDetail(documentId);
      const res = await downloadAdminDocument(documentId, detail.currentVersion.versionId);
      saveBlob(await res.blob(), detail.currentVersion.fileName);
    } catch (e) {
      setToast({ type: "error", text: e.message });
    }
  };

  const run = async (action, successText) => {
    try {
      await action();
      setToast({ type: "success", text: successText });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    }
  };

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
              placeholder="Document name…"
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
          <label className="label-text">Owner</label>
          <select
            className="select-field"
            value={ownerId}
            onChange={(e) => {
              setPage(0);
              setOwnerId(e.target.value);
            }}
          >
            <option value="">All</option>
            {owners.map((o) => (
              <option key={o.id} value={o.id}>
                {o.fullName}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex flex-1 min-h-0 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
        <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full min-w-[1100px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {[
                "File Name",
                "Owner",
                "Department",
                "Type",
                "Size",
                "Uploaded",
                "Processing",
                "Actions",
              ].map((h, i, arr) => (
                <th
                  key={h || i}
                  className={`sticky top-0 z-10 bg-bg-primary whitespace-nowrap px-4 py-3 text-xs font-medium uppercase tracking-wider text-text-primary ${
                    i === arr.length - 1 ? "text-right" : "text-left"
                  }`}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <AdminTableState
              colSpan={8}
              loading={loading}
              error={error}
              empty={!loading && !error && documents.length === 0}
              emptyText="No documents found"
              onRetry={load}
            />

            {!loading &&
              !error &&
              documents.map((doc) => {
                const { Icon: ExtIcon, bg, color } = extensionIcon(doc.extension);
                const processing = versionStatusBadge(doc.versionStatus);

                return (
                  <tr
                    key={doc.id}
                    className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                  >
                    <td className="w-full px-4 py-1">
                      <div className="flex items-center gap-3">
                        <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${bg}`}>
                          <ExtIcon className={`h-3.5 w-3.5 ${color}`} />
                        </span>
                        <div className="min-w-0">
                          <span className="block truncate text-xs font-medium text-text-primary" title={doc.title}>
                            {doc.title}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-1 text-xs text-text-secondary">
                      {doc.ownerName ?? "—"}
                    </td>
                    <td className="px-4 py-1">
                      <span className="badge badge-neutral">{doc.departmentName ?? "—"}</span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-1 text-xs uppercase text-text-secondary">
                      {documentTypeLabel(doc.documentType)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-1 text-xs text-text-secondary">
                      {formatBytes(doc.size)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-1 text-xs text-text-secondary">
                      {formatDateTime(doc.uploadTime)}
                    </td>
                    <td className="px-4 py-1">
                      <span className={`badge ${processing.badge}`}>{processing.label}</span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-1 text-right">
                      {canManageAccess && (
                        <button
                          type="button"
                          className="btn-ghost p-1.5"
                          aria-label={`Shares of ${doc.title}`}
                          onClick={() => openShares(doc.id)}
                        >
                          <Share2 className="h-4 w-4" />
                        </button>
                      )}
                      {canDownload && (
                        <button
                          type="button"
                          className="btn-ghost p-1.5"
                          aria-label={`Download ${doc.title}`}
                          onClick={() => download(doc.id)}
                        >
                          <Download className="h-4 w-4" />
                        </button>
                      )}
                      {canDelete && (
                        <button
                          type="button"
                          className="btn-ghost p-1.5"
                          aria-label={`Delete ${doc.title}`}
                          onClick={() => setPendingDelete(doc)}
                        >
                          <Trash2 className="h-4 w-4 text-error" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
        </div>
        <Pagination
          number={page}
          totalPages={totalPages}
          totalElements={totalElements}
          shown={documents.length}
          onPrev={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
          footerClassName="bg-bg-primary py-2"
        />
      </div>

      <Modal open={Boolean(shares)} onClose={() => setShares(null)} title="Shared with">
        {shares?.items === null && <p className="text-sm text-text-muted">Loading…</p>}
        {shares?.items?.length === 0 && (
          <p className="py-6 text-center text-sm text-text-muted">This document hasn&apos;t been shared.</p>
        )}
        {shares?.items?.length > 0 && (
          <ul className="flex flex-col gap-1">
            {shares.items.map((share) => (
              <li
                key={share.documentAccessId}
                className="flex items-center justify-between rounded-lg border border-border-subtle px-3 py-2"
              >
                <div>
                  <p className="text-xs font-medium text-text-primary">{share.sharedUserName}</p>
                  <p className="text-xs text-text-muted">{share.sharedUserEmail}</p>
                </div>
                <span className="text-xs text-text-muted">{formatDateTime(share.sharedAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete document"
        message={`Move "${pendingDelete?.title}" to trash? The original file is kept and can be restored.`}
        confirmLabel="Delete"
        onClose={() => setPendingDelete(null)}
        onConfirm={async () => {
          const target = pendingDelete;
          setPendingDelete(null);
          await run(() => deleteAdminDocument(target.id), "Moved to trash");
        }}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
