package com.terraledger.dto;

public class AuthResponse {
    private String token;
    private String userId;
    private String email;
    private String role;
    private String walletAddress;

    public AuthResponse() {}

    public AuthResponse(String token, String userId, String email, String role, String walletAddress) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.walletAddress = walletAddress;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
}
