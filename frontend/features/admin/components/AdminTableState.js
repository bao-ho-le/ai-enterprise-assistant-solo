"use client";

import { AlertCircle, Loader2 } from "lucide-react";

// Loading / error / empty state dùng chung cho mọi bảng admin, để 8 trang không lặp lại markup.
export default function AdminTableState({ colSpan, loading, error, empty, emptyText = "No data", onRetry }) {
  if (loading) {
    return (
      <tr>
        <td colSpan={colSpan} className="px-4 py-16 text-center">
          <Loader2 className="mx-auto h-5 w-5 animate-spin text-text-muted" />
        </td>
      </tr>
    );
  }

  if (error) {
    return (
      <tr>
        <td colSpan={colSpan} className="px-4 py-16 text-center">
          <AlertCircle className="mx-auto mb-2 h-5 w-5 text-error" />
          <p className="text-sm text-error">{error}</p>
          {onRetry && (
            <button type="button" className="btn-secondary mt-3" onClick={onRetry}>
              Retry
            </button>
          )}
        </td>
      </tr>
    );
  }

  if (empty) {
    return (
      <tr>
        <td colSpan={colSpan} className="px-4 py-16 text-center text-sm text-text-muted">
          {emptyText}
        </td>
      </tr>
    );
  }

  return null;
}
