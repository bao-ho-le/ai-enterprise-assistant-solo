"use client";

import { useCallback, useEffect, useState } from "react";
import { FileText, FolderIcon, RotateCcw, Search, Trash2 } from "lucide-react";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import ConfirmDialog from "@/features/admin/components/ConfirmDialog";
import {
  getAdminTrash,
  permanentlyDeleteTrashFolder,
  restoreTrashDocument,
  restoreTrashFolder,
} from "@/services/adminService";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminTrashPage() {
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [type, setType] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);
  const [pendingPurge, setPendingPurge] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminTrash({
      keyword: keyword.trim() || undefined,
      type: type || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        setItems(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword, type, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const restore = async (item) => {
    try {
      if (item.type === "DOCUMENT") {
        await restoreTrashDocument(item.itemId);
      } else {
        await restoreTrashFolder(item.itemId);
      }
      setToast({ type: "success", text: "Đã khôi phục" });
      load();
    } catch (e) {
      setToast({ type: "error", text: e.message });
    }
  };

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5">
        <h1 className="text-lg font-semibold text-text-primary">Trash</h1>
        <p className="mt-1 text-sm text-text-muted">
          Tài liệu và thư mục đã xoá mềm. File gốc vẫn được giữ trong storage.
        </p>
      </div>

      <div className="filter-toolbar mb-4">
        <div className="filter-toolbar-item filter-toolbar-item--date">
          <label className="label-text">Search</label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
            <input
              className="input-field"
              style={{ paddingLeft: "2.25rem" }}
              placeholder="Tên tài liệu hoặc thư mục…"
              value={keyword}
              onChange={(e) => {
                setPage(0);
                setKeyword(e.target.value);
              }}
            />
          </div>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--compact">
          <label className="label-text">Type</label>
          <select
            className="select-field"
            value={type}
            onChange={(e) => {
              setPage(0);
              setType(e.target.value);
            }}
          >
            <option value="">All</option>
            <option value="DOCUMENT">Document</option>
            <option value="FOLDER">Folder</option>
          </select>
        </div>
      </div>

      <div className="overflow-x-auto rounded-xl border border-border-subtle bg-bg-primary">
        <table className="w-full min-w-[1000px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {["Name", "Type", "Owner", "Department", "Deleted By", "Deleted At", ""].map((h, i) => (
                <th
                  key={h || i}
                  className="whitespace-nowrap px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary"
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
              empty={!loading && !error && items.length === 0}
              emptyText="Thùng rác trống"
              onRetry={load}
            />

            {!loading &&
              !error &&
              items.map((item) => (
                <tr
                  key={`${item.type}-${item.itemId}`}
                  className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                >
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-2.5">
                      {item.type === "DOCUMENT" ? (
                        <FileText className="h-4 w-4 shrink-0 text-text-muted" />
                      ) : (
                        <FolderIcon className="h-4 w-4 shrink-0 text-amber-400" />
                      )}
                      <span className="text-xs font-medium text-text-primary">{item.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2">
                    <span className="badge badge-neutral">{item.type}</span>
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">{item.ownerName ?? "—"}</td>
                  <td className="px-4 py-2 text-xs text-text-secondary">
                    {item.departmentName ?? "—"}
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">
                    {item.deletedByName ?? "—"}
                  </td>
                  <td className="whitespace-nowrap px-4 py-2 text-xs text-text-secondary">
                    {formatDateTime(item.deletedAt)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-2 text-right">
                    <button
                      type="button"
                      className="btn-ghost p-1.5"
                      aria-label={`Restore ${item.name}`}
                      onClick={() => restore(item)}
                    >
                      <RotateCcw className="h-4 w-4 text-success" />
                    </button>
                    {item.type === "FOLDER" && (
                      <button
                        type="button"
                        className="btn-ghost p-1.5"
                        aria-label={`Permanently delete ${item.name}`}
                        onClick={() => setPendingPurge(item)}
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

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            className="btn-secondary"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Trước
          </button>
          <span className="text-xs text-text-muted">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="btn-secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </button>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingPurge)}
        title="Xoá vĩnh viễn"
        message={`Xoá vĩnh viễn thư mục "${pendingPurge?.name}" cùng toàn bộ nội dung bên trong? Hành động này không thể hoàn tác.`}
        confirmLabel="Xoá vĩnh viễn"
        onClose={() => setPendingPurge(null)}
        onConfirm={async () => {
          const target = pendingPurge;
          setPendingPurge(null);
          try {
            await permanentlyDeleteTrashFolder(target.itemId);
            setToast({ type: "success", text: "Đã xoá vĩnh viễn" });
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
