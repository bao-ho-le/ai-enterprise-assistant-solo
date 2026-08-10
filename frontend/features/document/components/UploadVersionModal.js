"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { DOCUMENT_TYPES, UPLOAD_ACCEPT } from "@/constants/document";
import { getDocument, uploadNewVersion } from "@/services/documentService";
import { ApiError } from "@/lib/apiClient";

const EMPTY_FORM = { title: "", description: "", documentType: "REPORT" };

// Prefills title/description/documentType from the document's current metadata
// (like EditMetadataModal) so the form doesn't start blank — submitting updates
// them alongside the new version.
export default function UploadVersionModal({ open, onClose, documentId, title, onUploaded }) {
  const [file, setFile] = useState(null);
  const [changeNote, setChangeNote] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) {
      setFile(null);
      setChangeNote("");
      setForm(EMPTY_FORM);
      setError("");
      return;
    }
    if (!documentId) return;

    const controller = new AbortController();
    setLoading(true);
    setError("");
    getDocument(documentId, controller.signal)
      .then((detail) => {
        if (controller.signal.aborted) return;
        const info = detail?.documentInfo || {};
        setForm({
          title: info.title || "",
          description: info.description || "",
          documentType: info.documentType || "REPORT",
        });
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message || "Failed to load document");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [open, documentId]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (!file || !form.title.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await uploadNewVersion(documentId, {
        file,
        changeNote: changeNote.trim(),
        title: form.title.trim(),
        description: form.description.trim(),
        documentType: form.documentType,
      });
      onUploaded();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Upload New Version" preventClose={submitting}>
      {title && (
        <p className="mb-4 text-sm text-text-muted">
          Document: <span className="text-text-secondary">{title}</span>
        </p>
      )}

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={submit}>
          <div>
            <label className="label-text">File</label>
            <input
              type="file"
              accept={UPLOAD_ACCEPT}
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="input-field h-auto py-2 file:mr-3 file:rounded-md file:border-0 file:bg-bg-elevated file:px-3 file:py-1 file:text-sm file:text-text-primary"
            />
            <p className="mt-1 text-xs text-text-muted">PDF, DOC, DOCX, XLS, XLSX, TXT · up to 500 MB</p>
          </div>

          <div>
            <label className="label-text">Change Note</label>
            <textarea
              className="textarea-field"
              rows={3}
              maxLength={255}
              value={changeNote}
              onChange={(e) => setChangeNote(e.target.value)}
              placeholder="What changed in this version? (optional)"
            />
          </div>

          <div>
            <label className="label-text">Title</label>
            <input type="text" className="input-field" value={form.title} onChange={set("title")} />
          </div>

          <div>
            <label className="label-text">Description</label>
            <textarea className="textarea-field" rows={3} value={form.description} onChange={set("description")} />
          </div>

          <div>
            <label className="label-text">Document Type</label>
            <select className="select-field" value={form.documentType} onChange={set("documentType")}>
              {DOCUMENT_TYPES.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>

          {error && <p className="text-sm text-error">{error}</p>}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary text-sm"
              disabled={!file || !form.title.trim() || submitting}
            >
              {submitting ? "Uploading…" : "Upload Version"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
