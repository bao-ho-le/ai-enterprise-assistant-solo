"use client";

import { useEffect, useId } from "react";
import { X } from "lucide-react";

// Escape should only close the topmost modal when more than one is open at
// once (e.g. a confirm dialog opened from inside another modal).
const openStack = [];

export default function Modal({ open, onClose, title, children, maxWidth = "max-w-lg", preventClose = false }) {
  const id = useId();

  useEffect(() => {
    if (!open) return;
    openStack.push(id);
    const onKey = (e) => {
      if (e.key !== "Escape" || preventClose) return;
      if (openStack[openStack.length - 1] !== id) return;
      onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      const index = openStack.indexOf(id);
      if (index !== -1) openStack.splice(index, 1);
    };
  }, [open, onClose, preventClose, id]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={preventClose ? undefined : onClose}
      />
      <div
        className={`relative card w-full ${maxWidth} bg-bg-card p-6 shadow-2xl`}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-semibold text-text-primary">{title}</h2>
          <button
            type="button"
            className="btn-ghost p-1.5"
            aria-label="Close"
            onClick={onClose}
            disabled={preventClose}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
