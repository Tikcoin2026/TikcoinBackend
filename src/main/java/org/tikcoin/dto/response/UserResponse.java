package org.tikcoin.dto.response;

import lombok.Builder;
import lombok.Data;
import org.tikcoin.enums.UserRole;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String tiktokOpenId;
    private String displayName;
    private String tiktokHandle;
    private String avatarUrl;
    private UserRole role;
}