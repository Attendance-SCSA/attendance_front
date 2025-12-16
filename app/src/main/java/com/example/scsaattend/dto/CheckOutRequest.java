package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class CheckOutRequest {
    @SerializedName("leavingTime")
    private String leavingTime;

    @SerializedName("beaconData")
    private BeaconData beaconData;

    public CheckOutRequest(String leavingTime, String macAddress, int rssi) {
        this.leavingTime = leavingTime;
        this.beaconData = new BeaconData(macAddress, rssi);
    }

    public static class BeaconData {
        @SerializedName("macAddress")
        private String macAddress;

        @SerializedName("rssi")
        private int rssi;

        public BeaconData(String macAddress, int rssi) {
            this.macAddress = macAddress;
            this.rssi = rssi;
        }
    }
}