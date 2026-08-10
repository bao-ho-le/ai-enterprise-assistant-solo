package com.enterprise.aiassistant.backend.user.service;

import com.enterprise.aiassistant.backend.user.dto.request.UpdateProfileRequest;
import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String username);
    UserResponse updateProfile(String username, UpdateProfileRequest request);
}