"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

const MENU_WIDTH = 192; // w-48
const ITEM_HEIGHT = 36;
const MENU_PADDING = 8;
const VIEWPORT_MARGIN = 8;
// Rows open this menu from their "..." button (DocumentRow/FolderRow) or a
// right-click. The buttons own their own toggle — closing here on their press
// would make the menu flicker closed-then-reopened instead of toggling — so the
// outside-press handler below ignores presses on them and lets the row's onClick
// decide (same-row press closes, another row's press switches the menu over).
const MORE_ACTIONS_SELECTOR = 'button[aria-label="More actions"]';

// Anchors below the button, flipping above it when there isn't enough room left
// at the bottom of the viewport (e.g. the table's last rows). Right-click
// anchors at the cursor instead. Both axes are clamped so the menu never
// overflows the viewport even when opened right at the right/bottom edge.
function computeMenuPosition(anchor, menuHeight) {
  let top;
  let left;
  if (anchor.kind === "point") {
    const openUpward = window.innerHeight - anchor.y < menuHeight + VIEWPORT_MARGIN;
    top = openUpward ? anchor.y - menuHeight : anchor.y;
    left = anchor.x;
  } else {
    const rect = anchor.node.getBoundingClientRect();
    const openUpward = window.innerHeight - rect.bottom < menuHeight + VIEWPORT_MARGIN;
    top = openUpward ? rect.top - menuHeight - 4 : rect.bottom + 4;
    left = rect.right - MENU_WIDTH;
  }
  const maxTop = Math.max(VIEWPORT_MARGIN, window.innerHeight - menuHeight - VIEWPORT_MARGIN);
  const maxLeft = Math.max(VIEWPORT_MARGIN, window.innerWidth - MENU_WIDTH - VIEWPORT_MARGIN);
  return {
    top: Math.min(Math.max(top, VIEWPORT_MARGIN), maxTop),
    left: Math.min(Math.max(left, VIEWPORT_MARGIN), maxLeft),
  };
}

// The single context menu for the whole document/folder table (see DocumentTable)
// — replaces the old per-row RowActionsMenu. One instance, portaled to <body>
// with fixed positioning so it can't be clipped by the table's overflow-auto
// container and never shifts the table's layout.
//
// All state lives in the parent: `menu` says which row opened it
// ({ type, item, anchor, itemCount }) and `onClose` closes it — so at any moment
// at most one menu can exist, and right-clicking another row just replaces the
// state instead of stacking a second menu on top of the old one.
//
// children is a render prop receiving `close`, so each item can dismiss the menu
// before running its action. The menu re-anchors on scroll/resize (instead of
// closing — closing would also fire on the scroll-into-view a click can
// trigger), closes on outside left-click or Escape, and removes its listeners
// whenever it closes or the component unmounts.
export default function RowContextMenu({ menu, onClose, children }) {
  const menuRef = useRef(null);
  // Position is derived synchronously from the current menu (via the guarded
  // render-time setState below) so the portal is never painted at a stale spot
  // — even when right-clicking rapidly through rows, each switch repositions
  // before the browser paints. Scroll/resize updates go through the same state.
  const [position, setPosition] = useState(null);
  const [positionedAnchor, setPositionedAnchor] = useState(null);

  if (menu) {
    if (positionedAnchor !== menu.anchor) {
      setPosition(computeMenuPosition(menu.anchor, menu.itemCount * ITEM_HEIGHT + MENU_PADDING));
      setPositionedAnchor(menu.anchor);
    }
  } else if (positionedAnchor !== null) {
    setPosition(null);
    setPositionedAnchor(null);
  }

  // Stay glued to the anchor while the page/table scrolls or the window resizes.
  useEffect(() => {
    if (!menu) return;
    const reposition = () =>
      setPosition(computeMenuPosition(menu.anchor, menu.itemCount * ITEM_HEIGHT + MENU_PADDING));
    window.addEventListener("scroll", reposition, true);
    window.addEventListener("resize", reposition);
    return () => {
      window.removeEventListener("scroll", reposition, true);
      window.removeEventListener("resize", reposition);
    };
  }, [menu]);

  // Outside left-click (including on other rows' controls) or Escape closes the
  // menu. Row "..." buttons are exempt — their onClick owns the toggle (same-row
  // press closes, another row's press switches the menu to it). Right/middle-
  // button presses are ignored so the subsequent contextmenu on another row
  // switches this menu in a single state update instead of closing then
  // reopening it.
  useEffect(() => {
    if (!menu) return;
    const onPointerDown = (e) => {
      if (e.target?.closest?.(MORE_ACTIONS_SELECTOR)) return;
      if (e.button !== 0) return;
      if (menuRef.current && !menuRef.current.contains(e.target)) onClose();
    };
    const onKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menu, onClose]);

  if (!menu || !position) return null;

  return createPortal(
    <div
      ref={menuRef}
      style={{ position: "fixed", top: position.top, left: position.left, width: MENU_WIDTH }}
      className="rounded-lg border border-border-subtle bg-bg-card shadow-lg z-[200]"
    >
      <div className="py-1">{children(onClose)}</div>
    </div>,
    document.body
  );
}
