package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceResponse {

    @SerializedName("ainfoId")
    private int ainfoId;

    @SerializedName("aDate")
    private String aDate;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    public int getAinfoId() {
        return ainfoId;
    }

    public String getADate() {
        return aDate;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }
}
