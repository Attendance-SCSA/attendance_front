package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TodayMyAttendanceRequest {
    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("memIdList")
    private List<Integer> memIdList;

    public TodayMyAttendanceRequest(String startDate, String endDate, List<Integer> memIdList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.memIdList = memIdList;
    }
}