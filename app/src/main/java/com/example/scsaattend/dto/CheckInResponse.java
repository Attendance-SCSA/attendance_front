package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class CheckInResponse {
    @SerializedName("attendanceId")
    private int attendanceId;

    @SerializedName("newArrivalTime")
    private String newArrivalTime;

    @SerializedName("status")
    private String status;

    public int getAttendanceId() {
        return attendanceId;
    }

    public String getNewArrivalTime() {
        return newArrivalTime;
    }

    public String getStatus() {
        return status;
    }
}