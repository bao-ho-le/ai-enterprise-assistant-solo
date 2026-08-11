"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { Send, Loader2, Quote, FileText, AlertTriangle, Copy } from "lucide-react";
import MessageSourcesDialog from "./MessageSourcesDialog";
import DeletedDocumentsWarning from "./DeletedDocumentsWarning";
import { getDocumentQaConversationDetail, getConversationDocuments } from "@/services/conversationService";
import { sendMessage, getMessages } from "@/services/messageService";
import { formatDateTime, formatDateTimeSlash } from "@/utils/format";
import { conversationTypeLabel } from "@/constants/conversation";
import { ApiError } from "@/lib/apiClient";

const RECENT_MESSAGES_LIMIT = 20;

// Renders "[1]", "[2]" citation markers (FakeLLMService.qaAnswer) as small numbered
// badges inline instead of literal bracket text.
function renderContentWithCitations(content) {
  return content.split(/(\[\d+\])/g).map((part, i) => {
    const match = /^\[(\d+)\]$/.exec(part);
    if (!match) return part;
    return (
      <span
        key={i}
        className="mx-0.5 inline-flex h-4 w-4 items-center justify-center rounded-full bg-bg-elevated text-[10px] text-text-muted align-middle"
      >
        {match[1]}
      </span>
    );
  });
}

export default function DocumentQaDetailView({ conversationId }) {
  const [conversation, setConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const [content, setContent] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState("");

  const [sourcesTarget, setSourcesTarget] = useState(null);
  const [copiedMessageId, setCopiedMessageId] = useState(null);
  const bottomRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const messageRefs = useRef(new Map());
  // Message id + its offset from the viewport top, captured right before an older
  // page loads — lets us restore that exact spot afterwards instead of relying on
  // where in the array the new messages land.
  const scrollAnchorRef = useRef(null);
  const skipBottomScrollRef = useRef(false);
  // Synchronous re-entrancy guard — `loadingMore` state updates asynchronously, which
  // isn't enough to stop a second scroll event firing before the first fetch's state
  // update has committed.
  const loadingMoreRef = useRef(false);

  // Same copy-to-clipboard pattern as EmailPreview/GeneratedContentPreview's Copy
  // button — keyed by message id since multiple answers can sit in the list at once.
  const copyMessage = async (messageId, content) => {
    try {
      await navigator.clipboard.writeText(content || "");
      setCopiedMessageId(messageId);
      setTimeout(() => setCopiedMessageId((prev) => (prev === messageId ? null : prev)), 1500);
    } catch {
      // Clipboard API unavailable (e.g. insecure context) — nothing more we can do here.
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getDocumentQaConversationDetail(conversationId, RECENT_MESSAGES_LIMIT, controller.signal)
      .then((detail) => {
        if (controller.signal.aborted) return;
        setConversation(detail);
        setMessages(detail?.recentMessages || []);
        setHasMore(Boolean(detail?.hasMoreMessages));
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load conversation");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [conversationId]);

  useEffect(() => {
    const onRenamed = (e) => {
      if (String(e.detail?.id) === String(conversationId)) {
        setConversation((prev) => (prev ? { ...prev, title: e.detail.title } : prev));
      }
    };
    window.addEventListener("conversation-renamed", onRenamed);
    return () => window.removeEventListener("conversation-renamed", onRenamed);
  }, [conversationId]);

  // RightDocumentPanel attaches/removes documents independently (separate component,
  // separate fetch) — resync attachedDocuments here so the chat lock reacts live.
  useEffect(() => {
    const onDocsChanged = (e) => {
      if (String(e.detail?.conversationId) !== String(conversationId)) return;
      getConversationDocuments(conversationId)
        .then((docs) => setConversation((prev) => (prev ? { ...prev, attachedDocuments: docs } : prev)))
        .catch(() => {
          // Best-effort resync — a stale count just means the lock updates on next full reload.
        });
    };
    window.addEventListener("conversation-documents-changed", onDocsChanged);
    return () => window.removeEventListener("conversation-documents-changed", onDocsChanged);
  }, [conversationId]);

  useEffect(() => {
    // Loading older messages already restores the user's position itself (see the
    // layout effect below) — jumping to the bottom afterwards would undo that.
    if (skipBottomScrollRef.current) {
      skipBottomScrollRef.current = false;
      return;
    }
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages.length]);

  // Runs before paint whenever `messages` changes. Only acts when loadMore set an
  // anchor: finds that same message's new position and adjusts scrollTop by the
  // difference, so whatever the user was looking at stays exactly in view.
  useLayoutEffect(() => {
    const anchor = scrollAnchorRef.current;
    if (!anchor) return;
    scrollAnchorRef.current = null;
    const container = scrollContainerRef.current;
    const el = messageRefs.current.get(anchor.id);
    if (!container || !el) return;
    const newOffset = el.getBoundingClientRect().top - container.getBoundingClientRect().top;
    container.scrollTop += newOffset - anchor.offset;
  }, [messages]);

  // Fires when the user scrolls near the top of the list (see the scroll-listener
  // effect below). Guarded by loadingMoreRef (synchronous — state alone can lag behind
  // rapid scroll events) and hasMore, so it never overlaps itself and stops completely
  // once the oldest message in the conversation has been loaded.
  const loadMore = async () => {
    if (loadingMoreRef.current || !hasMore || messages.length === 0) return;
    loadingMoreRef.current = true;

    // Anchor on whichever message currently sits at the top of the visible viewport —
    // works regardless of where the newly-loaded messages end up in the list.
    const container = scrollContainerRef.current;
    if (container) {
      const containerTop = container.getBoundingClientRect().top;
      const anchorMessage =
        messages.find((m) => {
          const el = messageRefs.current.get(m.id);
          return el && el.getBoundingClientRect().top - containerTop >= 0;
        }) || messages[0];
      const anchorEl = anchorMessage && messageRefs.current.get(anchorMessage.id);
      if (anchorEl) {
        scrollAnchorRef.current = {
          id: anchorMessage.id,
          offset: anchorEl.getBoundingClientRect().top - containerTop,
        };
        skipBottomScrollRef.current = true;
      }
    }

    setLoadingMore(true);
    try {
      // Cursor = the oldest message currently loaded — the API returns the `size`
      // messages strictly before it, oldest-first, so they always prepend cleanly
      // with no gap or overlap regardless of how many messages exist in total.
      const oldestId = messages[0]?.id;
      const slice = await getMessages(conversationId, { beforeId: oldestId, size: RECENT_MESSAGES_LIMIT });
      const older = slice?.content || [];
      setMessages((prev) => {
        // Defensive merge: de-dup guards against the same page being merged twice
        // (e.g. a duplicate trigger that slipped past the guards above), and re-sorting
        // guards against the API ever returning a page out of createdAt order — the
        // merged list is always oldest-to-newest no matter what order either side came in.
        const seen = new Set(prev.map((m) => m.id));
        return [...older.filter((m) => !seen.has(m.id)), ...prev].sort(
          (a, b) => new Date(a.createdAt) - new Date(b.createdAt) || a.id - b.id
        );
      });
      setHasMore(Boolean(slice?.hasMore));
    } catch (err) {
      setError(err.message || "Failed to load more messages");
      scrollAnchorRef.current = null;
      skipBottomScrollRef.current = false;
    } finally {
      setLoadingMore(false);
      loadingMoreRef.current = false;
    }
  };

  // Keeps the scroll listener (attached once per conversation) calling whatever the
  // latest render's loadMore closure is, instead of a stale one.
  const loadMoreRef = useRef(loadMore);
  loadMoreRef.current = loadMore;

  const NEAR_TOP_PX = 100;

  // Infinite scroll: once scrollTop gets within NEAR_TOP_PX of the top, load the next
  // older page. loadMore's own guards (loadingMoreRef, hasMore) make this listener a
  // no-op once loading is in flight or the conversation's start has been reached.
  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    const onScroll = () => {
      if (container.scrollTop <= NEAR_TOP_PX) loadMoreRef.current();
    };
    container.addEventListener("scroll", onScroll);
    // Also check once up front — a short conversation's initial page may not fill the
    // viewport at all, leaving nothing for the user to scroll.
    onScroll();
    return () => container.removeEventListener("scroll", onScroll);
  }, [conversationId, loading]);

  const locked = !conversation?.attachedDocuments?.length;

  const submit = async (e) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed || sending || locked) return;
    setSending(true);
    setSendError("");
    try {
      const { userMessage, assistantMessage } = await sendMessage(conversationId, trimmed);
      // ponytail: appended straight to the tail even if hasMore is still true (older
      // pages not yet loaded) — acceptable for the expected conversation sizes here;
      // reload the full detail instead if conversations regularly exceed a few pages.
      setMessages((prev) => [...prev, userMessage, ...(assistantMessage ? [assistantMessage] : [])]);
      setContent("");
    } catch (err) {
      setSendError(err instanceof ApiError ? err.message : "Failed to send message. Please try again.");
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center overflow-hidden">
        <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
      </main>
    );
  }

  if (error) {
    return (
      <main className="flex-1 flex items-center justify-center overflow-hidden">
        <p className="text-sm text-error">{error}</p>
      </main>
    );
  }

  return (
    <main className="flex-1 flex flex-col overflow-hidden">
      <div ref={scrollContainerRef} className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
        <div className="max-w-3xl mx-auto space-y-6">
          {/* Reconstructed the same way GenerationDetailView builds its header (Write
              Email/Report/Summary), instead of showing the raw stored title verbatim —
              keeps the format identical across all conversation types. */}
          <h1 className="text-sm font-medium text-text-primary">
            {conversationTypeLabel(conversation?.conversationType)} - {formatDateTimeSlash(conversation?.createdAt)}
          </h1>

          {hasMore && (
            <div className="flex justify-center py-2">
              {loadingMore && <Loader2 className="h-4 w-4 animate-spin text-text-muted" />}
            </div>
          )}

          {messages.length === 0 && !conversation?.attachedDocuments?.length ? (
            <div className="flex flex-col items-center gap-2 text-center py-10">
              <FileText className="h-6 w-6 text-text-muted" />
              <p className="text-sm text-text-muted">No document selected yet.</p>
              <p className="flex items-center justify-center gap-1.5 text-xs text-warning">
                <AlertTriangle className="h-3 w-3 shrink-0" />
                Attach a document from the panel on the right to start asking questions.
              </p>
            </div>
          ) : messages.length === 0 ? (
            <p className="text-sm text-text-muted text-center py-10">
              No messages yet. Ask a question about your attached documents below.
            </p>
          ) : (
            messages.map((m) => {
              const isUser = m.role === "USER";
              return (
                <div
                  key={m.id}
                  ref={(el) => {
                    if (el) messageRefs.current.set(m.id, el);
                    else messageRefs.current.delete(m.id);
                  }}
                  className={`flex gap-4 ${isUser ? "flex-row-reverse" : ""}`}
                >
                  <div className={`flex-1 min-w-0 ${isUser ? "flex flex-col items-end" : "w-full"}`}>
                    <div
                      className={
                        isUser
                          ? "rounded-xl px-4 py-3 bg-bg-elevated border border-border-subtle max-w-[85%]"
                          : ""
                      }
                    >
                      <p
                        className={`text-sm text-text-primary leading-relaxed whitespace-pre-wrap ${
                          isUser ? "text-left" : "text-justify"
                        }`}
                      >
                        {isUser ? m.content : renderContentWithCitations(m.content)}
                      </p>
                    </div>
                    <div className={`flex items-center gap-2 mt-1.5 px-1 ${isUser ? "flex-row-reverse" : ""}`}>
                      {!isUser && (
                        <>
                          <button
                            type="button"
                            className="flex items-center gap-1 text-xs text-text-muted hover:text-accent transition-colors"
                            onClick={() => copyMessage(m.id, m.content)}
                          >
                            <Copy className="h-3 w-3" />
                            {copiedMessageId === m.id ? "Copied!" : "Copy"}
                          </button>
                          <button
                            type="button"
                            className="flex items-center gap-1 text-xs text-text-muted hover:text-accent transition-colors"
                            onClick={() => setSourcesTarget(m.id)}
                          >
                            <Quote className="h-3 w-3" />
                            Sources
                          </button>
                        </>
                      )}
                      <p className="text-xs text-text-muted">{formatDateTime(m.createdAt)}</p>
                    </div>
                  </div>
                </div>
              );
            })
          )}

          {conversation?.hasDeletedAttachedDocuments && (
            <DeletedDocumentsWarning
              documentIds={conversation.attachedDocuments?.map((doc) => doc.documentId)}
            />
          )}

          <div ref={bottomRef} />
        </div>
      </div>

      <div className="p-4">
        <form className="flex items-end gap-3 max-w-3xl mx-auto" onSubmit={submit}>
          <div className="flex-1">
            <div className="flex items-center gap-2 rounded-xl border border-border-subtle bg-bg-card px-3 py-2 min-h-[48px]">
              <textarea
                rows={1}
                placeholder={locked ? "Attach a document to start chatting" : "Ask a question about your documents..."}
                className="flex-1 resize-none bg-transparent text-sm text-text-primary outline-none leading-normal disabled:opacity-50"
                aria-label="Chat message input"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) submit(e);
                }}
                disabled={sending || locked}
              />
              <button
                type="submit"
                className="flex h-8 w-8 items-center justify-center rounded-md bg-bg-elevated text-text-muted hover:bg-bg-card disabled:opacity-50"
                aria-label="Send message"
                disabled={!content.trim() || sending || locked}
              >
                {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </form>
        {sendError ? (
          <p className="text-center text-xs text-error mt-2">{sendError}</p>
        ) : locked ? (
          <p className="text-center text-xs text-text-muted mt-2">
            Ask questions only about the attached document(s). The AI will answer based on their content.
          </p>
        ) : (
          <p className="text-center text-xs text-text-muted mt-2">
            AI responses are grounded in your selected source documents
          </p>
        )}
      </div>

      <MessageSourcesDialog
        open={Boolean(sourcesTarget)}
        onClose={() => setSourcesTarget(null)}
        conversationId={conversationId}
        messageId={sourcesTarget}
      />
    </main>
  );
}
