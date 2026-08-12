// Thin fetch wrapper: base URL from env, ErrorResponseDto -> ApiError, JSON + raw (blob) support.
// Every request in the app goes through here (via services/*), never fetch() in components.

import { getAccessToken, setTokens, clearTokens } from "@/lib/auth";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body; // parsed ErrorResponseDto when available
  }
}

function buildQuery(params) {
  if (!params) return "";
  const qs = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === "") continue;
    qs.append(key, value);
  }
  const s = qs.toString();
  return s ? `?${s}` : "";
}

async function toError(res) {
  let body = null;
  let message = `Request failed (${res.status})`;
  try {
    body = await res.json();
    if (body && body.message) message = body.message;
  } catch {
    // non-JSON error body — keep default message
  }
  return new ApiError(message, res.status, body);
}

// Single-flight refresh: concurrent 401s share one in-flight refresh call
// instead of each firing their own. Bare fetch (not apiClient) to avoid
// importing authService, which itself imports apiClient. No body/token to
// send — the refresh token travels as an httpOnly cookie the browser attaches
// automatically via credentials: "include".
let refreshPromise = null;

function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = fetch(`${BASE_URL}/auth/refresh-token`, {
      method: "POST",
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) throw await toError(res);
        return res.json();
      })
      .then(setTokens)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

async function request(path, { method = "GET", params, body, headers, signal, _retried } = {}) {
  const token = getAccessToken();
  const res = await fetch(`${BASE_URL}${path}${buildQuery(params)}`, {
    method,
    body,
    headers: token ? { ...headers, Authorization: `Bearer ${token}` } : headers,
    signal,
    credentials: "include",
  });
  if (res.status === 401 && !path.startsWith("/auth/")) {
    if (!_retried) {
      try {
        await refreshAccessToken();
        return request(path, { method, params, body, headers, signal, _retried: true });
      } catch {
        // refresh token invalid/expired — fall through to real logout
      }
    }
    clearTokens();
    if (typeof window !== "undefined") window.location.href = "/login";
  }
  if (!res.ok) throw await toError(res);
  return res;
}

async function json(path, opts) {
  const res = await request(path, opts);
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const apiClient = {
  BASE_URL,

  get: (path, { params, signal } = {}) => json(path, { params, signal }),

  // Raw Response, for downloads (blob + headers)
  getRaw: (path, { params, signal } = {}) =>
    request(path, { params, signal }),

  // JSON body
  postJson: (path, data) =>
    json(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }),

  putJson: (path, data) =>
    json(path, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }),

  // multipart/form-data — do NOT set Content-Type (browser adds boundary)
  postForm: (path, formData) =>
    json(path, { method: "POST", body: formData }),

  del: (path) => json(path, { method: "DELETE" }),
};
