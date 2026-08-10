"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { FileText, Folder, Loader2, RotateCcw, Search } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import Toast from "@/components/ui/Toast";
import { listDocuments, restoreDocument } from "@/services/documentService";
import { getDeletedFolders, restoreFolder } from "@/services/folderService";
import { useDebounce } from "@/hooks/useDebounce";

const PAGE_SIZE = 20;

// "10/08/2026 · 16:17" — dd/mm/yyyy, Trash-specific (File Storage's formatDateTime uses "Aug 10, 2026").
function formatDeletedAt(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (isNaN(d.getTime())) return "—";
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const time = d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false });
  return `${day}/${month}/${d.getFullYear()} · ${time}`;
}

// Restore only — permanent delete is deliberately not offered here.
//
// Both lists reuse endpoints that already existed: deleted documents come from the
// normal document list filtered by documentStatus=DELETED (the same filter the
// File Storage "Status" dropdown uses), deleted folders from /folders/deleted.
export default function TrashView() {
  // Deep link from DeletedDocumentsWarning (Document QA / Write Report / Summary):
  // /file-storage/trash?highlight=12,34 marks which deleted document(s) sent the
  // user here, so they land on the exact file instead of having to find it themselves.
  const searchParams = useSearchParams();
  const highlightIds = useMemo(
    () => new Set((searchParams.get("highlight") || "").split(",").filter(Boolean).map(Number)),
    [searchParams]
  );
  const scrolledToHighlightRef = useRef(false);

  const [folders, setFolders] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [folderPage, setFolderPage] = useState(0);
  const [documentPage, setDocumentPage] = useState(0);
  const [moreFolders, setMoreFolders] = useState(false);
  const [moreDocuments, setMoreDocuments] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState("");
  const [error, setError] = useState("");
  const [restoringKey, setRestoringKey] = useState(null);
  const [toast, setToast] = useState(null);

  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const trimmedKeyword = debouncedKeyword.trim();
  const isFiltered = trimmedKeyword.length > 0;

  const notify = useCallback((type, text) => setToast({ type, text }), []);

  const fetchFolders = useCallback(
    (page, signal) =>
      getDeletedFolders({ page, size: PAGE_SIZE, keyword: trimmedKeyword || undefined }, signal),
    [trimmedKeyword]
  );
  const fetchDocuments = useCallback(
    (page, signal) =>
      listDocuments(
        {
          documentStatus: "DELETED",
          sort: "newest",
          page,
          size: PAGE_SIZE,
          keyword: trimmedKeyword || undefined,
        },
        signal
      ),
    [trimmedKeyword]
  );

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");

    Promise.all([fetchFolders(0, controller.signal), fetchDocuments(0, controller.signal)])
      .then(([folderPageData, documentPageData]) => {
        if (controller.signal.aborted) return;
        setFolders(folderPageData?.content || []);
        setDocuments(documentPageData?.content || []);
        setMoreFolders(!(folderPageData?.last ?? true));
        setMoreDocuments(!(documentPageData?.last ?? true));
        setFolderPage(0);
        setDocumentPage(0);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load the trash");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [fetchFolders, fetchDocuments]);

  // Runs once the highlighted document actually shows up in the loaded list —
  // guarded so restoring/removing items later never re-triggers the scroll.
  useEffect(() => {
    if (scrolledToHighlightRef.current || highlightIds.size === 0) return;
    const target = documents.find((d) => highlightIds.has(d.id));
    if (!target) return;
    scrolledToHighlightRef.current = true;
    document.getElementById(`trash-document-${target.id}`)?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [documents, highlightIds]);

  const loadMoreFolders = async () => {
    setLoadingMore("folders");
    try {
      const next = folderPage + 1;
      const pageData = await fetchFolders(next);
      setFolders((prev) => [...prev, ...(pageData?.content || [])]);
      setMoreFolders(!(pageData?.last ?? true));
      setFolderPage(next);
    } catch (err) {
      notify("error", err.message || "Failed to load more folders");
    } finally {
      setLoadingMore("");
    }
  };

  const loadMoreDocuments = async () => {
    setLoadingMore("documents");
    try {
      const next = documentPage + 1;
      const pageData = await fetchDocuments(next);
      setDocuments((prev) => [...prev, ...(pageData?.content || [])]);
      setMoreDocuments(!(pageData?.last ?? true));
      setDocumentPage(next);
    } catch (err) {
      notify("error", err.message || "Failed to load more documents");
    } finally {
      setLoadingMore("");
    }
  };

  const onRestoreFolder = async (folder) => {
    setRestoringKey(`folder-${folder.id}`);
    try {
      await restoreFolder(folder.id);
      setFolders((prev) => prev.filter((f) => f.id !== folder.id));
      // Restoring a folder revives the documents that went to the trash with it,
      // so drop any of them that are showing in the documents list below.
      const restored = await fetchDocuments(0);
      setDocuments(restored?.content || []);
      setMoreDocuments(!(restored?.last ?? true));
      setDocumentPage(0);
      notify("success", "Folder restored");
    } catch (err) {
      notify("error", err.message || "Failed to restore folder");
    } finally {
      setRestoringKey(null);
    }
  };

  const onRestoreDocument = async (doc) => {
    setRestoringKey(`document-${doc.id}`);
    try {
      await restoreDocument(doc.id);
      setDocuments((prev) => prev.filter((d) => d.id !== doc.id));
      notify("success", "Document restored");
    } catch (err) {
      notify("error", err.message || "Failed to restore document");
    } finally {
      setRestoringKey(null);
    }
  };

  // Icon-only: .btn-secondary's own padding (0.625rem 1.25rem) is sized for a
  // button with a text label, so dropping "Restore" here needs an explicit
  // override for a proper square icon button — a padding utility class would
  // lose to .btn-secondary's own rule (unlayered CSS beats Tailwind utilities).
  const restoreButton = (key, onClick) => (
    <button
      type="button"
      className="btn-secondary shrink-0"
      style={{ padding: "0.5rem" }}
      aria-label="Restore"
      title="Restore"
      disabled={restoringKey != null}
      onClick={onClick}
    >
      {restoringKey === key ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <RotateCcw className="h-4 w-4" />
      )}
    </button>
  );

  const isEmpty = folders.length === 0 && documents.length === 0;

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 pt-2 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5">
        <h1 className="text-lg font-semibold text-text-primary">Trash</h1>
        <p className="mt-1 text-sm text-text-muted">
          Folders and documents you deleted. Restoring a folder also restores everything that was
          inside it.
        </p>
      </div>

      <div className="mb-5 flex h-9 items-center rounded-lg border border-border-subtle bg-bg-card px-3 sm:w-[calc((100%-0.75rem)/2)] lg:w-[calc((100%-1.5rem)/3)]">
        <Search className="h-4 w-4 shrink-0 text-text-muted" />
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search folders and documents..."
          aria-label="Search trash"
          className="ml-2 flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : error ? (
        <p className="text-sm text-error">{error}</p>
      ) : isEmpty ? (
        <p className="text-sm text-text-muted">
          {isFiltered ? "No matching items in trash." : "The trash is empty."}
        </p>
      ) : (
        <div className="space-y-6">
          {folders.length > 0 && (
            <section>
              <h2 className="mb-2 text-xs font-medium uppercase tracking-wider text-text-muted">
                Folders
              </h2>
              <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {folders.map((folder) => (
                  <li key={`folder-${folder.id}`} className="card flex items-center gap-3 p-3">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-500/10">
                      <Folder className="h-3.5 w-3.5 text-amber-400" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-text-primary" title={folder.name}>
                        {folder.name}
                      </p>
                      <p className="text-xs text-text-muted">Deleted at: {formatDeletedAt(folder.deletedAt)}</p>
                    </div>
                    {restoreButton(`folder-${folder.id}`, () => onRestoreFolder(folder))}
                  </li>
                ))}
              </ul>
              <LoadMore
                hasMore={moreFolders}
                loading={loadingMore === "folders"}
                onClick={loadMoreFolders}
              />
            </section>
          )}

          {documents.length > 0 && (
            <section>
              <h2 className="mb-2 text-xs font-medium uppercase tracking-wider text-text-muted">
                Documents
              </h2>
              <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {documents.map((doc) => (
                  <li
                    key={`document-${doc.id}`}
                    id={`trash-document-${doc.id}`}
                    className="card flex items-center gap-3 p-3"
                    // Same override reason as the border-collapse fix elsewhere: .card
                    // already sets its own border/background, so a Tailwind border/bg
                    // utility here would be silently ignored — inline style wins instead.
                    style={
                      highlightIds.has(doc.id)
                        ? { borderColor: "var(--accent)", backgroundColor: "rgba(59, 130, 246, 0.08)" }
                        : undefined
                    }
                  >
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-bg-elevated">
                      <FileText className="h-3.5 w-3.5 text-text-secondary" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-text-primary" title={doc.title}>
                        {doc.title}
                      </p>
                      <p className="text-xs text-text-muted">Deleted at: {formatDeletedAt(doc.deletedAt)}</p>
                    </div>
                    {restoreButton(`document-${doc.id}`, () => onRestoreDocument(doc))}
                  </li>
                ))}
              </ul>
              <LoadMore
                hasMore={moreDocuments}
                loading={loadingMore === "documents"}
                onClick={loadMoreDocuments}
              />
            </section>
          )}
        </div>
      )}

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
