"use client";

import { useCallback, useState } from "react";
import Modal from "@/components/ui/Modal";
import FolderBrowserPicker from "@/features/document/components/FolderBrowserPicker";
import { getDocument } from "@/services/documentService";
import { attachDocuments } from "@/services/conversationService";
import { ApiError } from "@/lib/apiClient";

// Document list rows carry no versionId (backend DocumentListResponse omits it) —
// resolve each selected document's current version at submit time, same pattern
// as documentService.downloadCurrentVersion.
//
// Two modes: pass `conversationId` + `onAttached` to attach immediately via the API
// (existing conversation). Pass `onSelect` instead when there's no conversation yet
// (generation create-form) — resolves versions the same way but just hands the
// picked documents back to the caller instead of calling the attach endpoint.
export default function AttachDocumentsModal({ open, onClose, conversationId, alreadyAttachedDocumentIds, onAttached, onSelect }) {
  const [selectedIds, setSelectedIds] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const documentFilter = (doc) => !alreadyAttachedDocumentIds?.includes(doc.id) && doc.versionStatus === "READY";

  // Raw setState identity is stable across renders, so this is safe to pass
  // straight into FolderBrowserPicker's onSelectionChange effect dependency
  // without re-firing on every parent render.
  const handleSelectionChange = useCallback((ids) => setSelectedIds(ids), []);

  const submit = async () => {
    if (selectedIds.length === 0 || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      const details = await Promise.all(selectedIds.map((id) => getDocument(id)));

      if (onSelect) {
        const selected = selectedIds
          .map((id, i) => {
            const versionId = details[i]?.currentVersion?.versionId;
            if (versionId == null) return null;
            return {
              documentId: id,
              documentVersionId: versionId,
              documentTitle: details[i]?.documentInfo?.title,
              versionNumber: details[i].currentVersion.versionNumber,
            };
          })
          .filter(Boolean);
        onSelect(selected);
        return;
      }

      const documentVersionIds = details.map((d) => d?.currentVersion?.versionId).filter((id) => id != null);
      await attachDocuments(conversationId, documentVersionIds);
      onAttached();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to attach documents. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Attach documents" maxWidth="max-w-2xl" preventClose={submitting}>
      <FolderBrowserPicker
        active={open}
        mode="attach"
        documentFilter={documentFilter}
        onSelectionChange={handleSelectionChange}
      />

      {error && <p className="text-sm text-error mt-3">{error}</p>}

      <div className="flex items-center justify-end gap-3 pt-4">
        <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
          Cancel
        </button>
        <button
          type="button"
          className="btn-primary text-sm"
          onClick={submit}
          disabled={selectedIds.length === 0 || submitting}
        >
          {submitting ? "Working…" : `${onSelect ? "Add" : "Attach"} (${selectedIds.length})`}
        </button>
      </div>
    </Modal>
  );
}
