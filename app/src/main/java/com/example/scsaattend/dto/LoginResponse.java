package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("loginId")
    private String loginId;

    @SerializedName("loginPwd")
    private String loginPwd;

    @SerializedName("name")
    private String name;

    @SerializedName("company")
    private String company;

    @SerializedName("role")
    private String role;

    @SerializedName("startDay")
    private String startDay;

    @SerializedName("endDay")
    private String endDay;

    public String getRole() {
        return role;
    }

    public int getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }
    
    public String getName() {
        return name;
    }
}