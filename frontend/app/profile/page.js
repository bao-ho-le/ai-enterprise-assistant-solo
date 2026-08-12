"use client";

import { useState } from "react";
import { CalendarDays, Building2, User } from "lucide-react";
import { useAuth } from "@/lib/AuthContext";
import { updateProfile } from "@/services/userService";
import { ApiError } from "@/lib/apiClient";
import { getInitials, formatDateTime } from "@/utils/format";

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ fullName: user?.fullName || "", email: user?.email || "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const updated = await updateProfile({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
      });
      setUser(updated);
      setSuccess("Profile updated");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Update failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-lg px-6 py-12">
      <h1 className="text-xl font-semibold text-text-primary">Profile</h1>
      <p className="mt-1.5 text-sm text-text-secondary">Manage your account information.</p>

      <div className="card mt-6 p-6">
        <div className="flex items-center gap-4">
          <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-bg-elevated text-xl font-semibold text-text-primary ring-2 ring-accent/30">
            {getInitials(user?.fullName)}
          </span>
          <div className="min-w-0">
            <p className="flex flex-wrap items-center gap-1.5 text-xs text-text-muted">
              <User className="h-3.5 w-3.5 shrink-0" />
              <span>
                Username:{" "}
                <span className="text-text-secondary">{user?.username}</span>
              </span>
              <span className="badge badge-accent">{user?.role}</span>
            </p>
            <p className="mt-1.5 flex items-center gap-1.5 text-xs text-text-muted">
              <CalendarDays className="h-3.5 w-3.5 shrink-0" />
              Joined {formatDateTime(user?.createdAt)}
            </p>
            <p className="mt-1.5 flex items-center gap-1.5 text-xs text-text-muted">
              <Building2 className="h-3.5 w-3.5 shrink-0" />
              <span>
                Department:{" "}
                <span className="text-text-secondary">{user?.departmentName || "—"}</span>
              </span>
            </p>
          </div>
        </div>

        <form className="mt-6 space-y-4" onSubmit={submit}>
          <div>
            <label className="label-text">Full name</label>
            <input type="text" className="input-field" value={form.fullName} onChange={set("fullName")} required />
          </div>
          <div>
            <label className="label-text">Email</label>
            <input type="email" className="input-field" value={form.email} onChange={set("email")} required />
          </div>

          {error && <p className="text-sm text-error">{error}</p>}
          {success && <p className="text-sm text-success">{success}</p>}

          <div className="flex justify-end pt-2">
            <button type="submit" className="btn-primary text-sm" disabled={submitting}>
              {submitting ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
