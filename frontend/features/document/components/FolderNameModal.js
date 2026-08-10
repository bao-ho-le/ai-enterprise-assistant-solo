"use client";

import { useEffect, useState } from "react";
import Modal from "@/components/ui/Modal";
import { createFolder, renameFolder } from "@/services/folderService";
import { ApiError } from "@/lib/apiClient";

// One modal for both "New Folder" and "Edit Name" — same single field, same
// validation; `folder` decides which endpoint runs (null = create in parentFolderId).
export default function FolderNameModal({ open, onClose, folder, parentFolderId, onSaved }) {
  const isRename = Boolean(folder);
  const [name, setName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) setName(folder?.name || "");
    setError("");
  }, [open, folder]);

  const submit = async (e) => {
    e.preventDefault();
    if (!name.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      if (isRename) {
        await renameFolder(folder.id, name.trim());
      } else {
        await createFolder({ name: name.trim(), parentId: parentFolderId });
      }
      onSaved(isRename);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Save failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isRename ? "Edit folder name" : "New folder"}
      maxWidth="max-w-md"
      preventClose={submitting}
    >
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label htmlFor="folder-name-input" className="label-text">
            Folder name
          </label>
          <input
            id="folder-name-input"
            type="text"
            className="input-field"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Untitled folder"
            autoFocus
          />
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <div className="flex items-center justify-end gap-3 pt-2">
          <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn-primary text-sm" disabled={!name.trim() || submitting}>
            {submitting ? "Saving…" : "Save"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
