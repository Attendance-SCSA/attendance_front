package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchAttendanceRequest {

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("memIdList") // Changed from memberIds
    private List<Long> memIdList;

    public SearchAttendanceRequest(String startDate, String endDate, List<Long> memIdList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.memIdList = memIdList;
    }
}
