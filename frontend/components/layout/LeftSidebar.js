"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";

// ── Refined icon: square pen (thinner stroke, softer corners) ──
function SquarePenIcon({ className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
      <path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z" />
    </svg>
  );
}

// ── Refined icon: trash (thinner stroke, softer corners) ──
function TrashIcon({ className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M3 6h18" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
      <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      <line x1="10" y1="11" x2="10" y2="17" />
      <line x1="14" y1="11" x2="14" y2="17" />
    </svg>
  );
}
import ConfirmDialog from "@/features/document/components/ConfirmDialog";
import Toast from "@/components/ui/Toast";
import RenameConversationModal from "@/features/conversation/components/RenameConversationModal";
import ConversationRowMenu from "@/features/conversation/components/ConversationRowMenu";
import {
  getConversations,
  softDeleteConversation,
} from "@/services/conversationService";
import { ROUTED_CONVERSATION_TYPES } from "@/constants/conversation";
import { formatConversationTitle } from "@/utils/format";

// Sidebar-only display formatting: backend conversation titles are generated as
// "<Type Name> - 29/07/2026 12:29" (see AIConversationHelper.defaultTitle). The
// sidebar shows just the type abbreviation (EG / RG / SG / QA) and a "-" between
// the date and the time so more rows fit the narrow column. Pure display — the
// stored title and API payload are untouched, and titles the user renamed (no
// recognized prefix) are shown verbatim. Longer labels must be matched before
// their short forms, so the map order below matters ("Summary Generation" before
// "Summary").
const SIDEBAR_TITLE_ABBREVIATIONS = {
  "Email Generation": "EG",
  "Report Generation": "RG",
  "Write Report": "RG",
  "Summary Generation": "SG",
  "Summary": "SG",
  "Document QA": "QA",
};

function formatSidebarTitle(title) {
  if (!title) return title;
  let t = title;
  // Case-insensitive: the backend's default title for Document QA is generated
  // as "Document Qa - ..." (not "Document QA"), which a case-sensitive prefix
  // check would silently skip, leaving it unabbreviated unlike every other type.
  for (const [label, abbr] of Object.entries(SIDEBAR_TITLE_ABBREVIATIONS)) {
    if (t.toLowerCase().startsWith(label.toLowerCase())) {
      t = abbr + t.slice(label.length);
      break;
    }
  }
  return formatConversationTitle(t);
}

// conversationType: which ConversationType this sidebar lists (e.g. "DOCUMENT_QA").
// basePath: route prefix conversations of this type live under (e.g. "/document-qa"),
// paired with a "[conversationId]" dynamic route.
export default function LeftSidebar({ conversationType, basePath }) {
  const pathname = usePathname();
  const router = useRouter();
  const activeConversationId = pathname.startsWith(`${basePath}/`)
    ? pathname.slice(basePath.length + 1).split("/")[0]
    : null;

  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  // Defaults to this screen's own type; the dropdown lets the user browse another
  // type's history without leaving this sidebar. List items route to wherever that
  // type's screen actually lives, not necessarily this page's basePath.
  const [selectedType, setSelectedType] = useState(conversationType);
  const selectedBasePath =
    ROUTED_CONVERSATION_TYPES.find((t) => t.value === selectedType)?.basePath || basePath;

  const [renameTarget, setRenameTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState(null);
  const notify = useCallback((type, text) => setToast({ type, text }), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getConversations({ conversationType: selectedType }, controller.signal)
      .then((page) => {
        if (controller.signal.aborted) return;
        setConversations(page?.content || []);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load conversations");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [selectedType, reloadKey]);

  const onRenamed = (updated) => {
    setRenameTarget(null);
    notify("success", "Conversation renamed");
    reload();
    // The detail view (main content) fetched its own copy of the title independently —
    // nudge it to stay in sync without wiring up shared state for one field.
    window.dispatchEvent(new CustomEvent("conversation-renamed", { detail: updated }));
  };

  // A conversation restored from the Deleted Conversations screen must reappear in
  // this active list — refresh without a full page reload (mirror of onRenamed).
  useEffect(() => {
    const onRestored = (e) => {
      if (e.detail?.id != null) reload();
    };
    window.addEventListener("conversation-restored", onRestored);
    return () => window.removeEventListener("conversation-restored", onRestored);
  }, [reload]);

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await softDeleteConversation(deleteTarget.id);
      notify("success", "Conversation deleted");
      if (String(deleteTarget.id) === activeConversationId) router.push(basePath);
      setDeleteTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <aside className="hidden md:flex w-68 shrink-0 flex-col border-r border-border-subtle bg-bg-primary">
      <div className="flex flex-col gap-0.5 p-2">
        <button
          type="button"
          onClick={() => router.push(basePath)}
          className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
        >
          <SquarePenIcon className="h-[18px] w-[18px] shrink-0 text-text-secondary" />
          <span className="text-sm font-medium text-text-primary">New conversation</span>
        </button>

        <Link
          href="/conversations/deleted"
          className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
        >
          <TrashIcon className="h-[18px] w-[18px] shrink-0 text-text-secondary" />
          <span className="text-sm font-medium text-text-primary">Deleted conversations</span>
        </Link>
      </div>

      <div className="flex-1 overflow-y-auto pt-1 pb-3 px-2">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-8 text-text-muted">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <p className="px-2 text-sm text-error">{error}</p>
        ) : conversations.length === 0 ? (
          <p className="px-2 text-sm text-text-muted">No conversations yet.</p>
        ) : (
          <ul className="space-y-0.5">
            {conversations.map((c) => {
              const active = String(c.id) === activeConversationId;
              return (
                <li key={c.id} className="group relative">
                  <Link
                    href={`${selectedBasePath}/${c.id}`}
                    className={
                      active
                        ? "flex items-center gap-1 rounded-lg px-2.5 py-2.5 bg-bg-elevated border border-border-subtle transition-colors hover:border-border-default"
                        : "flex items-center gap-1 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
                    }
                  >
                    <p
                      className={
                        active
                          ? "flex-1 min-w-0 truncate text-sm font-medium text-text-primary tabular-nums"
                          : "flex-1 min-w-0 truncate text-sm text-text-primary tabular-nums"
                      }
                      title={c.title}
                    >
                      {formatSidebarTitle(c.title)}
                    </p>
                    <span
                      className="flex shrink-0 items-center opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => e.preventDefault()}
                    >
                      <ConversationRowMenu
                        conversation={c}
                        onRename={setRenameTarget}
                        onDelete={setDeleteTarget}
                      />
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <RenameConversationModal
        open={Boolean(renameTarget)}
        onClose={() => setRenameTarget(null)}
        conversation={renameTarget}
        onRenamed={onRenamed}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title="Delete conversation"
        confirmLabel="Delete"
        message={`Delete "${deleteTarget?.title}"? You can't undo this from here, but the data isn't permanently erased.`}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </aside>
  );
}
