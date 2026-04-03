package org.tikcoin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Table(name = "admins")
@Getter
@Setter
public class Admin extends BaseUser {

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;
}