package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("loginId")
    private String loginId;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role;

    public long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
