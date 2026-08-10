"use client";

import { useCallback, useMemo, useState } from "react";
import Modal from "@/components/ui/Modal";
import FolderBrowserPicker from "./FolderBrowserPicker";
import { ApiError } from "@/lib/apiClient";

// items: [{ type: "document" | "folder", id, title }] — one item for a single row's
// "Move" action, several for a bulk/drag move. onMove(items, targetFolderId) does the
// actual API calls (owned by the caller, which is also the one that knows how to
// reload/notify afterwards — same function drag-and-drop uses directly).
export default function MoveItemModal({ open, onClose, items, currentFolderId, onMove, onMoved }) {
  const [destination, setDestination] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Raw setState identity is stable across renders, so this is safe to pass
  // straight into FolderBrowserPicker's onLocationChange effect dependency
  // without re-firing on every parent render.
  const handleLocationChange = useCallback((loc) => setDestination(loc), []);

  // Folders being moved can't be their own destination. They still show up in the
  // picker (dimmed — see FolderBrowserPicker's `movingItems`/PickerRow `dimmed`)
  // instead of vanishing, but this keeps them picking impossible either way.
  const excludeFolderIds = useMemo(
    () => (items || []).filter((it) => it.type === "folder").map((it) => it.id),
    [items]
  );

  const destinationFolderId = destination?.currentFolder?.id ?? null;
  const canMove = destinationFolderId != null && !excludeFolderIds.includes(destinationFolderId);

  const count = items?.length || 0;
  const title =
    count === 1
      ? `Moving ${items[0].type === "folder" ? "Folder" : "Document"}${items[0].title ? ` ${items[0].title}` : ""}`
      : `Moving ${count} items`;

  const submit = async () => {
    if (!canMove || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await onMove(items, destinationFolderId);
      onMoved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to move. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={title} maxWidth="max-w-2xl" preventClose={submitting}>
      <FolderBrowserPicker
        active={open}
        mode="move"
        initialFolderId={currentFolderId}
        movingItems={items}
        onLocationChange={handleLocationChange}
      />

      {error && <p className="mt-3 text-sm text-error">{error}</p>}

      <div className="flex items-center justify-end gap-3 pt-4">
        <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
          Cancel
        </button>
        <button type="button" className="btn-primary text-sm" onClick={submit} disabled={!canMove || submitting}>
          {submitting ? "Moving…" : "Move Here"}
        </button>
      </div>
    </Modal>
  );
}
