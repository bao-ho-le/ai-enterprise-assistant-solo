// Access token only — localStorage, read synchronously on every request by
// apiClient. The refresh token now lives in an httpOnly cookie, invisible to JS.
const ACCESS_KEY = "auth_access_token";
// Legacy key from before the httpOnly-cookie migration — still purged on
// clearTokens() so already-logged-in users don't keep a JS-readable refresh
// token sitting in localStorage forever.
const LEGACY_REFRESH_KEY = "auth_refresh_token";

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(ACCESS_KEY);
}

export function setTokens({ accessToken }) {
  localStorage.setItem(ACCESS_KEY, accessToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(LEGACY_REFRESH_KEY);
}
