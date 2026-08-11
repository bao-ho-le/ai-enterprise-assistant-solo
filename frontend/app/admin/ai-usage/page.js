"use client";

import { useCallback, useEffect, useState } from "react";
import { Activity, AlertCircle, Coins, Loader2, Wallet } from "lucide-react";
import AdminTableState from "@/features/admin/components/AdminTableState";
import {
  getAdminDepartments,
  getAdminUsageLogs,
  getAdminUsageOverview,
  getAdminUsers,
} from "@/services/adminService";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function AdminAIUsagePage() {
  const [overview, setOverview] = useState(null);
  const [logs, setLogs] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);

  const [departments, setDepartments] = useState([]);
  const [users, setUsers] = useState([]);

  const [userId, setUserId] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const [loading, setLoading] = useState(true);
  const [logsLoading, setLogsLoading] = useState(true);
  const [error, setError] = useState("");
  const [logsError, setLogsError] = useState("");

  // Input type=date trả "YYYY-MM-DD", backend nhận LocalDateTime ISO.
  const params = useCallback(
    () => ({
      userId: userId || undefined,
      departmentId: departmentId || undefined,
      fromDate: fromDate ? `${fromDate}T00:00:00` : undefined,
      toDate: toDate ? `${toDate}T23:59:59` : undefined,
    }),
    [userId, departmentId, fromDate, toDate]
  );

  const loadOverview = useCallback(() => {
    setLoading(true);
    setError("");
    getAdminUsageOverview(params())
      .then(setOverview)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [params]);

  const loadLogs = useCallback(() => {
    setLogsLoading(true);
    setLogsError("");
    getAdminUsageLogs({ ...params(), page, size: PAGE_SIZE })
      .then((data) => {
        setLogs(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
      })
      .catch((e) => setLogsError(e.message))
      .finally(() => setLogsLoading(false));
  }, [params, page]);

  useEffect(loadOverview, [loadOverview]);
  useEffect(loadLogs, [loadLogs]);

  useEffect(() => {
    getAdminDepartments({ size: 100 })
      .then((data) => setDepartments(data.content ?? []))
      .catch(() => setDepartments([]));
    getAdminUsers({ size: 200 })
      .then((data) => setUsers(data.content ?? []))
      .catch(() => setUsers([]));
  }, []);

  const userName = (id) => users.find((u) => u.id === id)?.fullName;

  const cards = [
    { label: "Total Requests", value: overview?.totalRequests ?? 0, Icon: Activity, color: "text-accent" },
    { label: "Total Tokens", value: overview?.totalTokens ?? 0, Icon: Coins, color: "text-purple-400" },
    { label: "Total Cost", value: overview?.totalCost ?? 0, Icon: Wallet, color: "text-emerald-400", isCost: true },
  ];

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 pt-6 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5">
        <h1 className="text-lg font-semibold text-text-primary">AI Usage</h1>
        <p className="mt-1 text-sm text-text-muted">
          Thống kê sử dụng AI toàn hệ thống theo người dùng và phòng ban.
        </p>
      </div>

      <div className="filter-toolbar mb-4">
        <div className="filter-toolbar-item filter-toolbar-item--wide">
          <label className="label-text">User</label>
          <select
            className="select-field"
            value={userId}
            onChange={(e) => {
              setPage(0);
              setUserId(e.target.value);
            }}
          >
            <option value="">All</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--wide">
          <label className="label-text">Department</label>
          <select
            className="select-field"
            value={departmentId}
            onChange={(e) => {
              setPage(0);
              setDepartmentId(e.target.value);
            }}
          >
            <option value="">All</option>
            {departments.map((d) => (
              <option key={d.departmentId} value={d.departmentId}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--date">
          <label className="label-text">From</label>
          <input
            className="input-field"
            type="date"
            value={fromDate}
            onChange={(e) => {
              setPage(0);
              setFromDate(e.target.value);
            }}
          />
        </div>
        <div className="filter-toolbar-item filter-toolbar-item--date">
          <label className="label-text">To</label>
          <input
            className="input-field"
            type="date"
            value={toDate}
            onChange={(e) => {
              setPage(0);
              setToDate(e.target.value);
            }}
          />
        </div>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
        </div>
      )}

      {!loading && error && (
        <div className="card mb-4 flex flex-col items-center gap-3 p-10 text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-error">{error}</p>
          <button type="button" className="btn-secondary" onClick={loadOverview}>
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && overview && (
        <>
          <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
            {cards.map(({ label, value, Icon, color, isCost }) => (
              <article key={label} className="card p-6">
                <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg border border-border-subtle bg-bg-elevated">
                  <Icon className={`h-5 w-5 ${color}`} />
                </div>
                <p className="mb-1 text-sm text-text-muted">{label}</p>
                <p className="text-2xl font-bold tracking-tight text-text-primary">
                  {isCost ? `$${Number(value).toFixed(4)}` : Number(value).toLocaleString()}
                </p>
              </article>
            ))}
          </div>

          <div className="mb-8 grid grid-cols-1 gap-4 lg:grid-cols-2">
            {[
              { title: "Usage by user", rows: overview.byUser, fallback: userName },
              { title: "Usage by department", rows: overview.byDepartment, fallback: null },
            ].map(({ title, rows, fallback }) => (
              <section key={title} className="card p-6">
                <h2 className="mb-4 text-sm font-semibold text-text-primary">{title}</h2>
                {(rows ?? []).length === 0 ? (
                  <p className="py-6 text-center text-sm text-text-muted">Chưa có dữ liệu.</p>
                ) : (
                  <ul className="flex flex-col gap-2">
                    {rows.map((row) => (
                      <li key={row.id} className="flex items-center justify-between text-xs">
                        <span className="truncate text-text-secondary">
                          {row.name || fallback?.(row.id) || `#${row.id}`}
                        </span>
                        <span className="shrink-0 tabular-nums text-text-muted">
                          {row.requestCount.toLocaleString()} reqs ·{" "}
                          {row.totalTokens.toLocaleString()} tokens
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            ))}
          </div>
        </>
      )}

      <div className="overflow-x-auto rounded-xl border border-border-subtle bg-bg-primary">
        <table className="w-full min-w-[900px] border-collapse">
          <thead>
            <tr className="border-b border-border-default">
              {["Time", "User", "Type", "Model", "Input", "Output", "Total", "Status"].map((h) => (
                <th
                  key={h}
                  className="whitespace-nowrap px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <AdminTableState
              colSpan={8}
              loading={logsLoading}
              error={logsError}
              empty={!logsLoading && !logsError && logs.length === 0}
              emptyText="Chưa có log nào"
              onRetry={loadLogs}
            />

            {!logsLoading &&
              !logsError &&
              logs.map((log, index) => (
                <tr
                  key={`${log.createdAt}-${index}`}
                  className="border-b border-border-default transition-colors hover:bg-bg-elevated/50"
                >
                  <td className="whitespace-nowrap px-4 py-2 text-xs text-text-secondary">
                    {formatDateTime(log.createdAt)}
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">
                    {userName(log.userId) ?? "Hệ thống"}
                  </td>
                  <td className="px-4 py-2 text-xs text-text-secondary">{log.conversationType}</td>
                  <td className="px-4 py-2 text-xs text-text-secondary">{log.model}</td>
                  <td className="px-4 py-2 text-xs tabular-nums text-text-secondary">
                    {log.inputTokens ?? 0}
                  </td>
                  <td className="px-4 py-2 text-xs tabular-nums text-text-secondary">
                    {log.outputTokens ?? 0}
                  </td>
                  <td className="px-4 py-2 text-xs tabular-nums text-text-secondary">
                    {log.totalTokens ?? 0}
                  </td>
                  <td className="px-4 py-2">
                    <span
                      className={`badge ${log.status === "SUCCESS" ? "badge-success" : "badge-error"}`}
                    >
                      {log.status}
                    </span>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            className="btn-secondary"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Trước
          </button>
          <span className="text-xs text-text-muted">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="btn-secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </button>
        </div>
      )}
    </main>
  );
}
