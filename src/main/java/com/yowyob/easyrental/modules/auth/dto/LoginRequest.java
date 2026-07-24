package com.yowyob.easyrental.modules.auth.dto;

public record LoginRequest(String email, String password, String source) {
    public LoginRequest(String email, String password) {
        this(email, password, null);
    }
}
