"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, FileText, Loader2, X } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { useDebounce } from "@/hooks/useDebounce";
import { DOCUMENT_TYPES, UPLOAD_ACCEPT } from "@/constants/document";
import { checkTitle, uploadDocument } from "@/services/documentService";
import { ApiError } from "@/lib/apiClient";
import { formatBytes } from "@/utils/format";

const DEFAULT_DOCUMENT_TYPE = "REPORT";

// Display-only: hide EMAIL_TEMPLATE from the upload form (same pattern as the File
// Storage filter in FilterToolbar.js). Backend enum and validation are untouched —
// existing documents of this type, if any, are unaffected.
const UPLOAD_DOCUMENT_TYPES = DOCUMENT_TYPES.filter((o) => o.value !== "EMAIL_TEMPLATE");

function newItem(file) {
  return {
    id: crypto.randomUUID(),
    file,
    title: "",
    description: "",
    documentType: DEFAULT_DOCUMENT_TYPE,
    titleError: false,
    typeError: false,
  };
}

// One card per selected file — file + its own Title/Description/DocumentType, same
// fields and same title-uniqueness check as the old single-file form. Kept as its
// own component so each card's inputs (and its own debounce/check-title state) stay
// attached to the right list item across add/remove (key={item.id}), independent of
// every other file's card.
function UploadItemCard({ index, item, isDuplicateTitle, onChange, onRemove }) {
  const debouncedTitle = useDebounce(item.title.trim(), 450);
  const [titleTaken, setTitleTaken] = useState(false);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    if (!debouncedTitle) {
      setTitleTaken(false);
      return;
    }
    const controller = new AbortController();
    setChecking(true);
    checkTitle(debouncedTitle, controller.signal)
      .then((exists) => setTitleTaken(Boolean(exists)))
      .catch(() => {})
      .finally(() => setChecking(false));
    return () => controller.abort();
  }, [debouncedTitle]);

  return (
    <div className="space-y-3 py-6 first:pt-0 last:pb-0">
      <div className="flex items-center gap-3">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent text-xs font-medium text-white">
          {index + 1}
        </span>
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-bg-elevated">
          <FileText className="h-5 w-5 text-text-muted" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm text-text-primary" title={item.file.name}>
            {item.file.name}
          </p>
          <p className="text-xs text-text-muted">{formatBytes(item.file.size)}</p>
        </div>
        <button
          type="button"
          onClick={onRemove}
          className="btn-ghost p-1.5 shrink-0"
          aria-label={`Remove ${item.file.name}`}
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div>
        <label className="label-text">Title</label>
        <input
          type="text"
          className={`input-field ${isDuplicateTitle ? "border-error" : ""}`}
          value={item.title}
          onChange={(e) => onChange({ title: e.target.value, titleError: false })}
          placeholder="Document title"
        />
        {item.titleError ? (
          <p className="mt-1 text-xs text-error">Title is required.</p>
        ) : isDuplicateTitle ? (
          <p className="mt-1 flex items-center gap-1.5 text-xs text-error">
            <AlertTriangle className="h-3.5 w-3.5" /> Another file in this batch already uses this title.
          </p>
        ) : checking ? (
          <p className="mt-1 flex items-center gap-1.5 text-xs text-text-muted">
            <Loader2 className="h-3 w-3 animate-spin" /> Checking title…
          </p>
        ) : item.title.trim() && titleTaken ? (
          <p className="mt-1 flex items-center gap-1.5 text-xs text-warning">
            <AlertTriangle className="h-3.5 w-3.5" /> A document with this title already exists.
          </p>
        ) : item.title.trim() && !titleTaken ? (
          <p className="mt-1 flex items-center gap-1.5 text-xs text-success">
            <CheckCircle2 className="h-3.5 w-3.5" /> Title is available.
          </p>
        ) : null}
      </div>

      <div>
        <label className="label-text">Description</label>
        <textarea
          className="textarea-field"
          rows={4}
          value={item.description}
          onChange={(e) => onChange({ description: e.target.value })}
          placeholder="Optional description"
        />
      </div>

      <div>
        <label className="label-text">Document Type</label>
        <select
          className="select-field"
          value={item.documentType}
          onChange={(e) => onChange({ documentType: e.target.value, typeError: false })}
        >
          {UPLOAD_DOCUMENT_TYPES.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        {item.typeError && <p className="mt-1 text-xs text-error">Document type is required.</p>}
      </div>
    </div>
  );
}

// items holds { file, title, description, documentType } paired together per file —
// never two separate arrays kept in sync manually. Add/remove/edit all operate on
// this one array by item.id, so files[] and documents[] built at submit time (see
// submit()) are guaranteed to stay index-aligned with each other, matching the
// backend's strict by-index files[i] <-> request.documents[i] mapping.
export default function UploadDocumentModal({ open, onClose, onUploaded, folderId }) {
  const [items, setItems] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) {
      setItems([]);
      setError("");
    }
  }, [open]);

  const onFilesSelected = (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    setItems((prev) => [...prev, ...files.map(newItem)]);
    // Deliberately not resetting e.target.value here: doing so would immediately
    // collapse the input's own native filename/count text back to "No file chosen"
    // right after every pick, which is the exact bug being fixed. The trade-off is
    // that re-picking the exact same file after removing its card may not re-fire
    // onChange in some browsers — a native <input type="file"> limitation, not
    // something worth fighting with more custom logic for.
  };

  const updateItem = (id, patch) => {
    setItems((prev) => prev.map((it) => (it.id === id ? { ...it, ...patch } : it)));
  };

  const removeItem = (id) => {
    setItems((prev) => prev.filter((it) => it.id !== id));
  };

  const canSubmit = items.length > 0 && !submitting;

  // Live, frontend-only duplicate-title check across the current batch — independent
  // of (and in addition to) each card's own server-side "does this title already
  // exist" check above. Recomputed on every keystroke via `items`; cheap for the
  // handful of files a batch upload realistically has.
  const duplicateTitleIds = useMemo(() => {
    const counts = new Map();
    for (const it of items) {
      const key = it.title.trim().toLowerCase();
      if (!key) continue;
      counts.set(key, (counts.get(key) || 0) + 1);
    }
    const dupIds = new Set();
    for (const it of items) {
      const key = it.title.trim().toLowerCase();
      if (key && counts.get(key) > 1) dupIds.add(it.id);
    }
    return dupIds;
  }, [items]);

  const submit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    // Title + Document Type required, Description optional — matches
    // DocumentHelper.validateBatchRequest on the backend. Flag every offending card
    // at once (not just the first) so the user sees exactly which files need fixing.
    let hasError = false;
    const validated = items.map((it) => {
      const titleError = !it.title.trim();
      const typeError = !it.documentType;
      if (titleError || typeError) hasError = true;
      return { ...it, titleError, typeError };
    });
    if (hasError) {
      setItems(validated);
      return;
    }

    // Duplicate titles within the same batch are always a mistake (unlike "already
    // exists on the server", which may be fine) — block submit; the affected cards
    // are already marked live via duplicateTitleIds.
    if (duplicateTitleIds.size > 0) return;

    setSubmitting(true);
    setError("");
    try {
      await uploadDocument(
        items.map(({ file, title, description, documentType }) => ({
          file,
          title: title.trim(),
          description: description.trim(),
          documentType,
        })),
        folderId
      );
      onUploaded(items.length);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Upload New Document" maxWidth="max-w-2xl" preventClose={submitting}>
      <form onSubmit={submit}>
        <div className="pb-6">
          <label htmlFor="upload-files-input" className="label-text">
            Files
          </label>
          {/* Real <input type=file> has no cross-browser-consistent way to align its
              native ::file-selector-button and filename text on one baseline (Chrome
              centers them via the display:flex hack, Firefox doesn't) — so the input
              is visually hidden and a real label + span stand in for it, giving us
              the same font/line-height on both and guaranteed alignment everywhere. */}
          <div className="input-field flex h-auto items-center py-2">
            <label
              htmlFor="upload-files-input"
              className="mr-3 shrink-0 cursor-pointer rounded-md bg-bg-elevated px-3 py-1 text-sm text-text-primary"
            >
              Choose Files
            </label>
            <span className="truncate text-sm text-text-secondary">
              {items.length === 0 ? "No file chosen" : `${items.length} file${items.length > 1 ? "s" : ""}`}
            </span>
            <input
              id="upload-files-input"
              type="file"
              multiple
              accept={UPLOAD_ACCEPT}
              onChange={onFilesSelected}
              className="sr-only"
            />
          </div>
          <p className="mt-1 text-xs text-text-muted">PDF, DOC, DOCX, XLS, XLSX, TXT · up to 500 MB each</p>
        </div>

        {items.length > 0 && (
          <div className="max-h-[50vh] divide-y divide-border-subtle overflow-y-auto border-t border-border-subtle py-6 [scrollbar-gutter:stable]">
            {items.map((item, i) => (
              <UploadItemCard
                key={item.id}
                index={i}
                item={item}
                isDuplicateTitle={duplicateTitleIds.has(item.id)}
                onChange={(patch) => updateItem(item.id, patch)}
                onRemove={() => removeItem(item.id)}
              />
            ))}
          </div>
        )}

        <div className="space-y-4 pt-4">
          {error && <p className="text-sm text-error">{error}</p>}

          <div className="flex items-center justify-end gap-3">
            <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn-primary text-sm" disabled={!canSubmit}>
              {submitting ? "Uploading…" : `Upload${items.length > 1 ? ` (${items.length})` : ""}`}
            </button>
          </div>
        </div>
      </form>
    </Modal>
  );
}
