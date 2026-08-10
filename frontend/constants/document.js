// Mirrors backend enums: DocumentType, VersionStatus, sort options.

import { FileText, FileSpreadsheet, File as FileIcon } from "lucide-react";

export const DOCUMENT_TYPES = [
  { value: "EMAIL_TEMPLATE", label: "Email Template" },
  { value: "REPORT", label: "Report" },
  { value: "MEETING_MINUTES", label: "Meeting Minutes" },
  { value: "CONTRACT", label: "Contract" },
  { value: "FORM", label: "Form" },
  { value: "OTHER", label: "Other" },
];

// Display-only: turns a raw enum value (e.g. "MEETING_MINUTES") into Title Case
// ("Meeting Minutes"). Never mutates the stored value.
function documentTypeToTitleCase(value) {
  return value
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export function documentTypeLabel(value) {
  if (!value) return "—";
  const t = DOCUMENT_TYPES.find((x) => x.value === value);
  // Known types use their curated label; anything else (e.g. a type added
  // later, like PRESENTATION) is formatted into Title Case for display.
  return t ? t.label : documentTypeToTitleCase(value);
}

// DocumentVersion.status -> badge class + label ("Processing" column)
export const VERSION_STATUS = {
  PENDING: { label: "PENDING", badge: "badge-warning" },
  PROCESSING: { label: "PROCESSING", badge: "badge-warning" },
  READY: { label: "READY", badge: "badge-success" },
  FAILED: { label: "FAILED", badge: "badge-error" },
};

export function versionStatusBadge(value) {
  return VERSION_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}

// Document.status -> badge class + label ("Status" column) — independent from
// version processing status, never conflate the two.
export const DOCUMENT_STATUS = {
  ACTIVE: { label: "ACTIVE", badge: "badge-success" },
  DELETED: { label: "DELETED", badge: "badge-error" },
};

export function documentStatusBadge(value) {
  return DOCUMENT_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}

// "Status" filter (Document.status) — no "All" option, defaults to ACTIVE.
export const DOCUMENT_STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Active" },
  { value: "DELETED", label: "Deleted" },
];

// "Processing" filter (DocumentVersion.status)
export const STATUS_OPTIONS = [
  { value: "", label: "All" },
  { value: "READY", label: "Ready" },
  { value: "PROCESSING", label: "Processing" },
  { value: "PENDING", label: "Pending" },
  { value: "FAILED", label: "Failed" },
];

export const SORT_OPTIONS = [
  { value: "newest", label: "Newest" },
  { value: "oldest", label: "Oldest" },
];

export const EXTENSION_OPTIONS = [
  { value: "", label: "All" },
  { value: "pdf", label: "PDF" },
  { value: "docx", label: "DOCX" },
  { value: "xlsx", label: "XLSX" },
  { value: "txt", label: "TXT" },
];

// Accept attribute + allowed MIME types for upload (matches backend DocumentHelper).
export const UPLOAD_ACCEPT =
  ".pdf,.doc,.docx,.xls,.xlsx,.txt";

// File Name icon + color, picked by extension (PDF red, Word blue, Excel green,
// plain text neutral, anything else falls back to a generic file icon). Shared by
// DocumentRow and any other place that renders a document by extension (e.g. the
// Attach/Move document pickers) so the icon mapping only lives in one place.
const EXTENSION_ICON = {
  pdf: { Icon: FileText, bg: "bg-red-500/10", color: "text-red-400" },
  doc: { Icon: FileText, bg: "bg-blue-500/10", color: "text-blue-400" },
  docx: { Icon: FileText, bg: "bg-blue-500/10", color: "text-blue-400" },
  xls: { Icon: FileSpreadsheet, bg: "bg-green-500/10", color: "text-green-400" },
  xlsx: { Icon: FileSpreadsheet, bg: "bg-green-500/10", color: "text-green-400" },
  txt: { Icon: FileText, bg: "bg-bg-elevated", color: "text-text-secondary" },
};

export function extensionIcon(extension) {
  return (
    EXTENSION_ICON[(extension || "").toLowerCase()] || {
      Icon: FileIcon,
      bg: "bg-bg-elevated",
      color: "text-text-secondary",
    }
  );
}
