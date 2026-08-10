"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import SearchBar from "./SearchBar";
import FilterPopover from "./FilterPopover";
import DocumentTable from "./DocumentTable";
import FolderBreadcrumb from "./FolderBreadcrumb";
import FolderNameModal from "./FolderNameModal";
import UploadDocumentModal from "./UploadDocumentModal";
import UploadVersionModal from "./UploadVersionModal";
import EditMetadataModal from "./EditMetadataModal";
import ConfirmDialog from "./ConfirmDialog";
import EvidenceDialog from "./EvidenceDialog";
import MoveItemModal from "./MoveItemModal";
import Toast from "@/components/ui/Toast";
import { useDebounce } from "@/hooks/useDebounce";
import { useSemanticSearch } from "@/hooks/useSemanticSearch";
import { dateInputToIso } from "@/utils/format";
import { folderSegment, folderIdFromSegment, hrefForBreadcrumb, hrefForSegments } from "@/utils/folderPath";
import { useRouter, useSearchParams } from "next/navigation";
import {
  listDocuments,
  getDocument,
  deleteDocument,
  restoreDocument,
  downloadCurrentVersion,
  moveDocument,
} from "@/services/documentService";
import { deleteFolder, getFolderContents, searchFolders, moveFolder } from "@/services/folderService";

// Document page size for infinite scroll — folders are no longer paginated,
// they always come back in full (see FolderServiceImpl.getFolderContents).
const PAGE_SIZE = 20;
const SEMANTIC_TOP_K = 50;
// ponytail: search scans the first 200 documents (matching current filters)
// client-side rather than a server-side "search within these IDs" endpoint —
// raise this or push the intersection server-side if a tenant outgrows it.
const SEARCH_SCAN_SIZE = 200;
// ponytail: keyword folder matches aren't paginated in the UI (unlike documents,
// which use infinite scroll) — this just caps how many can show at once. Raise it
// or add folder pagination if a tenant has more matching folders than this.
const KEYWORD_FOLDER_SCAN_SIZE = 200;

// Module-level cache of folder identities (id -> name, plus the root entry),
// shared by every FileStorageView instance across mounts. The backend
// breadcrumb is authoritative but only arrives with the folder-contents fetch;
// navigating to a new folder remounts this view (the folders route keys it by
// folder id), so without a fallback the whole path would blink out and render
// from scratch when the fetch lands. The cache lets the path render instantly
// from the URL + names the UI has already seen — existing segments stay put and
// only the new one appears.
const folderNameCache = new Map();
let cachedRootEntry = null;

function rememberBreadcrumb(breadcrumb) {
  if (!breadcrumb?.length) return;
  if (!cachedRootEntry) cachedRootEntry = breadcrumb[0];
  for (const item of breadcrumb) folderNameCache.set(item.id, item.name);
}

// Fallback path built from the URL (folderPath segments) while the new folder's
// contents — and therefore its authoritative breadcrumb — are still loading.
// Names come from the cache; a folder the UI has never seen falls back to its
// URL slug (the trailing "-<id>" is stripped), which the backend corrects as
// soon as the fetch resolves.
function breadcrumbFromPath(folderPath) {
  const crumbs = cachedRootEntry ? [cachedRootEntry] : [];
  for (const segment of folderPath) {
    const id = folderIdFromSegment(segment);
    if (id == null) continue;
    crumbs.push({
      id,
      name: folderNameCache.get(id) ?? segment.replace(/-\d+$/, ""),
    });
  }
  return crumbs;
}

// The last successfully loaded view, kept across mounts so a remount can render
// it instantly instead of blinking to the initial-load skeleton. Folder
// navigation remounts this whole component (the folders route keys it by folder
// id), and the cache is scoped by folderId: only a cached view that belongs to
// the folder currently being opened is reused. That keeps two things true at
// once — navigating BACK to the last-loaded folder shows its real content
// instantly, and navigating to a never-loaded folder never flashes the PREVIOUS
// folder's rows (which would read as wrong data). null means "no view loaded
// yet": that is the only time the skeleton shows.
let lastViewCache = null; // { folderId, documents, totalElements, folderView, keywordFolders }

const INITIAL_FILTERS = {
  sort: "newest",
  documentType: "",
  extension: "",
  fromDate: "",
  toDate: "",
  status: "",
  documentStatus: "ACTIVE",
};

// Folder browsing shows one folder's contents; searching and filtering span every
// folder, so any active filter drops back to the flat document list (the folder
// contents endpoint supports neither filters nor deleted documents).
function hasActiveFilters(filters) {
  return (
    filters.sort !== INITIAL_FILTERS.sort ||
    filters.documentStatus !== INITIAL_FILTERS.documentStatus ||
    Boolean(
      filters.documentType ||
        filters.extension ||
        filters.fromDate ||
        filters.toDate ||
        filters.status
    )
  );
}

// folderPath / folderId come from the URL (/file-storage = root, /file-storage/folders/…
// = a specific folder), so browser Back/Forward moves between folders instead of
// leaving the page, and a pasted folder URL loads that folder directly.
export default function FileStorageView({ folderPath = [], folderId = null }) {
  // Deep link from the conversation "deleted attachment" warning: /file-storage?status=DELETED
  // pre-applies the Status filter so the table opens already showing deleted documents.
  const searchParams = useSearchParams();
  const initialDocumentStatus = searchParams.get("status") === "DELETED" ? "DELETED" : "ACTIVE";

  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const trimmedKeyword = debouncedKeyword.trim();
  const isSearching = trimmedKeyword.length > 0;

  // "semantic" searches documents by meaning (existing vector search, unchanged);
  // "keyword" searches folder/document names via the existing LIKE-based APIs.
  const [searchMode, setSearchMode] = useState("semantic");
  const isSemanticSearching = isSearching && searchMode === "semantic";
  const isKeywordSearching = isSearching && searchMode === "keyword";
  // Cached view reuse is scoped to the folder being opened (lastViewCache is
  // saved alongside the folderId it was fetched for). Navigating to a folder the
  // cache doesn't belong to starts empty — no skeleton (that's for the very
  // first load only), no previous folder's rows, just the compact loading bar —
  // and swaps in the real content when the fetch lands. Navigating BACK to the
  // last-loaded folder restores its content instantly.
  const cacheMatchesFolder = lastViewCache?.folderId === folderId;
  const [keywordFolders, setKeywordFolders] = useState(() => (cacheMatchesFolder ? lastViewCache.keywordFolders ?? [] : []));

  const [filters, setFilters] = useState({ ...INITIAL_FILTERS, documentStatus: initialDocumentStatus });

  const [folderView, setFolderView] = useState(() => (cacheMatchesFolder ? lastViewCache.folderView ?? null : null));
  const folderMode = !isSearching && !hasActiveFilters(filters);

  // Accumulated document list — infinite scroll appends pages here instead of
  // replacing the current page. totalElements comes from the backend Page and
  // stays fixed for the life of this view (folder/filter/sort/search combo).
  const [documents, setDocuments] = useState(() => (cacheMatchesFolder ? lastViewCache.documents ?? [] : []));
  const [totalElements, setTotalElements] = useState(() => (cacheMatchesFolder ? lastViewCache.totalElements ?? 0 : 0));
  // Only ever true on the very first mount of this view (no cached view yet) —
  // that's what drives the initial-load skeleton. Every later mount (folder
  // navigation remounts this component) keeps it false: navigating shows either
  // the cached view (same folder) or the compact loading bar (new folder),
  // never the skeleton again.
  const [loading, setLoading] = useState(() => !lastViewCache);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  // abortRef holds the controller for the current view's requests (initial +
  // load-more), so switching folder/filter/sort/search cancels stale ones.
  // pageRef/loadingMoreRef are refs (not state) because loadMoreDocuments needs
  // synchronous re-entrancy guarding and the next-page index right away.
  const abortRef = useRef(null);
  const pageRef = useRef(0);
  const loadingMoreRef = useRef(false);

  const [selectedIds, setSelectedIds] = useState(new Set());
  const [selectedFolderIds, setSelectedFolderIds] = useState(new Set());
  const [toast, setToast] = useState(null);

  const [folderNameTarget, setFolderNameTarget] = useState(null); // folder | { create: true }
  const [uploadOpen, setUploadOpen] = useState(false);
  const [versionTarget, setVersionTarget] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
  const [deleteTargets, setDeleteTargets] = useState(null); // [{ type: "document" | "folder", id, title }]
  const [deleting, setDeleting] = useState(false);
  const [restoreTarget, setRestoreTarget] = useState(null);
  const [restoring, setRestoring] = useState(false);
  const [evidenceTarget, setEvidenceTarget] = useState(null);
  const [navigating, setNavigating] = useState(false);
  const [moveTargets, setMoveTargets] = useState(null); // [{ type: "document" | "folder", id, title }]

  // Anchor for shift-click range selection — the last row clicked without Shift,
  // as { type, id }. Range selection walks orderedItems (below) between this and
  // the row just shift-clicked.
  const lastClickedRef = useRef(null);

  const router = useRouter();

  const notify = useCallback((type, text) => setToast({ type, text }), []);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  // Semantic search only fires while that mode is active and there's a keyword.
  const {
    results: semanticHits,
    loading: searchLoading,
    error: searchError,
  } = useSemanticSearch(isSemanticSearching ? trimmedKeyword : "", SEMANTIC_TOP_K);

  // documentId -> { bestScore, matches: SemanticSearchResult[] } (matches stay
  // score-sorted since semanticHits is already score-sorted by the backend).
  const semanticByDocument = useMemo(() => {
    const map = new Map();
    for (const hit of semanticHits) {
      const entry = map.get(hit.documentId);
      if (!entry) {
        map.set(hit.documentId, { bestScore: hit.score, matches: [hit] });
      } else {
        entry.matches.push(hit);
        if (hit.score > entry.bestScore) entry.bestScore = hit.score;
      }
    }
    return map;
  }, [semanticHits]);

  // Builds the query params for /documents, shared between the initial fetch
  // and each subsequent infinite-scroll page so they stay in lockstep.
  const buildListParams = useCallback(
    (pageIndex, size) => ({
      keyword: isKeywordSearching ? trimmedKeyword : undefined,
      sort: filters.sort || undefined,
      documentType: filters.documentType || undefined,
      extension: filters.extension || undefined,
      fromDate: dateInputToIso(filters.fromDate, false),
      toDate: dateInputToIso(filters.toDate, true),
      status: filters.status || undefined,
      documentStatus: filters.documentStatus || undefined,
      page: pageIndex,
      size,
    }),
    [filters, isKeywordSearching, trimmedKeyword]
  );

  // Identity of the currently displayed list (folder, search mode, keyword and
  // filters). When it changes, the table is about to show a different list, so
  // DocumentTable resets its scroll to the top instead of opening the new view
  // mid-scroll. reloadKey is deliberately excluded: deleting/uploading/renaming
  // reloads the SAME list and should keep the user's place in it.
  const scrollResetKey = useMemo(
    () =>
      JSON.stringify([
        folderMode ? `folder:${folderId}` : "flat",
        isSemanticSearching ? "semantic" : isKeywordSearching ? "keyword" : "list",
        trimmedKeyword,
        filters,
      ]),
    [folderMode, folderId, isSemanticSearching, isKeywordSearching, trimmedKeyword, filters]
  );

  // The view the loaded data belongs to. viewTokenRef guards load-more so a
  // page fetched for a previous view can never be appended into the new one,
  // even in the window before the old request is aborted.
  const viewTokenRef = useRef(scrollResetKey);

  // The breadcrumb to show above the table, independent of the table's loading
  // state. The backend breadcrumb is used only when it belongs to the folder
  // currently open (its last crumb is that folder); while a navigation is in
  // flight (folderView absent or still the previous folder's), the URL path +
  // name cache renders the same existing segments plus the newly appended one,
  // so the breadcrumb never blinks out and reloads.
  const breadcrumb = useMemo(() => {
    const crumbs = folderView?.breadcrumb;
    // The backend breadcrumb is authoritative only when it belongs to the folder
    // currently open. Root (folderId == null) has a breadcrumb of exactly one
    // crumb (the root folder itself) — any longer one is a stale previous
    // folder's, which would otherwise show the wrong path while root loads.
    const isFresh =
      crumbs?.length && (folderId == null ? crumbs.length === 1 : crumbs[crumbs.length - 1].id === folderId);
    return isFresh ? crumbs : breadcrumbFromPath(folderPath);
  }, [folderView, folderPath, folderId]);

  // Every folder name the UI has seen (breadcrumb, listed subfolders, keyword-
  // search folder rows) is cached, so a navigation to any of them renders its
  // path instantly instead of waiting for the folder-contents fetch.
  useEffect(() => {
    if (folderView) {
      rememberBreadcrumb(folderView.breadcrumb);
      for (const sub of folderView.subfolders || []) folderNameCache.set(sub.id, sub.name);
    }
    for (const f of keywordFolders || []) folderNameCache.set(f.id, f.name);
  }, [folderView, keywordFolders]);

  // Remembers the latest loaded folder view so the next mount (folder navigation
  // remounts this component) can render it instantly — that's what lets a
  // navigation back to the same folder show its real content instead of the
  // skeleton. Only folder-browse views are cached: a search/filter view's
  // keyword and filter state resets on remount anyway, so restoring its (possibly
  // filtered or scanned) documents would flash the wrong content under a folder
  // view when navigating back to the same folder. Skipped while the very first
  // load is still in flight (nothing to cache yet) and while the folder's
  // contents haven't landed (folderMode with no folderView) — caching that
  // blank state would overwrite the previous good view and rob a rapid Back of
  // its instant restore.
  useEffect(() => {
    if (!folderMode || loading || !folderView) return;
    lastViewCache = { folderId, documents, totalElements, folderView, keywordFolders };
  }, [loading, folderId, documents, totalElements, folderView, keywordFolders, folderMode]);

  // Resets the view: fetches the folder (full, once) and/or the first document
  // page. Changing folder/filters/sort/search re-runs this and its cleanup
  // aborts whatever the previous view still had in flight (including a
  // load-more request), so a late response can't append into the new view.
  //
  // `loading` is deliberately NOT in the deps: it only matters on the very
  // first run (skeleton), and it flips to false via the fetch's finally without
  // any dep changing — adding it would re-run this effect on every load
  // completion and double-fetch. Each run's closure captures the fresh value.
  useEffect(() => {
    const controller = new AbortController();
    abortRef.current = controller;
    pageRef.current = 0;
    viewTokenRef.current = scrollResetKey;
    setLoadingMore(false);
    setError("");

    // First load only (no cached view yet): blank the table so the skeleton
    // shows. Every later load — folder navigation, reload, search, filter —
    // starts from the state the view settled into (cached rows when returning
    // to a folder whose view is cached, otherwise empty with the compact
    // loading bar) and swaps in the fresh response when it lands, so the table
    // never blinks out and never shows another folder's data.
    if (loading) {
      setDocuments([]);
      setTotalElements(0);
    }

    const request = folderMode
      ? getFolderContents(folderId, { page: 0, size: PAGE_SIZE }, controller.signal).then((contents) => {
          if (controller.signal.aborted) return;
          setFolderView(contents || null);
          setDocuments(contents?.documents?.content || []);
          setTotalElements(contents?.documents?.totalElements || 0);
          pageRef.current = 1;
        })
      : isKeywordSearching
      ? Promise.all([
          listDocuments(buildListParams(0, PAGE_SIZE), controller.signal),
          searchFolders(trimmedKeyword, { page: 0, size: KEYWORD_FOLDER_SCAN_SIZE }, controller.signal),
        ]).then(([pageData, folderPage]) => {
          if (controller.signal.aborted) return;
          setFolderView(null);
          setDocuments(pageData?.content || []);
          setTotalElements(pageData?.totalElements || 0);
          setKeywordFolders(folderPage?.content || []);
          pageRef.current = 1;
        })
      : listDocuments(
          buildListParams(0, isSemanticSearching ? SEARCH_SCAN_SIZE : PAGE_SIZE),
          controller.signal
        ).then((pageData) => {
          if (controller.signal.aborted) return;
          setFolderView(null);
          setDocuments(pageData?.content || []);
          setTotalElements(pageData?.totalElements || 0);
          pageRef.current = 1;
        });

    request
      .then(() => {
        if (!controller.signal.aborted) {
          setSelectedIds(new Set());
          setSelectedFolderIds(new Set());
        }
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load documents");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [
    filters,
    reloadKey,
    isSemanticSearching,
    isKeywordSearching,
    trimmedKeyword,
    folderMode,
    folderId,
    buildListParams,
    scrollResetKey,
  ]);

  // Fetches the next document page and appends it — called by DocumentTable's
  // scroll sentinel. Guarded by a ref (not state) so a fast double-trigger from
  // the IntersectionObserver can't fire two requests before state catches up.
  const loadMoreDocuments = useCallback(async () => {
    if (loadingMoreRef.current) return;
    const controller = abortRef.current;
    if (!controller) return;
    const pageToFetch = pageRef.current;
    // Page 0 is always the view's own initial fetch (the view-reset effect
    // resets pageRef to 0 and refetches it). Navigating back to a folder whose
    // view is cached reuses those rows immediately, so its scroll sentinel can
    // still be visible while that page-0 fetch is in flight — bail here instead
    // of re-fetching page 0 and appending duplicates on top of the cached rows.
    if (pageToFetch === 0) return;
    // The view this page was requested for. If the user changes folder/search/
    // filter while the request is in flight, the response is discarded — the
    // abort alone can race a response that already resolved.
    const viewAtStart = viewTokenRef.current;

    loadingMoreRef.current = true;
    setLoadingMore(true);
    try {
      if (folderMode) {
        const contents = await getFolderContents(
          folderId,
          { page: pageToFetch, size: PAGE_SIZE },
          controller.signal
        );
        if (controller.signal.aborted || viewAtStart !== viewTokenRef.current) return;
        setDocuments((prev) => [...prev, ...(contents?.documents?.content || [])]);
      } else {
        const pageData = await listDocuments(buildListParams(pageToFetch, PAGE_SIZE), controller.signal);
        if (controller.signal.aborted || viewAtStart !== viewTokenRef.current) return;
        setDocuments((prev) => [...prev, ...(pageData?.content || [])]);
      }
      pageRef.current = pageToFetch + 1;
    } catch (err) {
      if (!controller.signal.aborted && err.name !== "AbortError") {
        notify("error", err.message || "Failed to load more documents");
      }
    } finally {
      loadingMoreRef.current = false;
      // Unconditional: even when aborted by a view change, the "loading more"
      // indicator must clear — otherwise the next view shows a stuck spinner.
      setLoadingMore(false);
    }
  }, [folderMode, folderId, buildListParams, notify]);

  // A folder can be reached by id alone (the "go to folder" action on a search
  // result); once the breadcrumb arrives, rewrite the URL to its full path so the
  // address bar and the breadcrumb always agree. replace, not push — this is the
  // same location, not a navigation.
  useEffect(() => {
    const crumbs = folderView?.breadcrumb;
    if (!crumbs?.length) return;
    // Only rewrite when this breadcrumb belongs to the folder currently open —
    // folderView is null for any other folder (cache is folder-scoped), and on
    // root a breadcrumb longer than the single root crumb is always stale. A
    // stale breadcrumb would rewrite the URL back to ITS folder and bounce the
    // user straight back — the root case previously slipped past the folderId
    // guard because null skips the id comparison.
    if (folderId != null && crumbs[crumbs.length - 1].id !== folderId) return;
    if (folderId == null && crumbs.length > 1) return;
    const canonical = hrefForBreadcrumb(crumbs);
    if (canonical !== window.location.pathname) router.replace(canonical, { scroll: false });
  }, [folderView, folderId, router]);

  // While semantic searching, the accumulated list is replaced by semantic
  // matches, ranked by best score. Search already fetches its full bounded
  // scan (SEARCH_SCAN_SIZE) in one shot, so there's nothing left to load more
  // of. Keyword search skips this entirely — the backend already filtered
  // `documents` by keyword, so it's shown (and paginated) as-is.
  const matchedDocuments = useMemo(() => {
    if (!isSemanticSearching) return null;
    return documents
      .filter((doc) => semanticByDocument.has(doc.id))
      .map((doc) => ({ ...doc, semanticScore: semanticByDocument.get(doc.id).bestScore }))
      .sort((a, b) => b.semanticScore - a.semanticScore);
  }, [isSemanticSearching, documents, semanticByDocument]);

  const tableDocuments = isSemanticSearching ? matchedDocuments : documents;
  const tableTotalElements = isSemanticSearching ? matchedDocuments.length : totalElements;
  const hasMoreDocuments = tableDocuments.length < tableTotalElements;
  const tableFolders = folderMode ? folderView?.subfolders || [] : isKeywordSearching ? keywordFolders : [];

  // Called by the filter popover only when the user commits (Apply Filters /
  // Clear All) — the draft is edited inside the popover, filtering logic and
  // request building below are untouched.
  const handleApplyFilters = (nextFilters) => {
    setFilters(nextFilters);
  };
  const onKeywordChange = (value) => {
    setKeyword(value);
  };
  const onToggleSearchMode = () => {
    setSearchMode((mode) => (mode === "semantic" ? "keyword" : "semantic"));
  };

  // router.push, not local state: each folder gets a real history entry, so Back
  // returns to the parent folder instead of leaving File Storage entirely.
  const openFolder = (folder) => router.push(hrefForSegments([...folderPath, folderSegment(folder)]));
  const openParentFolder = () => router.push(hrefForSegments(folderPath.slice(0, -1)));

  const toggleRow = (id) => {
    lastClickedRef.current = { type: "document", id };
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const toggleFolderRow = (id) => {
    lastClickedRef.current = { type: "folder", id };
    setSelectedFolderIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  // Folders first, then documents — the same order DocumentTable renders them in,
  // so shift-click range selection walks the same sequence the user sees.
  const orderedItems = useMemo(
    () => [
      ...tableFolders.map((f) => ({ type: "folder", id: f.id })),
      ...tableDocuments.map((d) => ({ type: "document", id: d.id })),
    ],
    [tableFolders, tableDocuments]
  );

  // Finder/Explorer-style click semantics for a row body (not its checkbox):
  // plain click selects only this row, Cmd/Ctrl-click toggles it into/out of the
  // current selection, Shift-click selects the contiguous range from the last
  // plain/Cmd click through this row.
  const selectRow = (type, id, event) => {
    if (event.shiftKey && lastClickedRef.current) {
      const anchorIdx = orderedItems.findIndex(
        (it) => it.type === lastClickedRef.current.type && it.id === lastClickedRef.current.id
      );
      const clickedIdx = orderedItems.findIndex((it) => it.type === type && it.id === id);
      if (anchorIdx !== -1 && clickedIdx !== -1) {
        const [start, end] = anchorIdx < clickedIdx ? [anchorIdx, clickedIdx] : [clickedIdx, anchorIdx];
        const range = orderedItems.slice(start, end + 1);
        setSelectedFolderIds(new Set(range.filter((it) => it.type === "folder").map((it) => it.id)));
        setSelectedIds(new Set(range.filter((it) => it.type === "document").map((it) => it.id)));
        return;
      }
    }

    if (event.metaKey || event.ctrlKey) {
      type === "document" ? toggleRow(id) : toggleFolderRow(id);
      return;
    }

    // Plain click: select only this row.
    lastClickedRef.current = { type, id };
    setSelectedFolderIds(type === "folder" ? new Set([id]) : new Set());
    setSelectedIds(type === "document" ? new Set([id]) : new Set());
  };

  // Drag payload for a row: the whole current selection if the dragged row is
  // part of it (multi-item drag), otherwise just that one row.
  const buildDragPayload = (type, id) => {
    const isSelected = type === "document" ? selectedIds.has(id) : selectedFolderIds.has(id);
    if (isSelected && selectedIds.size + selectedFolderIds.size > 1) {
      return [
        ...[...selectedFolderIds].map((fid) => ({ type: "folder", id: fid })),
        ...[...selectedIds].map((did) => ({ type: "document", id: did })),
      ];
    }
    return [{ type, id }];
  };

  // The current selection, shaped for MoveItemModal (folders first, then
  // documents, each carrying its title for the modal's header/exclusion list).
  const selectionAsItems = () => [
    ...tableFolders.filter((f) => selectedFolderIds.has(f.id)).map((f) => ({ type: "folder", id: f.id, title: f.name })),
    ...tableDocuments.filter((d) => selectedIds.has(d.id)).map((d) => ({ type: "document", id: d.id, title: d.title })),
  ];

  // Target(s) for a single row's Move/Delete action (row menu, right-click menu):
  // the whole current selection if this row is part of a multi-selection (so
  // acting works the same whether triggered by the toolbar button, drag, or a
  // row's own menu), otherwise just this row. Shared by both actions — "am I
  // acting on the selection or just this row" is the same question either way.
  const resolveSelectionTargets = (type, id, title) => {
    const isSelected = type === "document" ? selectedIds.has(id) : selectedFolderIds.has(id);
    if (isSelected && selectedIds.size + selectedFolderIds.size > 1) return selectionAsItems();
    return [{ type, id, title }];
  };

  // Shared by MoveItemModal's submit button and drag-and-drop — moves a batch of
  // typed items to one destination folder. No bulk endpoint on the backend, so
  // this is one request per item, same pattern as bulk delete below.
  const moveItems = useCallback(async (items, targetFolderId) => {
    await Promise.all(
      items.map((it) => (it.type === "folder" ? moveFolder(it.id, targetFolderId) : moveDocument(it.id, targetFolderId)))
    );
  }, []);

  const handleDropMove = async (targetFolderId, payload) => {
    // A folder dropped on itself is a no-op, not an error — drop it silently.
    const items = payload.filter((it) => !(it.type === "folder" && it.id === targetFolderId));
    if (items.length === 0) return;
    try {
      await moveItems(items, targetFolderId);
      notify("success", `Moved ${items.length} item(s)`);
      reload();
    } catch (err) {
      notify("error", err.message || "Move failed");
    }
  };

  // Drop target for the ".." parent row (drag-up-a-level) — same id the
  // breadcrumb's own parent-navigation link resolves to. Derived from the
  // display breadcrumb memo, not folderView: while a folder's contents are
  // loading (folderView still null), the memo falls back to the URL path, so the
  // parent target is already correct instead of dangling until the fetch lands.
  const parentFolderId = breadcrumb.length > 1 ? breadcrumb[breadcrumb.length - 2].id : null;

  // Modal target folder: the folder being opened (folderId, straight from the
  // URL) once a navigation is in flight, and the authoritative currentFolder
  // (which carries root's real id on the root page) once its contents load.
  const modalFolderId = folderView?.currentFolder?.id ?? folderId;

  // A folder navigation is "in flight" when browsing a folder whose contents
  // haven't arrived yet. That state deliberately shows nothing but the blank
  // table (no initial-load skeleton — that's reserved for the first-ever mount,
  // and no indicator pinned to the table's top border, which read as a blinking
  // blue focus ring) and simply suppresses the "No documents found" empty state
  // until the new folder's rows arrive. The table is never the previous
  // folder's rows.
  const navigationLoading = folderMode && !folderView && !loading && !error;

  // ConfirmDialog copy for deleteTargets — worded for whatever mix of
  // folders/documents is actually being deleted (single item, all-one-type, or
  // mixed), instead of a generic "N items" for every case.
  const deleteDialogText = (() => {
    const total = deleteTargets?.length || 0;
    if (total === 0) return { title: "", message: "" };
    if (total === 1) {
      const [item] = deleteTargets;
      const isFolder = item.type === "folder";
      return {
        title: `Delete ${isFolder ? "folder" : "document"}`,
        message: `Delete "${item.title}"? ${isFolder ? "Everything inside it is moved to the trash too. " : ""}This cannot be undone.`,
      };
    }
    const folderCount = deleteTargets.filter((it) => it.type === "folder").length;
    if (folderCount === 0) {
      return { title: `Delete ${total} documents`, message: `Delete ${total} documents? This cannot be undone.` };
    }
    if (folderCount === total) {
      return {
        title: `Delete ${total} folders`,
        message: `Delete ${total} folders? Everything inside them is moved to the trash too. This cannot be undone.`,
      };
    }
    return {
      title: `Delete ${total} items`,
      message: `Delete ${total} item(s)? Any folders are deleted along with everything inside them. This cannot be undone.`,
    };
  })();

  // Folders and documents share one "select all" checkbox — everything currently
  // in the table (both types) toggles together.
  const toggleAll = () => {
    const allSelected =
      (tableDocuments.length > 0 || tableFolders.length > 0) &&
      tableDocuments.every((d) => selectedIds.has(d.id)) &&
      tableFolders.every((f) => selectedFolderIds.has(f.id));
    if (allSelected) {
      setSelectedIds(new Set());
      setSelectedFolderIds(new Set());
    } else {
      setSelectedIds(new Set(tableDocuments.map((d) => d.id)));
      setSelectedFolderIds(new Set(tableFolders.map((f) => f.id)));
    }
  };

  const onDownload = async (doc) => {
    try {
      await downloadCurrentVersion(doc.id);
    } catch (err) {
      notify("error", err.message || "Download failed");
    }
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await Promise.all(
        deleteTargets.map((it) => (it.type === "folder" ? deleteFolder(it.id) : deleteDocument(it.id)))
      );
      notify(
        "success",
        deleteTargets.length === 1
          ? `${deleteTargets[0].type === "folder" ? "Folder" : "Document"} deleted`
          : `Deleted ${deleteTargets.length} item(s)`
      );
      setDeleteTargets(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  const confirmRestore = async () => {
    setRestoring(true);
    try {
      await restoreDocument(restoreTarget.id);
      notify("success", "Document restored");
      setRestoreTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Restore failed");
    } finally {
      setRestoring(false);
    }
  };

  const navigateToFeature = useCallback(
    async (href) => {
      const ids = [...selectedIds];
      if (ids.length === 0) return;
      setNavigating(true);
      try {
        // Resolve current version for each selected document (same pattern as AttachDocumentsModal).
        const details = await Promise.all(ids.map((id) => getDocument(id)));
        const attachDocs = ids
          .map((id, i) => {
            const versionId = details[i]?.currentVersion?.versionId;
            if (versionId == null) return null;
            const listItem = documents.find((doc) => doc.id === id);
            return {
              documentId: id,
              documentVersionId: versionId,
              documentTitle: listItem?.title,
              versionNumber: details[i].currentVersion.versionNumber,
            };
          })
          .filter(Boolean);

        if (attachDocs.length === 0) {
          notify("error", "Could not resolve document versions. Please try again.");
          return;
        }

        sessionStorage.setItem("file-storage-attach", JSON.stringify(attachDocs));
        router.push(href);
      } catch (err) {
        notify("error", err.message || "Failed to prepare documents. Please try again.");
      } finally {
        setNavigating(false);
      }
    },
    [selectedIds, documents, notify, router]
  );

  return (
    // flex flex-col + min-h-0 + overflow-hidden: this box is pinned to exactly the
    // height the root layout leaves below the nav bar, instead of growing with row
    // count — DocumentTable (flex-1 min-h-0) then fills whatever's left below the
    // search bar and breadcrumb row and scrolls its own rows (and loads more) internally.
    // overscroll-y-contain: defense in depth alongside DocumentTable's own
    // overscroll-y-contain — if this box ever ends up a sub-pixel taller than the
    // root layout's scroll wrapper (font metrics, scrollbar reflow, zoom), this stops
    // that wrapper from chaining a *vertical* scroll into itself and dragging this
    // whole page section (sticky table header included) instead of clipping the
    // excess. Y-only (not the old `overscroll-contain`, which contains both axes):
    // containing the X axis too ate the two-finger horizontal swipe browsers use
    // for back/forward navigation, which every other page still has.
    <main className="mx-auto flex min-h-0 w-full max-w-[1440px] flex-1 flex-col overflow-hidden overscroll-y-contain px-4 pt-2 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5 shrink-0">
        <SearchBar
          value={keyword}
          onChange={onKeywordChange}
          mode={searchMode}
          onToggleMode={onToggleSearchMode}
          leftSlot={
            <FilterPopover
              filters={filters}
              defaults={INITIAL_FILTERS}
              active={hasActiveFilters(filters)}
              onApply={handleApplyFilters}
            />
          }
        />
      </div>

      {/* Same slot as the folder breadcrumb, kept mounted while searching (either
          mode) so the header height doesn't jump between browsing and search —
          and now also while a folder view is loading its contents, so the table
          doesn't shift down the moment the breadcrumb arrives (the backend always
          returns a breadcrumb, root included). h-8 pins all branches to the
          breadcrumb's height (nav + copy button) regardless of which renders. */}
      {(folderView || isSearching || folderMode) && (
        <div className="mb-4 flex h-8 min-w-0 shrink-0 items-center">
          {isSearching ? (
            <span className="px-1.5 py-1 text-sm text-text-secondary">Search Results</span>
          ) : (
            <FolderBreadcrumb breadcrumb={breadcrumb} />
          )}
        </div>
      )}

      <DocumentTable
        documents={tableDocuments}
        hasMore={hasMoreDocuments}
        loadingMore={loadingMore}
        onLoadMore={loadMoreDocuments}
        showSemanticColumn={isSemanticSearching}
        folderMode={folderMode}
        onCreateFolder={() => setFolderNameTarget({ create: true })}
        onUploadClick={() => setUploadOpen(true)}
        folders={tableFolders}
        showParentRow={folderMode && folderPath.length > 0}
        onOpenParentFolder={openParentFolder}
        onOpenFolder={openFolder}
        onRenameFolder={(folder) => setFolderNameTarget(folder)}
        onDeleteFolder={(folder) => setDeleteTargets(resolveSelectionTargets("folder", folder.id, folder.name))}
        onMoveFolder={(folder) => setMoveTargets(resolveSelectionTargets("folder", folder.id, folder.name))}
        loading={loading || (isSemanticSearching && searchLoading)}
        navigationLoading={navigationLoading}
        error={error || (isSemanticSearching ? searchError : "")}
        selectedIds={selectedIds}
        onToggleRow={toggleRow}
        selectedFolderIds={selectedFolderIds}
        onToggleFolderRow={toggleFolderRow}
        onToggleAll={toggleAll}
        onRowSelect={selectRow}
        buildDragPayload={buildDragPayload}
        onDropMove={handleDropMove}
        parentFolderId={parentFolderId}
        onDownload={onDownload}
        onMoveDocument={(doc) => setMoveTargets(resolveSelectionTargets("document", doc.id, doc.title))}
        onBulkMove={() => setMoveTargets(selectionAsItems())}
        onUploadVersion={(doc) => setVersionTarget(doc)}
        onEdit={(doc) => setEditTarget(doc)}
        onDelete={(doc) => setDeleteTargets(resolveSelectionTargets("document", doc.id, doc.title))}
        onRestore={(doc) => setRestoreTarget(doc)}
        onBulkDelete={() => setDeleteTargets(selectionAsItems())}
        onViewEvidence={(doc) => setEvidenceTarget(doc)}
        scrollResetKey={scrollResetKey}
        disabled={navigating}
        onNavigateToWriteReport={() => navigateToFeature("/write-report")}
        onNavigateToSummary={() => navigateToFeature("/summary")}
        onNavigateToDocumentQA={() => navigateToFeature("/document-qa")}
      />

      <FolderNameModal
        open={Boolean(folderNameTarget)}
        onClose={() => setFolderNameTarget(null)}
        folder={folderNameTarget?.create ? null : folderNameTarget}
        parentFolderId={modalFolderId}
        onSaved={(isRename) => {
          notify("success", isRename ? "Folder renamed" : "Folder created");
          reload();
        }}
      />

      <UploadDocumentModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        folderId={modalFolderId}
        onUploaded={(count) => {
          notify("success", count > 1 ? `Uploaded ${count} document(s)` : "Document uploaded");
          reload();
        }}
      />

      <UploadVersionModal
        open={Boolean(versionTarget)}
        onClose={() => setVersionTarget(null)}
        documentId={versionTarget?.id}
        title={versionTarget?.title}
        onUploaded={() => {
          notify("success", "New version uploaded");
          reload();
        }}
      />

      <EditMetadataModal
        open={Boolean(editTarget)}
        onClose={() => setEditTarget(null)}
        documentId={editTarget?.id}
        onSaved={() => {
          notify("success", "Metadata updated");
          reload();
        }}
      />

      <MoveItemModal
        open={Boolean(moveTargets)}
        onClose={() => setMoveTargets(null)}
        items={moveTargets}
        currentFolderId={modalFolderId}
        onMove={moveItems}
        onMoved={() => {
          notify(
            "success",
            moveTargets.length === 1
              ? `${moveTargets[0].type === "folder" ? "Folder" : "Document"} moved`
              : `Moved ${moveTargets.length} item(s)`
          );
          setMoveTargets(null);
          reload();
        }}
      />

      <ConfirmDialog
        open={Boolean(deleteTargets)}
        onClose={() => setDeleteTargets(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title={deleteDialogText.title}
        confirmLabel="Delete"
        message={deleteDialogText.message}
      />

      <ConfirmDialog
        open={Boolean(restoreTarget)}
        onClose={() => setRestoreTarget(null)}
        onConfirm={confirmRestore}
        loading={restoring}
        tone="default"
        title="Restore document"
        confirmLabel="Restore"
        loadingLabel="Restoring…"
        message={`Restore "${restoreTarget?.title}"? It will become active again.`}
      />

      <EvidenceDialog
        open={Boolean(evidenceTarget)}
        onClose={() => setEvidenceTarget(null)}
        doc={evidenceTarget}
        matches={evidenceTarget ? semanticByDocument.get(evidenceTarget.id)?.matches : null}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
