package com.yowyob.easyrental.modules.auth.dto;

public record RegisterRequest(
    String firstname,
    String lastname,
    String email,
    String password) {}
