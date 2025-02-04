package dev.gunho.api.user.constant;

import lombok.Getter;

@Getter
public enum UserRole {

    DEFAULT("DEFAULT");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

}
