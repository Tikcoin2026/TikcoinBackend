package org.tikcoin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TikTokLoginRequest {
    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "Code verifier is required")
    private String codeVerifier;

    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;
}