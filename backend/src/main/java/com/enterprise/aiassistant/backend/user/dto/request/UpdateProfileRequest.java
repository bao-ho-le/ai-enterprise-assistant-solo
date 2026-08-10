package com.enterprise.aiassistant.backend.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

// UpdateProfileRequest.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    Secrets Detected
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    // Không cho sửa username, password ở đây — tách riêng endpoint đổi password
}