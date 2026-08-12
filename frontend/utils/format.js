// Cycled background/icon tint for attached-document rows — mirrors the old UI's
// per-file coloring without depending on a file-extension field the API doesn't return.
const ATTACHMENT_ICON_STYLES = [
  { bg: "bg-red-500/10", color: "text-red-400" },
  { bg: "bg-blue-500/10", color: "text-blue-400" },
  { bg: "bg-emerald-500/10", color: "text-emerald-400" },
];

export function attachmentIconStyle(index) {
  return ATTACHMENT_ICON_STYLES[index % ATTACHMENT_ICON_STYLES.length];
}

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return "—";
  if (bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const value = bytes / Math.pow(1024, i);
  return `${value.toFixed(value < 10 && i > 0 ? 1 : 0)} ${units[i]}`;
}

// LocalDateTime from backend is ISO without zone, e.g. "2026-07-10T14:30:00".
function toDate(value) {
  if (!value) return null;
  const d = new Date(value);
  return isNaN(d.getTime()) ? null : d;
}

// "Jul 5, 2026 · 09:14"
export function formatDateTime(value) {
  const d = toDate(value);
  if (!d) return "—";
  const date = d.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  const time = d.toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  return `${date} · ${time}`;
}

// "10/07/2026 · 14:30"
export function formatDateTimeSlash(value) {
  const d = toDate(value);
  if (!d) return "—";
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} · ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// "10/07/2026"
export function formatDateShort(value) {
  const d = toDate(value);
  if (!d) return "—";
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

// Backend conversation titles are generated as "<Type Name> - 29/07/2026 12:29"
// (see AIConversationHelper.defaultTitle). "12:29" (and "· 12:29") -> "- 12:29" so
// the date and time are both dash-separated wherever a title is shown verbatim
// (sidebar, deleted conversations) — titles that already separate them are left alone.
export function formatConversationTitle(title) {
  if (!title) return title;
  return title.replace(/(\d{2}\/\d{2}\/\d{4})\s*(?:·|-)?\s*(\d{2}:\d{2})/, "$1 - $2");
}

// A <input type="date"> value ("2026-07-19") -> ISO LocalDateTime the backend expects.
export function dateInputToIso(value, endOfDay = false) {
  if (!value) return null;
  return endOfDay ? `${value}T23:59:59` : `${value}T00:00:00`;
}

// Content-Disposition: attachment; filename="report.pdf"; filename*=UTF-8''report.pdf
export function filenameFromContentDisposition(header, fallback = "download") {
  if (!header) return fallback;
  const star = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (star) return decodeURIComponent(star[1].trim());
  const plain = /filename="?([^";]+)"?/i.exec(header);
  return plain ? plain[1].trim() : fallback;
}

// Splits `text` into [{ text, match }] segments, marking the given
// [start, end) ranges (text-relative, may overlap/be unsorted) for
// rendering <mark> highlights.
export function rangeSegments(text, ranges) {
  if (!text) return [];
  if (!ranges || ranges.length === 0) return [{ text, match: false }];

  const merged = [...ranges]
    .map(([start, end]) => [Math.max(0, start), Math.min(text.length, end)])
    .filter(([start, end]) => end > start)
    .sort((a, b) => a[0] - b[0])
    .reduce((acc, [start, end]) => {
      const last = acc[acc.length - 1];
      if (last && start <= last[1]) last[1] = Math.max(last[1], end);
      else acc.push([start, end]);
      return acc;
    }, []);

  if (merged.length === 0) return [{ text, match: false }];

  const segments = [];
  let cursor = 0;
  for (const [start, end] of merged) {
    if (start > cursor) segments.push({ text: text.slice(cursor, start), match: false });
    segments.push({ text: text.slice(start, end), match: true });
    cursor = end;
  }
  if (cursor < text.length) segments.push({ text: text.slice(cursor), match: false });
  return segments;
}

// "Nguyen Tran" -> "NT" (avatar initials: first + last word).
export function getInitials(fullName) {
  if (!fullName) return "?";
  const parts = fullName.trim().split(/\s+/);
  const first = parts[0]?.[0] || "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

// Save a Blob to disk from the browser.
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
