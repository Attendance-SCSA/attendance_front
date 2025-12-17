package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("id")
    private long id; // int에서 long으로 변경하여 더 큰 값을 담을 수 있도록 함

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

    public String getLoginId() {
        return loginId;
    }
    
    public String getName() {
        return name;
    }

    public long getId() { // getId() 메소드 추가
        return id;
    }
}