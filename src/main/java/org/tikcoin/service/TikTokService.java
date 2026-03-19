package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.tikcoin.exception.BadRequestException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TikTokService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokService.class);

    private static final String TIKTOK_AUTH_URL = "https://www.tiktok.com/v2/auth/authorize/";
    private static final String TIKTOK_TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/";
    private static final String TIKTOK_USER_INFO_URL =
            "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";

    @Value("${tiktok.client-key}")
    private String clientKey;

    @Value("${tiktok.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public String generateCodeVerifier() {
        byte[] bytes = new byte[96];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    public String buildAuthorizationUrl(String redirectUri, String state, String codeChallenge) {
        return TIKTOK_AUTH_URL
                + "?client_key=" + encode(clientKey)
                + "&scope=user.info.basic"
                + "&response_type=code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    public Map<String, Object> exchangeCodeForTokens(String code, String codeVerifier, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_key=" + encode(clientKey)
                + "&client_secret=" + encode(clientSecret)
                + "&code=" + encode(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&code_verifier=" + encode(codeVerifier);

        logger.info("TikTok token exchange — client_key={}, redirect_uri={}, code_length={}, verifier_length={}",
                clientKey, redirectUri, code.length(), codeVerifier.length());

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TIKTOK_TOKEN_URL, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.containsKey("error")) {
                logger.error("TikTok token exchange failed: {}", responseBody);
                String desc = responseBody != null ? String.valueOf(responseBody.get("error_description")) : "null response";
                throw new BadRequestException("TikTok authentication failed: " + desc);
            }
            return responseBody;
        } catch (BadRequestException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            logger.error("TikTok token exchange HTTP error {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("TikTok token exchange failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("TikTok token exchange unexpected error", e);
            throw new BadRequestException("Failed to exchange TikTok authorization code: " + e.getMessage());
        }
    }

    public Map<String, Object> fetchUserInfo(String tiktokAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tiktokAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TIKTOK_USER_INFO_URL, HttpMethod.GET, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BadRequestException("Empty response from TikTok user info API");
            }
            Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
            if (error != null && !"ok".equals(error.get("code"))) {
                logger.error("TikTok user info error: {}", error);
                throw new BadRequestException("Failed to fetch TikTok user info");
            }
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            return (Map<String, Object>) data.get("user");
        } catch (BadRequestException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            logger.error("TikTok user info HTTP error {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Failed to fetch TikTok user info: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error fetching TikTok user info", e);
            throw new BadRequestException("Failed to fetch TikTok user info: " + e.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}