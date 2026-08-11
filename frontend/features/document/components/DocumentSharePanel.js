"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, Share2, Trash2 } from "lucide-react";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission } from "@/lib/permissions";
import { getMyDepartment } from "@/services/departmentService";
import {
  getDocumentShares,
  revokeDocumentShare,
  shareDocument,
} from "@/services/documentService";
import { formatDateTimeSlash } from "@/utils/format";

// Chia sẻ tài liệu cho user cụ thể. Backend chỉ cấp READ/DOWNLOAD cho người được chia sẻ
// và tự chặn nếu người gọi không có DOCUMENT_MANAGE_ACCESS trên chính tài liệu đó.
export default function DocumentSharePanel({ documentId, onToast }) {
  const { user } = useAuth();

  const [shares, setShares] = useState([]);
  const [members, setMembers] = useState([]);
  const [targetUserId, setTargetUserId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const canManageAccess = hasPermission(user, "DOCUMENT_MANAGE_ACCESS");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getDocumentShares(documentId)
      .then(setShares)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [documentId]);

  useEffect(load, [load]);

  // Người nhận chia sẻ lấy từ department của chính user — API user thường được phép gọi.
  useEffect(() => {
    if (!canManageAccess) return;
    getMyDepartment()
      .then((detail) => setMembers(detail?.members ?? []))
      .catch(() => setMembers([]));
  }, [canManageAccess]);

  const submit = async () => {
    setBusy(true);
    try {
      await shareDocument(documentId, Number(targetUserId));
      setTargetUserId("");
      onToast?.({ type: "success", text: "Đã chia sẻ tài liệu" });
      load();
    } catch (e) {
      onToast?.({ type: "error", text: e.message });
    } finally {
      setBusy(false);
    }
  };

  const revoke = async (sharedUserId) => {
    try {
      await revokeDocumentShare(documentId, sharedUserId);
      onToast?.({ type: "success", text: "Đã thu hồi quyền truy cập" });
      load();
    } catch (e) {
      onToast?.({ type: "error", text: e.message });
    }
  };

  const candidates = members.filter(
    (member) =>
      member.userId !== user?.id && !shares.some((share) => share.sharedUserId === member.userId)
  );

  return (
    <div className="card p-6">
      <div className="mb-5 flex items-center gap-2">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-500/10">
          <Share2 className="h-4 w-4 text-purple-400" />
        </span>
        <h2 className="text-base font-semibold text-text-primary">Shared With</h2>
      </div>

      {canManageAccess && (
        <div className="mb-4 flex items-end gap-2">
          <div className="flex-1">
            <label className="label-text">Chia sẻ cho</label>
            <select
              className="select-field"
              value={targetUserId}
              onChange={(e) => setTargetUserId(e.target.value)}
            >
              <option value="">Chọn người dùng…</option>
              {candidates.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.fullName} ({member.email})
                </option>
              ))}
            </select>
          </div>
          <button
            type="button"
            className="btn-primary py-2 px-4 text-sm"
            disabled={!targetUserId || busy}
            onClick={submit}
          >
            {busy ? "Đang chia sẻ…" : "Chia sẻ"}
          </button>
        </div>
      )}

      {loading && <Loader2 className="mx-auto my-6 h-5 w-5 animate-spin text-text-muted" />}

      {!loading && error && <p className="py-4 text-center text-sm text-error">{error}</p>}

      {!loading && !error && shares.length === 0 && (
        <p className="py-4 text-center text-sm text-text-muted">
          Tài liệu chưa được chia sẻ với ai.
        </p>
      )}

      {!loading && !error && shares.length > 0 && (
        <ul className="flex flex-col gap-1">
          {shares.map((share) => (
            <li
              key={share.documentAccessId}
              className="flex items-center justify-between rounded-lg border border-border-subtle px-3 py-2"
            >
              <div>
                <p className="text-sm text-text-primary">{share.sharedUserName}</p>
                <p className="text-xs text-text-muted">
                  {share.sharedUserEmail} · {formatDateTimeSlash(share.sharedAt)}
                </p>
              </div>
              {canManageAccess && (
                <button
                  type="button"
                  className="btn-ghost p-1.5"
                  aria-label={`Revoke ${share.sharedUserName}`}
                  onClick={() => revoke(share.sharedUserId)}
                >
                  <Trash2 className="h-4 w-4 text-error" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
