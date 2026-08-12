"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Sparkles, Menu, User, LogOut, ShieldCheck, Trash2, CalendarDays, Building2 } from "lucide-react";
import { useAuth } from "@/lib/AuthContext";
import { isAdmin } from "@/lib/permissions";
import { getInitials, formatDateTime } from "@/utils/format";

const NAV_LINKS = [
  { href: "/", label: "Home" },
  { href: "/file-storage", label: "File Storage" },
  { href: "/write-email", label: "Write Email" },
  { href: "/write-report", label: "Write Report" },
  { href: "/summary", label: "Summary" },
  { href: "/document-qa", label: "Document QA" },
];

function matchesPath(pathname, href) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

// Longest-prefix match: "/file-storage/trash" and "/file-storage" both match
// /file-storage/trash, so without this both nav items would light up together.
function isActive(pathname, href) {
  if (!matchesPath(pathname, href)) return false;
  return !NAV_LINKS.some(
    (link) => link.href !== href && link.href.length > href.length && matchesPath(pathname, link.href)
  );
}

export default function NavigationBar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const accountRef = useRef(null);

  useEffect(() => {
    if (!accountOpen) return;
    const onOutsideClick = (e) => {
      if (!accountRef.current?.contains(e.target)) setAccountOpen(false);
    };
    document.addEventListener("click", onOutsideClick);
    return () => document.removeEventListener("click", onOutsideClick);
  }, [accountOpen]);

  // Facebook-style login/register screens own their own header — no app nav there.
  // /admin has its own vertical sidebar shell (app/admin/layout.js) instead.
  if (pathname === "/login" || pathname === "/register" || pathname.startsWith("/admin")) return null;

  return (
    <header className="sticky top-0 z-50 border-b border-border-subtle bg-bg-primary/80 backdrop-blur-xl">
      <nav
        className="grid h-12 w-full grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center px-4"
        aria-label="Main navigation"
      >
        {/* Left: Logo */}
        <div className="flex min-w-0 items-center justify-self-start">
          <Link
            href="/"
            className="flex items-center gap-2.5 transition-opacity hover:opacity-80"
          >
            <span className="flex h-8 w-8 items-center justify-center">
              <Sparkles className="h-4 w-4 text-accent" />
            </span>
            <span className="hidden sm:inline text-sm font-semibold tracking-tight text-text-primary">
              Enterprise AI Assistant
            </span>
          </Link>
        </div>

        {/* Center: Navigation */}
        <div className="hidden justify-self-center lg:flex">
          <div className="flex items-center gap-1 whitespace-nowrap">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`btn-ghost px-3 py-1 text-sm ${
                  isActive(pathname, link.href) ? "nav-link-active" : ""
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>

        {/* Right: Actions */}
        <div className="flex min-w-0 items-center justify-self-end gap-3">
          {/* Whole cluster (avatar, name) is one click target for the account popover */}
          <div className="relative" ref={accountRef}>
            <button
              type="button"
              onClick={() => setAccountOpen((v) => !v)}
              className="flex items-center gap-2 rounded-lg border-l border-border-subtle py-1 pl-2 pr-2 transition-colors hover:bg-bg-elevated"
              aria-haspopup="menu"
              aria-expanded={accountOpen}
              aria-label="Account menu"
            >
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-bg-elevated text-xs font-medium text-text-secondary">
                {getInitials(user?.fullName)}
              </span>
              <span className="hidden text-sm text-text-secondary sm:inline">
                {user?.fullName}
              </span>
              <Menu className="h-6 w-6 text-text-muted" strokeWidth={1.5} />
            </button>

            {accountOpen && (
              <div className="absolute right-0 top-full z-50 mt-2 w-64 rounded-lg border border-border-subtle bg-bg-card shadow-lg">
                {/* Profile header */}
                <div className="border-b border-border-subtle px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-bg-elevated text-sm font-semibold text-text-primary ring-2 ring-accent/30">
                      {getInitials(user?.fullName)}
                    </span>
                    <span className="badge badge-neutral shrink-0">{user?.role}</span>
                  </div>
                  <div className="mt-2.5 space-y-1.5">
                    <p className="flex items-center gap-1.5 text-xs text-text-muted">
                      <CalendarDays className="h-3.5 w-3.5 shrink-0" />
                      <span>Joined {formatDateTime(user?.createdAt)}</span>
                    </p>
                    <p className="flex items-center gap-1.5 text-xs text-text-muted">
                      <Building2 className="h-3.5 w-3.5 shrink-0" />
                      <span>
                        Department:{" "}
                        <span className="text-text-secondary">
                          {user?.departmentName || "—"}
                        </span>
                      </span>
                    </p>
                  </div>
                </div>

                {/* Actions */}
                <div role="menu" className="py-1">
                  <Link
                    href="/profile"
                    role="menuitem"
                    onClick={() => setAccountOpen(false)}
                    className="flex items-center gap-2 px-3 py-2 text-sm text-text-primary transition-colors hover:bg-bg-elevated"
                  >
                    <User className="h-4 w-4 text-text-muted" />
                    Profile
                  </Link>
                  <Link
                    href="/file-storage/trash"
                    role="menuitem"
                    onClick={() => setAccountOpen(false)}
                    className="flex items-center gap-2 px-3 py-2 text-sm text-text-primary transition-colors hover:bg-bg-elevated"
                  >
                    <Trash2 className="h-4 w-4 text-text-muted" />
                    Trash
                  </Link>
                  {isAdmin(user) && (
                    <Link
                      href="/admin/users"
                      role="menuitem"
                      onClick={() => setAccountOpen(false)}
                      className="flex items-center gap-2 px-3 py-2 text-sm text-text-primary transition-colors hover:bg-bg-elevated"
                    >
                      <ShieldCheck className="h-4 w-4 text-text-muted" />
                      Admin
                    </Link>
                  )}
                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setAccountOpen(false);
                      logout();
                    }}
                    className="flex w-full items-center gap-2 px-3 py-2 text-sm text-error transition-colors hover:bg-bg-elevated"
                  >
                    <LogOut className="h-4 w-4" />
                    Logout
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* .btn-ghost is unlayered CSS (globals.css) so it beats the layered
              `lg:hidden` utility if put on the same element — wrap it instead. */}
          <div className="lg:hidden">
            <button
              type="button"
              className="btn-ghost"
              aria-label="Open menu"
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen((v) => !v)}
            >
              <Menu className="h-5 w-5" />
            </button>
          </div>
        </div>
      </nav>

      {/* Mobile Navigation */}
      <div
        id="mobile-nav-panel"
        className={`border-t border-border-subtle bg-bg-secondary px-4 py-4 lg:hidden ${
          mobileOpen ? "is-open" : ""
        }`}
      >
        <div className="flex flex-col gap-1">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setMobileOpen(false)}
              className={`btn-ghost justify-start px-3 py-2.5 text-sm ${
                isActive(pathname, link.href) ? "nav-link-active" : ""
              }`}
            >
              {link.label}
            </Link>
          ))}
        </div>
      </div>
    </header>
  );
}
