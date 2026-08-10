"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Check, Copy, CornerLeftUp, Folder, Inbox, Loader2, Search } from "lucide-react";
import { extensionIcon } from "@/constants/document";
import { useDebounce } from "@/hooks/useDebounce";
import { getFolderContents, searchFolders } from "@/services/folderService";
import { listDocuments } from "@/services/documentService";

// ponytail: no infinite scroll in these popups — a folder/search result beyond
// this many items just won't show. Raise it or add pagination if a tenant needs more.
const POPUP_PAGE_SIZE = 100;

// Same idea as FolderBreadcrumb (File Storage's own breadcrumb), but navigating a
// segment updates the picker's local browse state instead of the real page route —
// this breadcrumb never leaves the popup.
function PickerBreadcrumb({ breadcrumb, onNavigate }) {
  const [copied, setCopied] = useState(false);

  if (!breadcrumb || breadcrumb.length === 0) return null;

  const path = breadcrumb.map((item) => item.name).join("/");

  const copyPath = async () => {
    try {
      await navigator.clipboard.writeText(path);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard is blocked outside a secure context — nothing useful to show here.
    }
  };

  return (
    <div className="flex items-center gap-2">
      <nav aria-label="Folder path" className="flex min-w-0 flex-wrap items-center">
        {breadcrumb.map((item) => (
          <span key={item.id} className="flex items-center">
            <button
              type="button"
              onClick={() => onNavigate(item.id)}
              className="max-w-[220px] truncate rounded-md px-1.5 py-1 text-sm text-text-secondary transition-colors hover:bg-bg-elevated hover:text-text-primary"
              title={item.name}
            >
              {item.name}
            </button>
            <span className="text-sm text-text-muted">/</span>
          </span>
        ))}
      </nav>

      <button
        type="button"
        onClick={copyPath}
        className="btn-ghost p-1.5 shrink-0"
        aria-label="Copy folder path"
        title={copied ? "Copied" : `Copy "${path}"`}
      >
        {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
      </button>
    </div>
  );
}

// One row shape for parent/folder/document alike, styled to match File Storage's
// table rows (icon swatch, text sizing, row padding) but as a plain flex row —
// a modal is too narrow for File Storage's full multi-column grid, and this
// picker only ever needs an icon, a title and an extension anyway.
//
// doubleClick: folders (and the parent row) only navigate on double-click, matching
// File Storage's own folder rows — single click is reserved for document selection.
//
// dimmed: the row for an item that's currently being moved (MoveItemModal) — stays
// visible in place (same spot it'd normally show up) instead of vanishing from the
// listing, just faded and inert, like the ghosted source item Windows/Mac show
// during a drag-move.
function PickerRow({ icon: Icon, iconBg, iconColor, title, extLabel, interactive, doubleClick, onActivate, checkbox, dimmed }) {
  const active = interactive && !dimmed;
  return (
    <div
      role="row"
      className={`flex items-center gap-3 border-b border-border-default px-4 py-1 transition-colors ${
        active ? "cursor-pointer select-none hover:bg-bg-elevated/50" : ""
      } ${dimmed ? "cursor-not-allowed" : ""}`}
      onClick={active && !doubleClick ? onActivate : undefined}
      onDoubleClick={active && doubleClick ? onActivate : undefined}
      title={dimmed ? "Currently being moved" : active && doubleClick ? "Double-click to open" : undefined}
    >
      {/* opacity only on the icon/text — not the row div itself — so the row's
          own border-b stays at full strength instead of fading along with it. */}
      <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${iconBg} ${dimmed ? "opacity-40" : ""}`}>
        <Icon className={`h-3.5 w-3.5 ${iconColor}`} />
      </span>
      {title && (
        <p
          className={`min-w-0 flex-1 truncate text-xs font-medium text-text-primary ${dimmed ? "opacity-40" : ""}`}
          title={title}
        >
          {title}
        </p>
      )}
      {!title && <span className="min-w-0 flex-1" />}
      <span className={`w-14 shrink-0 text-right text-xs text-text-secondary uppercase ${dimmed ? "opacity-40" : ""}`}>
        {extLabel ?? "—"}
      </span>
      <span className="flex w-4 shrink-0 items-center justify-center">{checkbox}</span>
    </div>
  );
}

// Shared folder/document browser used inside both AttachDocumentsModal (pick
// documents to attach) and MoveItemModal (pick a destination folder).
//
// mode="attach": documents get checkboxes + a select-all checkbox (same one
// File Storage's table header uses), folders are navigation-only, and a
// keyword search box searches both (reusing the same folder/document keyword
// APIs File Storage's search bar uses).
// mode="move": nothing is selectable — folders navigate, documents are listed
// read-only for context — and there's no search box (the caller only needs to
// reach a destination folder, not filter its way there).
export default function FolderBrowserPicker({
  active,
  mode,
  documentFilter,
  movingItems,
  initialFolderId = null,
  onSelectionChange,
  onLocationChange,
}) {
  const [browseFolderId, setBrowseFolderId] = useState(initialFolderId);
  const [folderView, setFolderView] = useState(null);
  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const trimmedKeyword = debouncedKeyword.trim();
  const isSearching = mode === "attach" && trimmedKeyword.length > 0;
  const [searchResults, setSearchResults] = useState({ folders: [], documents: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedIds, setSelectedIds] = useState(new Set());

  // Reset to the caller's starting folder (root by default, or the folder it was
  // opened from for MoveItemModal) every time the popup opens.
  useEffect(() => {
    if (!active) return;
    setBrowseFolderId(initialFolderId);
    setKeyword("");
    setSelectedIds(new Set());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, initialFolderId]);

  useEffect(() => {
    if (!active) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");

    const request = isSearching
      ? Promise.all([
          listDocuments(
            { keyword: trimmedKeyword, documentStatus: "ACTIVE", size: POPUP_PAGE_SIZE },
            controller.signal
          ),
          searchFolders(trimmedKeyword, { size: POPUP_PAGE_SIZE }, controller.signal),
        ]).then(([docPage, folderPage]) => {
          if (controller.signal.aborted) return;
          setFolderView(null);
          setSearchResults({ documents: docPage?.content || [], folders: folderPage?.content || [] });
        })
      : getFolderContents(browseFolderId, { page: 0, size: POPUP_PAGE_SIZE }, controller.signal).then(
          (contents) => {
            if (controller.signal.aborted) return;
            setFolderView(contents || null);
          }
        );

    request
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [active, browseFolderId, isSearching, trimmedKeyword]);

  useEffect(() => {
    onLocationChange?.({ folderId: browseFolderId, currentFolder: folderView?.currentFolder || null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [browseFolderId, folderView]);

  useEffect(() => {
    onSelectionChange?.([...selectedIds]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedIds]);

  // Items being moved (MoveItemModal) stay in the listing wherever they'd normally
  // show up, dimmed instead of removed — see PickerRow's `dimmed`.
  const movingFolderIds = (movingItems || []).filter((it) => it.type === "folder").map((it) => it.id);
  const movingDocumentIds = (movingItems || []).filter((it) => it.type === "document").map((it) => it.id);

  const folders = isSearching ? searchResults.folders : folderView?.subfolders || [];
  // documentFilter (AttachDocumentsModal: already-attached / not-yet-ready docs)
  // no longer removes rows — same "stays put, just dimmed" treatment as
  // movingFolderIds/movingDocumentIds above. selectableDocuments is the filtered
  // subset, used only for "select all" bookkeeping below.
  const documents = isSearching ? searchResults.documents : folderView?.documents?.content || [];
  const selectableDocuments = documentFilter ? documents.filter(documentFilter) : documents;
  const hasParent = !isSearching && (folderView?.breadcrumb?.length || 0) > 1;

  const openFolder = useCallback((folder) => setBrowseFolderId(folder.id), []);
  const openParent = useCallback(() => {
    const crumb = folderView?.breadcrumb || [];
    const parent = crumb[crumb.length - 2];
    setBrowseFolderId(parent ? parent.id : null);
  }, [folderView]);

  const toggleDoc = (id) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  // Same semantics as File Storage's own header "Select all" checkbox: checked
  // once every selectable document currently listed is selected, toggling flips
  // all of them together — scoped to the current folder/search listing, same as
  // there. Dimmed (already-attached / not-ready) docs sit outside this entirely.
  const allSelected = selectableDocuments.length > 0 && selectableDocuments.every((d) => selectedIds.has(d.id));
  const toggleAllInFolder = () =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      selectableDocuments.forEach((d) => (allSelected ? next.delete(d.id) : next.add(d.id)));
      return next;
    });

  return (
    <div>
      <div className="mb-2 flex h-8 min-w-0 items-center">
        {isSearching ? (
          <span className="px-1.5 py-1 text-sm text-text-secondary">Search Results</span>
        ) : (
          <PickerBreadcrumb breadcrumb={folderView?.breadcrumb} onNavigate={setBrowseFolderId} />
        )}
      </div>

      {mode === "attach" && (
        <div className="mb-2 flex items-center gap-2">
          <div className="flex h-9 w-1/2 items-center rounded-lg border border-border-subtle bg-bg-card px-3">
            <Search className="h-4 w-4 shrink-0 text-text-muted" />
            <input
              type="search"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Search folders and documents..."
              className="ml-2 flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
            />
          </div>
          {/* Same checkbox as File Storage's own header "Select all" — ticks every
              document currently listed, same toggle-all semantics. ml-auto pushes it
              to the right; mr-4 then insets it by the same 16px as a row's own px-4,
              so it lines up in the same column as every row's checkbox below. */}
          <input
            type="checkbox"
            className="ml-auto mr-4 h-4 w-4 shrink-0 rounded border-border-default bg-bg-primary accent-accent"
            aria-label="Select all documents in this folder"
            checked={allSelected}
            onChange={toggleAllInFolder}
          />
        </div>
      )}

      {/* h-[50vh] not max-h: a fixed height regardless of item count (browsing or
          searching) so the popup doesn't resize as you navigate or type a search. */}
      <div className="h-[50vh] overflow-y-auto rounded-lg border border-border-subtle">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <div className="flex items-center justify-center gap-2 py-10 text-error">
            <AlertCircle className="h-5 w-5" />
            <span className="text-sm">{error}</span>
          </div>
        ) : folders.length === 0 && documents.length === 0 && !hasParent ? (
          <div className="flex flex-col items-center justify-center gap-2 py-10 text-text-muted">
            <Inbox className="h-5 w-5" />
            <span className="text-sm">Nothing here.</span>
          </div>
        ) : (
          <>
            {hasParent && (
              <PickerRow
                icon={CornerLeftUp}
                iconBg="bg-amber-500/10"
                iconColor="text-amber-400"
                title=""
                extLabel=""
                interactive
                doubleClick
                onActivate={openParent}
              />
            )}
            {folders.map((folder) => (
              <PickerRow
                key={`folder-${folder.id}`}
                icon={Folder}
                iconBg="bg-amber-500/10"
                iconColor="text-amber-400"
                title={folder.name}
                extLabel=""
                interactive
                doubleClick
                onActivate={() => openFolder(folder)}
                dimmed={movingFolderIds.includes(folder.id)}
              />
            ))}
            {documents.map((doc) => {
              const { Icon, bg, color } = extensionIcon(doc.extension);
              const dimmed = movingDocumentIds.includes(doc.id) || (documentFilter ? !documentFilter(doc) : false);
              const selectable = mode === "attach" && !dimmed;
              return (
                <PickerRow
                  key={`doc-${doc.id}`}
                  icon={Icon}
                  iconBg={bg}
                  iconColor={color}
                  title={doc.title}
                  extLabel={doc.extension || "—"}
                  interactive={selectable}
                  onActivate={selectable ? () => toggleDoc(doc.id) : undefined}
                  dimmed={dimmed}
                  checkbox={
                    selectable ? (
                      <input
                        type="checkbox"
                        className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
                        checked={selectedIds.has(doc.id)}
                        onChange={() => toggleDoc(doc.id)}
                        onClick={(e) => e.stopPropagation()}
                        aria-label={`Select ${doc.title}`}
                      />
                    ) : null
                  }
                />
              );
            })}
          </>
        )}
      </div>
    </div>
  );
}
