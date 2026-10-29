package com.infotech.lang.application.data.entity;

import java.util.ArrayList;
import java.util.List;

public enum UserRole {
    ADMIN("Admin", true),
    TEACHER("Teacher", true),
    STUDENT("Student", false);

    private final String label;
    private final boolean canManageUsers;

    UserRole(String label, boolean canManageUsers) {
        this.label = label;
        this.canManageUsers = canManageUsers;
    }

    public String getLabel() {
        return label;
    }

    public boolean canManageUsers() {
        return canManageUsers;
    }

    public List<String> getAuthorityNames() {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_USER");
        authorities.add("ROLE_" + name());
        if (canManageUsers) {
            authorities.add("ROLE_USER_MANAGER");
        }
        return List.copyOf(authorities);
    }

    public static UserRole defaultRole() {
        return STUDENT;
    }
}
