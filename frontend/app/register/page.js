"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/AuthContext";
import { ApiError } from "@/lib/apiClient";
import AuthLayout from "@/features/auth/components/AuthLayout";

export default function RegisterPage() {
  const { register } = useAuth();
  const [form, setForm] = useState({
    fullName: "",
    userName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await register({
        fullName: form.fullName.trim(),
        userName: form.userName.trim(),
        email: form.email.trim(),
        password: form.password,
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Registration failed. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Create account" subtitle="Get started with your enterprise AI workspace.">
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="label-text">Full name</label>
          <input
            type="text"
            className="input-field"
            autoComplete="name"
            value={form.fullName}
            onChange={set("fullName")}
            required
          />
        </div>
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
          <label className="label-text">Email</label>
          <input
            type="email"
            className="input-field"
            autoComplete="email"
            value={form.email}
            onChange={set("email")}
            required
          />
        </div>
        <div>
          <label className="label-text">Password</label>
          <input
            type="password"
            className="input-field"
            autoComplete="new-password"
            value={form.password}
            onChange={set("password")}
            required
            minLength={8}
          />
        </div>
        <div>
          <label className="label-text">Confirm password</label>
          <input
            type="password"
            className="input-field"
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={set("confirmPassword")}
            required
            minLength={8}
          />
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <button type="submit" className="btn-primary w-full text-sm" disabled={submitting}>
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-text-secondary">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-accent hover:text-accent-hover">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
