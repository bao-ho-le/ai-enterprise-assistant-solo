package com.enterprise.aiassistant.backend.auth.dto.response;

// Bridges service -> controller only. refreshToken never enters AuthResponse,
// so it can never serialize into the JSON body the browser's JS can read.
public record AuthResult(AuthResponse body, String refreshToken) {
}
