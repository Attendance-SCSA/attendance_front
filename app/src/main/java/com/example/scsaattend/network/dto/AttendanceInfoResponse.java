package com.example.scsaattend.network.dto;

import com.google.gson.annotations.SerializedName;

// API 응답 리스트의 각 항목을 나타내는 클래스
public class AttendanceInfoResponse {

    @SerializedName("ainfoId")
    private long ainfoId;

    @SerializedName("member")
    private MemberDto member;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    @SerializedName("leavingTime")
    private String leavingTime;

    @SerializedName("status")
    private String status;

    @SerializedName("isApproved")
    private String isApproved; // "denied" 등의 문자열이 올 수 있으므로 String

    @SerializedName("isOfficial")
    private String isOfficial;

    @SerializedName("aDate")
    private String aDate;

    // Getters
    public long getAinfoId() { return ainfoId; }
    public MemberDto getMember() { return member; }
    public String getArrivalTime() { return arrivalTime; }
    public String getLeavingTime() { return leavingTime; }
    public String getStatus() { return status; }
    public String getIsApproved() { return isApproved; }
    public String getIsOfficial() { return isOfficial; }
    public String getADate() { return aDate; }
}
