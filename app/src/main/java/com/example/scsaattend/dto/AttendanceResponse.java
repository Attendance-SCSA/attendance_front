package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceResponse {
    @SerializedName("ainfoId")
    private int ainfoId;

    @SerializedName("aDate")
    private String aDate;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    @SerializedName("leavingTime")
    private String leavingTime;

    @SerializedName("isOff")
    private String isOff;

    @SerializedName("hasDoc")
    private String hasDoc;

    // Getters
    public int getAinfoId() { return ainfoId; }
    public String getADate() { return aDate; }
    public String getArrivalTime() { return arrivalTime; }
    public String getLeavingTime() { return leavingTime; }
    public String getIsOff() { return isOff; }
    public String getHasDoc() { return hasDoc; }
}