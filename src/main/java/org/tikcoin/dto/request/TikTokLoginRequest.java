package org.tikcoin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.tikcoin.enums.LoginPlatform;

@Data
public class TikTokLoginRequest {
    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "Code verifier is required")
    private String codeVerifier;

    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;

    @NotNull(message = "Platform is required (MOBILE or WEB)")
    private LoginPlatform platform;

    /** Optional: FCM device token. Required when platform=MOBILE and user is an admin. */
    private String fcmToken;
}