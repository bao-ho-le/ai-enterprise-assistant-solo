import { ChevronLeft, ChevronRight } from "lucide-react";

// showSummary=false drops the "Showing X of Y" caption and leaves just the page
// controls — File Storage counts folders and documents together, so a document-only
// caption there would be wrong. AI Usage keeps it.
//
// bare=true drops the card-footer chrome (background/border/padding) entirely and
// renders only the Prev/Next controls — for File Storage, where the controls sit
// inline in the breadcrumb row instead of as the table's own footer bar.
export default function Pagination({
  number,
  totalPages,
  totalElements,
  shown,
  onPrev,
  onNext,
  itemLabel = "documents",
  showSummary = true,
  bare = false,
  footerClassName = "bg-bg-secondary py-3",
}) {
  const controls = (
    <div className="flex items-center gap-2">
      <button
        type="button"
        className="btn-ghost p-2"
        aria-label="Previous page"
        disabled={number <= 0}
        onClick={onPrev}
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <span className="text-sm text-text-secondary px-2">
        Page {totalPages === 0 ? 0 : number + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="btn-ghost p-2"
        aria-label="Next page"
        disabled={number >= totalPages - 1}
        onClick={onNext}
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );

  if (bare) return controls;

  return (
    <div className={`flex items-center px-4 border-t border-border-subtle ${footerClassName} ${showSummary ? "justify-between" : "justify-end"}`}>
      {showSummary && (
        <p className="text-sm text-text-muted">
          Showing {shown} of {totalElements} {itemLabel}
        </p>
      )}
      {controls}
    </div>
  );
}
