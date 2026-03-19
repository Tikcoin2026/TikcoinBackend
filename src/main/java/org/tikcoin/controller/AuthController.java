package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.TikTokLoginRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.AuthResponse;
import org.tikcoin.service.AuthService;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/tiktok/authorize")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> getAuthorizationUrl(
            @RequestParam String redirectUri) {
        Map<String, String> result = authService.buildAuthorizationUrl(redirectUri);
        return ResponseEntity.ok(ApiResponseDto.success("TikTok authorization URL generated", result));
    }

    @GetMapping("/tiktok/callback")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("TikTok authorization failed: " + error_description));
        }
        Map<String, String> result = new HashMap<>();
        result.put("code", code);
        result.put("state", state);
        result.put("hint", "Copy the code above and use it in POST /api/auth/login");
        return ResponseEntity.ok(ApiResponseDto.success("Authorization code received", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponse>> login(
            @Valid @RequestBody TikTokLoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
    }
}