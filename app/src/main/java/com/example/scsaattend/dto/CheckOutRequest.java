package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class CheckOutRequest {

    @SerializedName("leavingTime")
    private String leavingTime;

    @SerializedName("macAddress")
    private String macAddress;

    @SerializedName("rssi")
    private int rssi;

    public CheckOutRequest(String leavingTime, String macAddress, int rssi) {
        this.leavingTime = leavingTime;
        this.macAddress = macAddress;
        this.rssi = rssi;
    }
}
