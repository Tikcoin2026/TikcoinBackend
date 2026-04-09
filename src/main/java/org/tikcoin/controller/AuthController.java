package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.TikTokLoginRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.AuthResponse;
import org.tikcoin.service.AuthService;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.web.callback-url}")
    private String webCallbackUrl;

    @GetMapping("/tiktok/authorize")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> getAuthorizationUrl(
            @RequestParam String redirectUri,
            @RequestParam(defaultValue = "MOBILE") String platform) {
        Map<String, String> result = authService.buildAuthorizationUrl(redirectUri, platform);
        return ResponseEntity.ok(ApiResponseDto.success("TikTok authorization URL generated", result));
    }

    @GetMapping("/tiktok/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {


        String platform = "MOBILE";
        if (state != null && state.contains(".")) {
            platform = state.substring(state.lastIndexOf('.') + 1).toUpperCase();
        }

        String redirectLocation;
        if ("WEB".equals(platform)) {
            if (error != null) {
                redirectLocation = webCallbackUrl + "?error=" + error;
            } else {
                redirectLocation = webCallbackUrl + "?code=" + code + "&state=" + state;
            }
        } else {
            if (error != null) {
                redirectLocation = "tikcoin://auth/callback?error=" + error;
            } else {
                redirectLocation = "tikcoin://auth/callback?code=" + code + "&state=" + state;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectLocation));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponse>> login(
            @Valid @RequestBody TikTokLoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<AuthResponse>> refresh(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDto.error("Refresh token is required"));
        }
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(ApiResponseDto.success("Token refreshed successfully", response));
    }
}