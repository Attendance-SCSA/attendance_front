package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class CheckInApiResponse {

    @SerializedName("attendanceInfo")
    private AttendanceInfo attendanceInfo;

    public AttendanceInfo getAttendanceInfo() {
        return attendanceInfo;
    }

    public static class AttendanceInfo {
        @SerializedName("arrivalTime")
        private String arrivalTime;

        @SerializedName("leavingTime")
        private String leavingTime;

        public String getArrivalTime() {
            return arrivalTime;
        }

        public String getLeavingTime() {
            return leavingTime;
        }
    }
}
