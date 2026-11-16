package com.terraledger.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements Principal {
    private final String id;
    private final String email;
    private final String role;

    public UserPrincipal(String id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public boolean isOfficerOrAdmin() {
        return "OFFICER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)
                || "ROLE_OFFICER".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return List.of(new SimpleGrantedAuthority(authName));
    }

    @Override
    public String getName() {
        return email;
    }
}
