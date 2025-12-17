package com.example.scsaattend.dto;

public class AttendanceTypeRequest {
    private String name;
    private String earliestTime;
    private String startTime;
    private String endTime;
    private String latestTime;

    public AttendanceTypeRequest(String name, String earliestTime, String startTime, String endTime, String latestTime) {
        this.name = name;
        this.earliestTime = earliestTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.latestTime = latestTime;
    }
}
