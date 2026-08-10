"use client";

import { useState } from "react";
import { CornerLeftUp, Folder, MoreHorizontal } from "lucide-react";
import { formatDateTime } from "@/utils/format";
import { gridTemplateColumns } from "./documentTableGrid";
import { useClickVsDoubleClick } from "@/hooks/useClickVsDoubleClick";

// Folders never appear alongside the semantic search results column (folder
// browsing and searching are mutually exclusive — see FileStorageView), so
// they always use the plain (non-Match) column layout.
const COLUMNS = gridTemplateColumns(false);

// Same "no data" treatment the Semantic Similarity column already uses for documents.
// Every cell carries the row divider (border-b) — unchanged from the old
// border-separate table, just on a grid cell <div> now instead of a <td>.
function EmptyCell() {
  return (
    <div role="cell" className="flex items-center border-b border-border-default px-4 py-1 text-xs">
      <span className="text-text-muted">—</span>
    </div>
  );
}

// Parent-folder row: navigates to the parent folder on double-click, like a file
// manager. It deliberately has no checkbox and no actions — it isn't a real row
// of data — and every other column is left empty rather than showing a placeholder.
// Also a drag-and-drop target (targetFolderId/onDropMove) so items can be dragged
// up a level without leaving the current folder first.
export function ParentFolderRow({ onOpen, targetFolderId, onDropMove }) {
  const [dragOver, setDragOver] = useState(false);

  return (
    // min-h-[41px]: every other row's tallest cell is its Actions button, which
    // auto-sizes this grid's single row track to 41px (CSS grid's default
    // align-content:stretch backfills a min-height onto an auto row track, which
    // then stretches every cell via align-items:stretch — the same mechanism
    // that sizes the row from a tall cell's own content). This row has no
    // button, only a 24px icon, so it needs the explicit min-height to match.
    <div
      role="row"
      className={`grid min-h-[41px] bg-bg-primary cursor-pointer select-none transition-colors hover:bg-bg-elevated/50 ${
        dragOver ? "bg-accent/10 ring-1 ring-inset ring-accent" : ""
      }`}
      style={{ gridTemplateColumns: COLUMNS }}
      onDoubleClick={onOpen}
      title="Double-click to go to the parent folder"
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        const raw = e.dataTransfer.getData("application/json");
        if (!raw) return;
        onDropMove?.(targetFolderId, JSON.parse(raw));
      }}
    >
      <div role="cell" className="flex items-center border-b border-border-default px-4 py-1">
        <span className="flex h-6 w-6 items-center justify-center rounded-md bg-amber-500/10">
          <CornerLeftUp className="h-3.5 w-3.5 text-amber-400" />
        </span>
      </div>
      <EmptyCell />
      <EmptyCell />
      <EmptyCell />
      <EmptyCell />
      <EmptyCell />
      <EmptyCell />
      <EmptyCell />
    </div>
  );
}

// Click selects (Finder/Explorer semantics — see FileStorageView's selectRow:
// plain click selects only this row, Cmd/Ctrl-click toggles it, Shift-click
// selects a range); double-click opens the folder. Draggable itself (drags the
// whole current selection if this row is part of it, otherwise just itself —
// see buildDragPayload), and a drop target for moving other rows into it.
export default function FolderRow({
  folder,
  onOpen,
  selected,
  onToggle,
  onRowSelect,
  buildDragPayload,
  onDropMove,
  onMenuTrigger,
  isContextMenuTarget,
}) {
  const [dragOver, setDragOver] = useState(false);
  // Delays the select-on-click so a double-click (open the folder) doesn't
  // flash a selection highlight right before the folder navigation remounts
  // the page — see useClickVsDoubleClick.
  const { onClick, onDoubleClick } = useClickVsDoubleClick(
    (mods) => onRowSelect?.("folder", folder.id, mods),
    () => onOpen(folder)
  );

  return (
    <div
      role="row"
      draggable
      className={`grid cursor-pointer select-none transition-colors hover:bg-bg-elevated/50 ${
        dragOver
          ? "bg-accent/10 ring-1 ring-inset ring-accent"
          : isContextMenuTarget
          ? "bg-bg-elevated/70"
          : "bg-bg-primary"
      }`}
      style={{ gridTemplateColumns: COLUMNS }}
      onClick={onClick}
      onDoubleClick={onDoubleClick}
      onContextMenu={(e) => {
        e.preventDefault();
        onMenuTrigger?.("folder", folder, { kind: "point", x: e.clientX, y: e.clientY });
      }}
      onDragStart={(e) => {
        e.dataTransfer.effectAllowed = "move";
        e.dataTransfer.setData("application/json", JSON.stringify(buildDragPayload?.("folder", folder.id) || []));
      }}
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        const raw = e.dataTransfer.getData("application/json");
        if (!raw) return;
        onDropMove?.(folder.id, JSON.parse(raw));
      }}
      title="Double-click to open"
    >
      <div role="cell" className="min-w-0 flex items-center gap-3 border-b border-border-default px-4 py-1">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-500/10">
          <Folder className="h-3.5 w-3.5 text-amber-400" />
        </span>
        {/* No max-width cap: the File Name column expands to fill the table,
            so the folder name only truncates when it genuinely exceeds the
            available width. */}
        <div className="min-w-0">
          <p className="truncate text-xs font-medium text-text-primary" title={folder.name}>
            {folder.name}
          </p>
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
          onChange={() => onToggle(folder.id)}
          aria-label={`Select ${folder.name}`}
        />
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap"
      >
        {formatDateTime(folder.createdAt)}
      </div>

      <div role="cell" className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary uppercase">
        Folder
      </div>

      <EmptyCell />
      <EmptyCell />
      <EmptyCell />

      <div
        role="cell"
        className="flex items-center justify-end gap-1 border-b border-border-default px-4 py-1"
        onClick={(e) => e.stopPropagation()}
        onDoubleClick={(e) => e.stopPropagation()}
      >
        {/* No Download button: there is no folder download endpoint. The "..." opens
            the table's one context menu (see DocumentTable's contextMenu state), so
            right-click and this button are just two triggers for the same menu. */}
        <button
          type="button"
          className="btn-ghost p-1.5"
          aria-label="More actions"
          onClick={(e) => onMenuTrigger?.("folder", folder, { kind: "button", node: e.currentTarget })}
        >
          <MoreHorizontal className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
