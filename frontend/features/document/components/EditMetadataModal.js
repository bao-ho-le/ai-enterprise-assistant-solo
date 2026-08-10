"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { DOCUMENT_TYPES } from "@/constants/document";
import { getDocument, updateMetadata } from "@/services/documentService";
import { ApiError } from "@/lib/apiClient";

// Fetches the document's current metadata on open so we never clobber fields
// (the list response has no `description`).
export default function EditMetadataModal({ open, onClose, documentId, onSaved }) {
  const [form, setForm] = useState({ title: "", description: "", documentType: "REPORT" });
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !documentId) return;
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
    if (!form.title.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await updateMetadata(documentId, {
        title: form.title.trim(),
        description: form.description.trim(),
        documentType: form.documentType,
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Update failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Edit Metadata" preventClose={submitting}>
      {loading ? (
        <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={submit}>
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
            <button type="submit" className="btn-primary text-sm" disabled={!form.title.trim() || submitting}>
              {submitting ? "Saving…" : "Save Changes"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
