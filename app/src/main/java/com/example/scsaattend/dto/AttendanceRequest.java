package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceRequest {
    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("memId")
    private int memId;

    @SerializedName("status")
    private String status;

    @SerializedName("isApproved")
    private String isApproved;

    @SerializedName("isOfficial")
    private String isOfficial;

    public AttendanceRequest(String startDate, String endDate, int memId) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.memId = memId;
        this.status = null;
        this.isApproved = null;
        this.isOfficial = null;
    }
}