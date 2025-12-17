package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceTypeDto {
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("earliestTime")
    private String earliestTime;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("latestTime")
    private String latestTime;

    // Getters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEarliestTime() {
        return earliestTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getLatestTime() {
        return latestTime;
    }
}
