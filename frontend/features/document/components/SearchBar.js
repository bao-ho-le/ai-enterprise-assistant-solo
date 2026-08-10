"use client";

import { ArrowUpDown, Info, Search } from "lucide-react";

const MODE_INFO = {
  semantic: {
    placeholder: "Search documents by meaning...",
    ariaLabel: "Semantic search",
    tooltip: ["Powered by vector embeddings", "Search by meaning, not just file names"],
    toggleTitle: "Semantic search — click to switch to keyword search",
  },
  keyword: {
    placeholder: "Search folders and documents by keywords...",
    ariaLabel: "Keyword search",
    tooltip: ["Matches folder and document names", "Click the toggle to search by meaning instead"],
    toggleTitle: "Keyword search — click to switch to semantic search",
  },
};

// `leftSlot` (optional) renders immediately to the left of the search input —
// used by the File Storage page's filter popover trigger, so the filter icon
// sits directly beside the search bar.
//
// `mode`/`onToggleMode` drive the mode toggle button — semantic vs keyword
// search. It's a plain click-to-flip button, not a dropdown, so there is no
// open/close state to own here.
//
// The plain info icon to the right of the input explains what the active mode
// does. It has no button chrome and no click behaviour — the tooltip is
// hover-only (CSS group-hover, desktop).
//
// The input carries aria-describedby pointing at the tooltip, which is always in
// the DOM (only visually hidden). That is deliberate: with a hover-only CSS
// tooltip and no JS state, this is the only way screen-reader users get the
// explanation — don't hide the tooltip from assistive tech.
export default function SearchBar({ value, onChange, leftSlot, mode, onToggleMode }) {
  const info = MODE_INFO[mode];
  return (
    <div className="relative w-full max-w-3xl mx-auto">
      <div className="relative">
        <div className="flex items-center gap-2">
          {leftSlot}
          <div className="flex h-12 flex-1 items-center rounded-lg border border-border-subtle bg-bg-card pl-2 pr-[13px]">
            <button
              type="button"
              onClick={onToggleMode}
              className="mr-3 flex h-8 w-8 shrink-0 items-center justify-center rounded-md border border-border-default bg-bg-elevated text-text-muted transition-colors hover:bg-white/10 hover:text-text-primary"
              aria-label={`Switch search mode (currently ${info.ariaLabel})`}
              title={info.toggleTitle}
            >
              <ArrowUpDown className="h-5 w-5" />
            </button>
            <input
              type="search"
              value={value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={info.placeholder}
              className="flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
              aria-label={info.ariaLabel}
              aria-describedby="search-mode-info"
            />
            <Search className="h-5 w-5 text-text-muted ml-3" />
          </div>

          <div className="group relative flex shrink-0 items-center">
            {/* Default cursor on hover — the icon is not interactive, only its
                tooltip is; the color shift alone signals the hover. */}
            <Info
              className="h-5 w-5 text-text-muted transition-colors group-hover:text-text-primary"
              aria-hidden={false}
              aria-label={`About ${info.ariaLabel.toLowerCase()}`}
            />
            <div
              id="search-mode-info"
              role="tooltip"
              className="pointer-events-none absolute right-0 top-full z-20 mt-2 w-64 rounded-lg border border-border-subtle bg-bg-card p-3 text-xs leading-relaxed text-text-secondary opacity-0 shadow-lg transition-opacity duration-150 group-hover:opacity-100"
            >
              {info.tooltip.map((line) => (
                <p key={line}>{line}</p>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
