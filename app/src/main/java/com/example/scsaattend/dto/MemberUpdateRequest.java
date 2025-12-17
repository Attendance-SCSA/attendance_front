package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class MemberUpdateRequest {
    @SerializedName("loginPwd")
    private String loginPwd;

    @SerializedName("name")
    private String name;

    @SerializedName("company")
    private String company;

    public MemberUpdateRequest(String loginPwd, String name, String company) {
        this.loginPwd = loginPwd;
        this.name = name;
        this.company = company;
    }
}