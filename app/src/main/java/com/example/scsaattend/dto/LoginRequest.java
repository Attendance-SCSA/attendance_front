package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("loginId")
    private String loginId;

    @SerializedName("loginPwd")
    private String loginPwd;

    public LoginRequest(String loginId, String loginPwd) {
        this.loginId = loginId;
        this.loginPwd = loginPwd;
    }
}