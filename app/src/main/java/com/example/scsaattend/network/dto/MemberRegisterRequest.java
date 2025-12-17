package com.example.scsaattend.network.dto;

import com.google.gson.annotations.SerializedName;

public class MemberRegisterRequest {
    @SerializedName("loginId")
    private String loginId;

    @SerializedName("loginPwd")
    private String loginPwd;

    @SerializedName("name")
    private String name;

    @SerializedName("company")
    private String company;

    @SerializedName("startDay")
    private String startDay;

    @SerializedName("endDay")
    private String endDay;

    @SerializedName("role")
    private String role;

    public MemberRegisterRequest(String name, String loginId, String loginPwd, String company, String startDay, String endDay) {
        this.name = name;
        this.loginId = loginId;
        this.loginPwd = loginPwd;
        this.company = company;
        this.startDay = startDay;
        this.endDay = endDay;
        this.role = "USER"; // 기본값으로 일반 사용자 설정
    }
}