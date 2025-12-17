package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AttendanceRequest {

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("memberIds")
    private List<Long> memberIds;

    public AttendanceRequest(String startDate, String endDate, List<Long> memberIds) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.memberIds = memberIds;
    }
}
