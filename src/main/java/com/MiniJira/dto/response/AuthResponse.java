package com.minijira.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;
}
