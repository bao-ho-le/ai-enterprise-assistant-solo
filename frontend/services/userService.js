import { apiClient } from "@/lib/apiClient";

// GET /users/me -> UserResponse
export function getMe(signal) {
  return apiClient.get("/users/me", { signal });
}

// PUT /users/me  { fullName, email } -> UserResponse
export function updateProfile({ fullName, email }) {
  return apiClient.putJson("/users/me", { fullName, email });
}
