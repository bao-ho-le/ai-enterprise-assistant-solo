import { apiClient } from "@/lib/apiClient";

// POST /auth/login -> AuthResponse { accessToken, refreshToken, ... }
export function login(userName, password) {
  return apiClient.postJson("/auth/login", { userName, password });
}

// POST /auth/register -> AuthResponse (backend auto-logs-in on register)
export function register({ userName, email, password, fullName }) {
  return apiClient.postJson("/auth/register", { userName, email, password, fullName });
}

// POST /auth/logout -> revokes the refresh token
export function logout(refreshToken) {
  return apiClient.postJson("/auth/logout", { refreshToken });
}
