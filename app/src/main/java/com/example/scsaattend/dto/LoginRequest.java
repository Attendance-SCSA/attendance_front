package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("loginId")
    private String loginId;

    @SerializedName("password")
    private String password;

    public LoginRequest(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }
}
