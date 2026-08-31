"use client";

import { useCallback, useEffect, useState } from "react";
import { FileText, FolderIcon, RotateCcw, Search } from "lucide-react";
import Toast from "@/components/ui/Toast";
import AdminTableState from "@/features/admin/components/AdminTableState";
import Pagination from "@/features/document/components/Pagination";
import {
  getAdminTrash,
  restoreTrashDocument,
  restoreTrashFolder,
} from "@/services/adminService";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminTrashPage() {
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [type, setType] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

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
        setTotalElements(data.totalElements ?? 0);
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
      setToast({ type: "success", text: "Restored" });
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
              placeholder="Document or folder name…"
              value={keyword}
              onChange={(e) => {
                setPage(0);
                setKeyword(e.target.value);
              }}
            />
          </div>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--auto">
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

      <div className="flex flex-1 min-h-0 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
        <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full min-w-[1000px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {["Name", "Type", "Owner", "Department", "Deleted By", "Deleted At", "Actions"].map((h, i, arr) => (
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
              colSpan={7}
              loading={loading}
              error={error}
              empty={!loading && !error && items.length === 0}
              emptyText="Trash is empty"
              onRetry={load}
            />

            {!loading &&
              !error &&
              items.map((item) => (
                <tr
                  key={`${item.type}-${item.itemId}`}
                  className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                >
                  <td className="px-4 py-1">
                    <div className="flex items-center gap-3">
                      {item.type === "DOCUMENT" ? (
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-bg-elevated">
                          <FileText className="h-3.5 w-3.5 text-text-muted" />
                        </span>
                      ) : (
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-500/10">
                          <FolderIcon className="h-3.5 w-3.5 text-amber-400" />
                        </span>
                      )}
                      <span className="text-xs font-medium text-text-primary">{item.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-1">
                    <span className="badge badge-neutral">{item.type}</span>
                  </td>
                  <td className="px-4 py-1 text-xs text-text-secondary">{item.ownerName ?? "—"}</td>
                  <td className="px-4 py-1 text-xs text-text-secondary">
                    {item.departmentName ?? "—"}
                  </td>
                  <td className="px-4 py-1 text-xs text-text-secondary">
                    {item.deletedByName ?? "—"}
                  </td>
                  <td className="whitespace-nowrap px-4 py-1 text-xs text-text-secondary">
                    {formatDateTime(item.deletedAt)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-1 text-right">
                    <button
                      type="button"
                      className="btn-ghost p-1.5"
                      aria-label={`Restore ${item.name}`}
                      onClick={() => restore(item)}
                    >
                      <RotateCcw className="h-4 w-4 text-success" />
                    </button>
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
          shown={items.length}
          onPrev={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
          itemLabel="items"
          footerClassName="bg-bg-primary py-2"
        />
      </div>

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
