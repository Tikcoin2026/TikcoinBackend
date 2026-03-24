package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {

        String deepLink;
        if (error != null) {
            deepLink = "tikcoin://auth/callback?error=" + error;
        } else {
            deepLink = "tikcoin://auth/callback?code=" + code + "&state=" + state;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(deepLink));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponse>> login(
            @Valid @RequestBody TikTokLoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
    }
}