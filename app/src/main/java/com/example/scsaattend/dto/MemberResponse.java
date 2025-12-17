package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class MemberResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("loginId")
    private String loginId;

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

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getName() { return name; }
    public String getCompany() { return company; }
    public String getStartDay() { return startDay; }
    public String getEndDay() { return endDay; }
}