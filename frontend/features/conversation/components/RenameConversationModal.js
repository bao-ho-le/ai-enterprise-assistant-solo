"use client";

import { useEffect, useState } from "react";
import Modal from "@/components/ui/Modal";
import { renameConversation } from "@/services/conversationService";
import { ApiError } from "@/lib/apiClient";

export default function RenameConversationModal({ open, onClose, conversation, onRenamed }) {
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) setTitle(conversation?.title || "");
    setError("");
  }, [open, conversation]);

  const submit = async (e) => {
    e.preventDefault();
    if (!title.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      const updated = await renameConversation(conversation.id, title.trim());
      onRenamed(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Rename failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Rename conversation" maxWidth="max-w-md" preventClose={submitting}>
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label htmlFor="rename-conversation-title" className="label-text">Title</label>
          <input
            id="rename-conversation-title"
            type="text"
            className="input-field"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            autoFocus
          />
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <div className="flex items-center justify-end gap-3 pt-2">
          <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn-primary text-sm" disabled={!title.trim() || submitting}>
            {submitting ? "Saving…" : "Save"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
