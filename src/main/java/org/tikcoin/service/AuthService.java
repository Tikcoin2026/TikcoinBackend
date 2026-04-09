package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tikcoin.dto.request.TikTokLoginRequest;
import org.tikcoin.dto.response.AuthResponse;
import org.tikcoin.dto.response.UserResponse;
import org.tikcoin.enums.LoginPlatform;
import org.tikcoin.enums.TokenType;
import org.tikcoin.enums.UserRole;
import org.tikcoin.exception.UnauthorizedException;
import org.tikcoin.model.Admin;
import org.tikcoin.model.Buyer;
import org.tikcoin.model.Token;
import org.tikcoin.repository.AdminRepository;
import org.tikcoin.repository.BuyerRepository;
import org.tikcoin.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TikTokService tikTokService;
    private final JwtService jwtService;
    private final BuyerRepository buyerRepository;
    private final AdminRepository adminRepository;
    private final TokenRepository tokenRepository;

    public Map<String, String> buildAuthorizationUrl(String redirectUri, String platform) {
        String codeVerifier = tikTokService.generateCodeVerifier();
        String codeChallenge = tikTokService.generateCodeChallenge(codeVerifier);
        String state = UUID.randomUUID().toString() + "." + (platform != null ? platform.toUpperCase() : "MOBILE");
        String authUrl = tikTokService.buildAuthorizationUrl(redirectUri, state, codeChallenge);
        return Map.of("authUrl", authUrl, "codeVerifier", codeVerifier, "state", state);
    }

    @Transactional
    public AuthResponse login(TikTokLoginRequest request) {
        Map<String, Object> tokenData = tikTokService.exchangeCodeForTokens(
                request.getCode(), request.getCodeVerifier(), request.getRedirectUri());

        String tiktokAccessToken = (String) tokenData.get("access_token");
        String tiktokRefreshToken = (String) tokenData.get("refresh_token");

        Map<String, Object> userInfo = tikTokService.fetchUserInfo(tiktokAccessToken);

        String openId      = (String) userInfo.get("open_id");
        String displayName = (String) userInfo.get("display_name");
        String avatarUrl   = (String) userInfo.get("avatar_url");
        String username    = (String) userInfo.get("username");

        if (request.getPlatform() == LoginPlatform.MOBILE) {
            Admin admin = adminRepository.findByTiktokOpenId(openId).orElse(null);

            if (admin == null) {
                buyerRepository.findByTiktokOpenId(openId).ifPresent(buyer -> {
                    buyer.setTiktokOpenId(null);
                    buyerRepository.save(buyer);
                    buyerRepository.flush();
                });

                admin = new Admin();
                admin.setTiktokOpenId(openId);
                admin.setRole(UserRole.ADMIN);
                admin.setCreatedAt(LocalDateTime.now());
            }

            admin.setTiktokUsername(displayName);
            admin.setTiktokHandle(username);
            admin.setProfilePicture(avatarUrl);
            admin.setUpdatedAt(LocalDateTime.now());

            if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
                admin.setFcmToken(request.getFcmToken());
            }

            adminRepository.save(admin);

            return issueTokens(admin.getUsername(), admin.getId(), openId, displayName,
                    username, avatarUrl, UserRole.ADMIN, tiktokRefreshToken);
        }

        Buyer buyer = buyerRepository.findByTiktokOpenId(openId).orElseGet(() -> {
            Buyer newBuyer = new Buyer();
            newBuyer.setTiktokOpenId(openId);
            newBuyer.setRole(UserRole.BUYER);
            newBuyer.setCreatedAt(LocalDateTime.now());
            return newBuyer;
        });

        buyer.setTiktokUsername(displayName);
        buyer.setTiktokHandle(username);
        buyer.setProfilePicture(avatarUrl);
        buyer.setUpdatedAt(LocalDateTime.now());
        buyerRepository.save(buyer);

        return issueTokens(buyer.getUsername(), buyer.getId(), openId, displayName,
                username, avatarUrl, UserRole.BUYER, tiktokRefreshToken);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isExpired() || storedToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        String subject = jwtService.extractUsername(refreshToken);
        if (subject == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Revoke the old refresh token
        storedToken.setRevoked(true);
        storedToken.setExpired(true);
        tokenRepository.save(storedToken);

        // Issue new access + refresh tokens
        String newAccessToken = jwtService.generateToken(subject);
        String newRefreshToken = jwtService.generateRefreshToken(subject);

        tokenRepository.save(Token.builder()
                .token(newAccessToken)
                .tokenType(TokenType.BEARER)
                .isRevoked(false)
                .isExpired(false)
                .userType(storedToken.getUserType())
                .build());

        tokenRepository.save(Token.builder()
                .token(newRefreshToken)
                .tokenType(TokenType.BEARER)
                .isRevoked(false)
                .isExpired(false)
                .userType(storedToken.getUserType())
                .build());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private AuthResponse issueTokens(String subject, Long userId, String openId,
                                     String displayName, String handle, String avatarUrl,
                                     UserRole role, String tiktokRefreshToken) {
        String accessToken = jwtService.generateToken(subject);
        String refreshToken = jwtService.generateRefreshToken(subject);

        tokenRepository.save(Token.builder()
                .token(accessToken)
                .tokenType(TokenType.BEARER)
                .isRevoked(false)
                .isExpired(false)
                .userType(role.name())
                .value(tiktokRefreshToken)
                .build());

        tokenRepository.save(Token.builder()
                .token(refreshToken)
                .tokenType(TokenType.BEARER)
                .isRevoked(false)
                .isExpired(false)
                .userType(role.name())
                .build());

        UserResponse user = UserResponse.builder()
                .id(userId)
                .tiktokOpenId(openId)
                .displayName(displayName)
                .tiktokHandle(handle)
                .avatarUrl(avatarUrl)
                .role(role)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }
}