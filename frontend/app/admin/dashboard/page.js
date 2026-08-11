"use client";

import { useCallback, useEffect, useState } from "react";
import {
  Activity,
  AlertCircle,
  Building2,
  Coins,
  FileText,
  Loader2,
  Trash2,
  UserCheck,
  Users,
} from "lucide-react";
import { getAdminDashboard } from "@/services/adminService";

const CARDS = [
  { key: "totalUsers", label: "Total Users", Icon: Users, iconColor: "text-accent" },
  { key: "activeUsers", label: "Active Users", Icon: UserCheck, iconColor: "text-emerald-400" },
  { key: "totalDepartments", label: "Departments", Icon: Building2, iconColor: "text-purple-400" },
  { key: "totalDocuments", label: "Documents", Icon: FileText, iconColor: "text-blue-400" },
  { key: "documentsInTrash", label: "Documents in Trash", Icon: Trash2, iconColor: "text-amber-400" },
  { key: "aiRequestCount", label: "AI Requests", Icon: Activity, iconColor: "text-pink-400" },
  { key: "aiTotalTokens", label: "AI Token Usage", Icon: Coins, iconColor: "text-cyan-400" },
];

export default function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminDashboard()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const maxDepartmentTokens = Math.max(
    1,
    ...(data?.usageByDepartment ?? []).map((row) => row.totalTokens ?? 0)
  );

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5">
        <h1 className="text-lg font-semibold text-text-primary">Dashboard</h1>
        <p className="mt-1 text-sm text-text-muted">Tổng quan toàn hệ thống.</p>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-24">
          <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
        </div>
      )}

      {!loading && error && (
        <div className="card flex flex-col items-center gap-3 p-10 text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-error">{error}</p>
          <button type="button" className="btn-secondary" onClick={load}>
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && data && (
        <>
          <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {CARDS.map(({ key, label, Icon, iconColor }) => (
              <article key={key} className="card p-6">
                <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg border border-border-subtle bg-bg-elevated">
                  <Icon className={`h-5 w-5 ${iconColor}`} />
                </div>
                <p className="mb-1 text-sm text-text-muted">{label}</p>
                <p className="text-2xl font-bold tracking-tight text-text-primary">
                  {(data[key] ?? 0).toLocaleString()}
                </p>
              </article>
            ))}
          </div>

          {/* Plain divs sized by percentage — no chart library for a handful of bars. */}
          <section className="card p-6">
            <h2 className="mb-4 text-sm font-semibold text-text-primary">AI usage by department</h2>
            {(data.usageByDepartment ?? []).length === 0 ? (
              <p className="py-6 text-center text-sm text-text-muted">Chưa có dữ liệu sử dụng.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {data.usageByDepartment.map((row) => (
                  <div key={row.id} className="flex items-center gap-4">
                    <span className="w-32 shrink-0 truncate text-xs text-text-secondary">
                      {row.name || `#${row.id}`}
                    </span>
                    <div className="h-2 min-w-0 flex-1 rounded-full bg-bg-elevated">
                      <div
                        className="h-2 rounded-full bg-accent"
                        style={{ width: `${((row.totalTokens ?? 0) / maxDepartmentTokens) * 100}%` }}
                      />
                    </div>
                    <span className="w-28 shrink-0 text-right text-xs tabular-nums text-text-secondary">
                      {(row.totalTokens ?? 0).toLocaleString()} tokens
                    </span>
                    <span className="w-24 shrink-0 text-right text-xs tabular-nums text-text-muted">
                      {(row.requestCount ?? 0).toLocaleString()} reqs
                    </span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </main>
  );
}
