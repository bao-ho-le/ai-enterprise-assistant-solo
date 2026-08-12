"use client";

import { useState } from "react";
import { ArrowLeft, Copy, Save } from "lucide-react";
import { generatedDocumentTypeLabel } from "@/constants/generatedContent";
import { formatDateTime } from "@/utils/format";

// Report/Summary counterpart of EmailPreview — same card/footer layout, without the
// email-only header (From/To/Subject).
export default function GeneratedContentPreview({ item, onBack }) {
  const [copyLabel, setCopyLabel] = useState("Copy to Clipboard");
  const [saveLabel, setSaveLabel] = useState("Save");
  const [bodyCopyLabel, setBodyCopyLabel] = useState("");

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(item?.content || "");
      setCopyLabel("Copied!");
      setTimeout(() => setCopyLabel("Copy to Clipboard"), 1500);
    } catch {
      // Clipboard API unavailable (e.g. insecure context) — nothing more we can do here.
    }
  };

  const copyBody = async () => {
    try {
      await navigator.clipboard.writeText(item?.content || "");
      setBodyCopyLabel("Copied!");
      setTimeout(() => setBodyCopyLabel(""), 1500);
    } catch {
      // Clipboard API unavailable — nothing more we can do here.
    }
  };

  const save = () => {
    // Already persisted server-side the moment it was generated — this only confirms that.
    setSaveLabel("Saved!");
    setTimeout(() => setSaveLabel("Save"), 1500);
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-text-primary">Generated Content</h1>
        <button type="button" className="btn-secondary text-sm" onClick={onBack}>
          <ArrowLeft className="h-4 w-4" />
          Back to Form
        </button>
      </div>

      <div className="card overflow-hidden">
        <div className="space-y-2 p-4 border-b border-border-subtle">
          <div className="flex items-center gap-3">
            <h2 className="min-w-0 flex-1 truncate text-sm font-medium text-text-primary" title={item?.title}>
              {item?.title || "—"}
            </h2>
            <span className="badge badge-neutral shrink-0">
              {generatedDocumentTypeLabel(item?.generatedType)}
            </span>
          </div>
          <p className="text-xs text-text-muted">
            Created {formatDateTime(item?.createdAt)} · Updated {formatDateTime(item?.updatedAt)}
          </p>
        </div>

        <div className="p-4 relative group">
          <div className="absolute top-3 right-3 flex items-center gap-2">
            {bodyCopyLabel && (
              <span className="text-xs text-accent font-medium">{bodyCopyLabel}</span>
            )}
            <button
              type="button"
              onClick={copyBody}
              title="Copy"
              className="p-1 rounded-md text-text-muted hover:text-text-primary hover:bg-bg-elevated transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
            >
              <Copy className="h-3.5 w-3.5" />
            </button>
          </div>
          <p className="text-sm text-text-primary leading-relaxed whitespace-pre-wrap text-justify">
            {item?.content}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 p-4 border-t border-border-subtle">
          <button type="button" className="btn-primary text-sm" onClick={copy}>
            <Copy className="h-4 w-4" />
            {copyLabel}
          </button>
          <button type="button" className="btn-secondary text-sm" onClick={save}>
            <Save className="h-4 w-4" />
            {saveLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
