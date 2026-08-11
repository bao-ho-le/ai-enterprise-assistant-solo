"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  ArrowLeft,
  Building2,
  FileText,
  LayoutDashboard,
  ShieldCheck,
  Share2,
  Trash2,
  Users,
} from "lucide-react";
import { useAuth } from "@/lib/AuthContext";
import { isAdmin } from "@/lib/permissions";

const ADMIN_LINKS = [
  { href: "/admin/dashboard", label: "Dashboard", Icon: LayoutDashboard },
  { href: "/admin/users", label: "Users", Icon: Users },
  { href: "/admin/departments", label: "Departments", Icon: Building2 },
  { href: "/admin/documents", label: "Documents", Icon: FileText },
  { href: "/admin/shared-documents", label: "Shared Documents", Icon: Share2 },
  { href: "/admin/roles", label: "Roles", Icon: ShieldCheck },
  { href: "/admin/ai-usage", label: "AI Usage", Icon: Activity },
  { href: "/admin/trash", label: "Trash", Icon: Trash2 },
];

export default function AdminLayout({ children }) {
  const pathname = usePathname();
  const { user } = useAuth();

  // Chỉ ẩn UI. Mọi API /admin/** đã bị backend chặn theo role + permission + resource scope.
  if (!isAdmin(user)) {
    return (
      <main className="flex flex-1 items-center justify-center px-6">
        <div className="card max-w-md p-8 text-center">
          <ShieldCheck className="mx-auto mb-4 h-8 w-8 text-text-muted" />
          <h1 className="mb-2 text-base font-semibold text-text-primary">
            Không có quyền truy cập
          </h1>
          <p className="text-sm text-text-muted">
            Khu vực quản trị chỉ dành cho tài khoản ADMIN.
          </p>
          <Link href="/file-storage" className="btn-secondary mt-5 inline-flex">
            Về app chính
          </Link>
        </div>
      </main>
    );
  }

  return (
    <div className="flex min-h-0 flex-1">
      <aside className="flex w-56 shrink-0 flex-col gap-1 border-r border-border-subtle bg-bg-secondary px-3 py-4">
        <p className="px-3 pb-2 text-xs font-semibold uppercase tracking-wider text-text-muted">
          Admin
        </p>
        {ADMIN_LINKS.map(({ href, label, Icon }) => (
          <Link
            key={href}
            href={href}
            // Plain utilities, not .btn-ghost: that class is unlayered CSS and its
            // justify-content:center would beat a `justify-start` utility here.
            className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
              pathname.startsWith(href)
                ? "nav-link-active"
                : "text-text-secondary hover:bg-bg-elevated hover:text-text-primary"
            }`}
          >
            <Icon className="h-4 w-4" />
            {label}
          </Link>
        ))}
        <Link
          href="/file-storage"
          className="mt-auto flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-text-muted transition-colors hover:bg-bg-elevated hover:text-text-primary"
        >
          <ArrowLeft className="h-4 w-4" />
          Về app chính
        </Link>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col overflow-y-auto">{children}</div>
    </div>
  );
}
