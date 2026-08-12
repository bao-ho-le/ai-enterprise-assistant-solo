"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, Share2, Trash2 } from "lucide-react";
import { useAuth } from "@/lib/AuthContext";
import { hasPermission } from "@/lib/permissions";
import { getDepartmentDetail, getDepartments } from "@/services/departmentService";
import {
  getDocumentShares,
  revokeDocumentShare,
  shareDocument,
} from "@/services/documentService";
import { formatDateTimeSlash } from "@/utils/format";

// Chia sẻ tài liệu cho user cụ thể. Chỉ chủ sở hữu tài liệu mới thấy form chia sẻ/thu hồi —
// backend cũng tự chặn nếu người gọi không có DOCUMENT_MANAGE_ACCESS trên chính tài liệu đó.
export default function DocumentSharePanel({ documentId, ownerId, onToast }) {
  const { user } = useAuth();

  const [shares, setShares] = useState([]);
  const [members, setMembers] = useState([]);
  const [targetUserIds, setTargetUserIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const isOwner = ownerId != null && user?.id === ownerId;
  const canManageAccess = isOwner && hasPermission(user, "DOCUMENT_MANAGE_ACCESS");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getDocumentShares(documentId)
      .then(setShares)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [documentId]);

  useEffect(load, [load]);

  // Người nhận chia sẻ: toàn bộ user trong tổ chức, gom theo từng department —
  // không có endpoint "list all users" nào mà user thường được phép gọi.
  useEffect(() => {
    if (!canManageAccess) return;
    getDepartments({ size: 200 })
      .then(async (page) => {
        const departments = page?.content ?? [];
        const details = await Promise.all(
          departments.map((d) => getDepartmentDetail(d.departmentId).catch(() => null))
        );
        setMembers(
          details.flatMap(
            (detail, i) =>
              detail?.members?.map((member) => ({
                ...member,
                departmentName: departments[i].name,
              })) ?? []
          )
        );
      })
      .catch(() => setMembers([]));
  }, [canManageAccess]);

  const submit = async () => {
    setBusy(true);
    try {
      await Promise.all(targetUserIds.map((id) => shareDocument(documentId, Number(id))));
      setTargetUserIds([]);
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
  const candidatesByDepartment = candidates.reduce((acc, member) => {
    (acc[member.departmentName] ??= []).push(member);
    return acc;
  }, {});

  return (
    <div className="card p-6">
      <div className="mb-5 flex items-center gap-2">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-500/10">
          <Share2 className="h-4 w-4 text-purple-400" />
        </span>
        <h2 className="text-base font-semibold text-text-primary">Shared With</h2>
      </div>

      {canManageAccess && (
        <div className="mb-4 flex flex-col gap-2">
          <label className="label-text">Chia sẻ cho (giữ Ctrl/Cmd để chọn nhiều)</label>
          <select
            multiple
            size={Math.min(6, Math.max(candidates.length, 3))}
            className="w-full rounded-lg border border-border-subtle bg-bg-primary p-2 text-sm text-text-primary"
            value={targetUserIds}
            onChange={(e) =>
              setTargetUserIds(Array.from(e.target.selectedOptions, (o) => o.value))
            }
          >
            {Object.entries(candidatesByDepartment).map(([departmentName, groupMembers]) => (
              <optgroup key={departmentName} label={departmentName}>
                {groupMembers.map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.fullName} ({member.email})
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
          <button
            type="button"
            className="btn-primary self-end py-2 px-4 text-sm"
            disabled={targetUserIds.length === 0 || busy}
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
