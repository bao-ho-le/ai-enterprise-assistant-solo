"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ScanSearch, Download, FolderOpen, MoreHorizontal } from "lucide-react";
import { documentTypeLabel, versionStatusBadge, extensionIcon } from "@/constants/document";
import { formatBytes, formatDateTime } from "@/utils/format";
import { hrefForFolderId } from "@/utils/folderPath";
import { gridTemplateColumns } from "./documentTableGrid";
import { useClickVsDoubleClick } from "@/hooks/useClickVsDoubleClick";

// Match dot colour per score band. The two middle bands are a linear RGB gradient
// between the endpoints --success (#22c55e) and --warning (#eab308): t=1/3 ->
// #65bf41, t=2/3 -> #a7b925. Below 65% is unreachable on screen —
// qdrant.score-threshold=0.65 drops those hits server-side — so it falls back to
// the muted neutral instead of getting a band of its own.
function matchDotColor(percent) {
  if (percent >= 80) return "var(--success)";
  if (percent >= 75) return "#65bf41";
  if (percent >= 70) return "#a7b925";
  if (percent >= 65) return "var(--warning)";
  return "var(--text-muted)";
}

export default function DocumentRow({
  doc,
  showSemanticColumn,
  selected,
  onToggle,
  onRowSelect,
  buildDragPayload,
  onDownload,
  onMenuTrigger,
  isContextMenuTarget,
  onViewEvidence,
}) {
  const processing = versionStatusBadge(doc.versionStatus);
  const hasMatch = doc.semanticScore !== null && doc.semanticScore !== undefined;
  const similarityPercent = hasMatch ? Math.round(doc.semanticScore * 100) : 0;
  const { Icon: ExtIcon, bg: iconBg, color: iconColor } = extensionIcon(doc.extension);
  const router = useRouter();
  // Delays the select-on-click so a double-click (open the document) doesn't
  // flash a selection highlight right before navigating away — see
  // useClickVsDoubleClick.
  const { onClick, onDoubleClick } = useClickVsDoubleClick(
    (mods) => onRowSelect?.("document", doc.id, mods),
    () => router.push(`/file-storage/${doc.id}`)
  );

  return (
    // Clicking anywhere on the row selects it (Finder/Explorer semantics — see
    // FileStorageView's selectRow); double-click opens the document detail page.
    // The checkbox and Actions cells stop the click so selecting/acting doesn't
    // also re-select the row underneath.
    //
    // grid, not <tr>: the header row lives outside the scroll container (see
    // DocumentTable.js), so there's no shared <table> left to align columns —
    // gridTemplateColumns is the same shared value the header uses instead.
    // Every cell carries its own border-b (that part is unchanged from the old
    // border-separate table) plus flex/items-center to replace the vertical
    // centering a <td> gave for free.
    //
    // Right-click and the "..." button both open the table's ONE context menu
    // (see DocumentTable's contextMenu state) via onMenuTrigger — no menu lives
    // inside the row anymore, so there can never be two menus at once. Only
    // wired when that menu actually exists (search-result rows show different
    // actions with no row menu).
    //
    // draggable: drags the whole current selection if this row is part of it,
    // otherwise just this row (see buildDragPayload in FileStorageView).
    <div
      role="row"
      draggable={!showSemanticColumn}
      className={`grid cursor-pointer transition-colors hover:bg-bg-elevated/50 ${
        isContextMenuTarget ? "bg-bg-elevated/70" : "bg-bg-primary"
      }`}
      style={{ gridTemplateColumns: gridTemplateColumns(showSemanticColumn) }}
      onClick={onClick}
      onDoubleClick={onDoubleClick}
      onDragStart={(e) => {
        e.dataTransfer.effectAllowed = "move";
        e.dataTransfer.setData("application/json", JSON.stringify(buildDragPayload?.("document", doc.id) || []));
      }}
      onContextMenu={
        showSemanticColumn
          ? undefined
          : (e) => {
              e.preventDefault();
              onMenuTrigger?.("document", doc, { kind: "point", x: e.clientX, y: e.clientY });
            }
      }
    >
      <div role="cell" className="min-w-0 flex items-center gap-3 border-b border-border-default px-4 py-1">
        <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${iconBg}`}>
          <ExtIcon className={`h-3.5 w-3.5 ${iconColor}`} />
        </span>
        {/* No max-width cap: the File Name column expands to fill the table,
            so the filename only truncates when it genuinely exceeds the
            available width. */}
        <div className="min-w-0">
          {/* Plain text, not a Link: a Link here would navigate on the first
              click and bypass the row's double-click-to-open behavior. */}
          <span
            className="block truncate text-xs font-medium text-text-primary"
            title={doc.title}
          >
            {doc.title}
          </span>
        </div>
      </div>
      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1"
        onClick={(e) => e.stopPropagation()}
        onDoubleClick={(e) => e.stopPropagation()}
      >
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
          checked={selected}
          onChange={() => onToggle(doc.id)}
          aria-label={`Select ${doc.title}`}
        />
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap"
      >
        {formatDateTime(doc.uploadTime)}
      </div>

      {/* uppercase is display-only, the same utility the Extension cell already
          uses — the stored DocumentType enum value is untouched. */}
      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap uppercase"
      >
        {documentTypeLabel(doc.documentType)}
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary uppercase"
      >
        {doc.extension || "—"}
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap"
      >
        {formatBytes(doc.size)}
      </div>

      {showSemanticColumn && (
        <div role="cell" className="flex items-center border-b border-border-default px-4 py-1 text-xs">
          {hasMatch ? (
            <div className="flex items-center gap-1.5">
              {/* Fixed width + right-aligned + tabular-nums: "71%" and "99%" render at
                  slightly different widths otherwise (proportional digit spacing), which
                  shifts the dot that follows. Locking the text column's width keeps the
                  dot's position constant regardless of the digits. */}
              <span className="w-7 shrink-0 text-right text-xs tabular-nums text-text-secondary whitespace-nowrap">
                {similarityPercent}%
              </span>
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: matchDotColor(similarityPercent) }}
                aria-hidden="true"
              />
            </div>
          ) : (
            <span className="text-text-muted">—</span>
          )}
        </div>
      )}

      <div role="cell" className="flex items-center border-b border-border-default px-4 py-1">
        <span className={`badge ${processing.badge}`}>{processing.label}</span>
      </div>

      {/* Search results only carry the two actions that make sense on a match:
          inspect the evidence, or jump to where the document lives. */}
      <div
        role="cell"
        className="flex items-center justify-end gap-1 border-b border-border-default px-4 py-1"
        onClick={(e) => e.stopPropagation()}
        onDoubleClick={(e) => e.stopPropagation()}
      >
        {showSemanticColumn ? (
          <>
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="View matching chunks"
              title="View matching chunks"
              disabled={!hasMatch}
              onClick={() => onViewEvidence(doc)}
            >
              <ScanSearch className="h-4 w-4" />
            </button>
            <Link
              href={hrefForFolderId(doc.folderId)}
              className="btn-ghost p-1.5"
              aria-label="Go to containing folder"
              title="Go to containing folder"
            >
              <FolderOpen className="h-4 w-4" />
            </Link>
          </>
        ) : (
          <>
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="Download"
              onClick={() => onDownload(doc)}
            >
              <Download className="h-4 w-4" />
            </button>
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="More actions"
              onClick={(e) => onMenuTrigger?.("document", doc, { kind: "button", node: e.currentTarget })}
            >
              <MoreHorizontal className="h-4 w-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
