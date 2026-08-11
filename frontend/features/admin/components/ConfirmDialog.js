"use client";

import Modal from "@/components/ui/Modal";

// Xác nhận cho mọi hành động phá huỷ (xoá user/department, thu hồi chia sẻ, xoá vĩnh viễn).
export default function ConfirmDialog({ open, title, message, confirmLabel = "Xác nhận", busy, onConfirm, onClose }) {
  return (
    <Modal open={open} onClose={onClose} title={title} maxWidth="max-w-md" preventClose={busy}>
      <p className="text-sm text-text-secondary">{message}</p>
      <div className="mt-6 flex justify-end gap-2">
        <button type="button" className="btn-secondary" onClick={onClose} disabled={busy}>
          Huỷ
        </button>
        <button type="button" className="btn-primary" onClick={onConfirm} disabled={busy}>
          {busy ? "Đang xử lý…" : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
