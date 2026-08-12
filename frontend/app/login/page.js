"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/AuthContext";
import { ApiError } from "@/lib/apiClient";
import AuthLayout from "@/features/auth/components/AuthLayout";

export default function LoginPage() {
  const { login } = useAuth();
  const [form, setForm] = useState({ userName: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await login(form.userName.trim(), form.password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Sign in" subtitle="Welcome back. Enter your details to continue.">
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="label-text">Username</label>
          <input
            type="text"
            className="input-field"
            autoComplete="username"
            value={form.userName}
            onChange={set("userName")}
            required
          />
        </div>
        <div>
          <label className="label-text">Password</label>
          <input
            type="password"
            className="input-field"
            autoComplete="current-password"
            value={form.password}
            onChange={set("password")}
            required
            minLength={8}
          />
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <button type="submit" className="btn-primary w-full text-sm" disabled={submitting}>
          {submitting ? "Signing in…" : "Sign in"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-text-secondary">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="font-medium text-accent hover:text-accent-hover">
          Sign up
        </Link>
      </p>
    </AuthLayout>
  );
}
