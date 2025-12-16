package com.example.scsaattend.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AttendanceRequest {

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("memIdList")
    private List<Long> memIdList;

    public AttendanceRequest(String startDate, String endDate, List<Long> memIdList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.memIdList = memIdList;
    }
}
