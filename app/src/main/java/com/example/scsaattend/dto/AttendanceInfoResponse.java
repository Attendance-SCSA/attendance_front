package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceInfoResponse {

    @SerializedName("ainfoId")
    private long ainfoId;

    @SerializedName("member")
    private MemberResponse member;

    @SerializedName("atype")
    private AttendanceTypeResponse attendanceType;

    @SerializedName("isOff")
    private String isOff;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    @SerializedName("leavingTime")
    private String leavingTime;

    @SerializedName("status")
    private String status;

    @SerializedName("isApproved")
    private String isApproved;

    @SerializedName("isOfficial")
    private String isOfficial;

    @SerializedName("aDate")
    private String aDate;

    @SerializedName("memNote")
    private String memNote;

    @SerializedName("adminNote")
    private String adminNote;

    // Getters
    public long getAinfoId() { return ainfoId; }
    public MemberResponse getMember() { return member; }
    public AttendanceTypeResponse getAttendanceType() { return attendanceType; }
    public String getIsOff() { return isOff; }
    public String getArrivalTime() { return arrivalTime; }
    public String getLeavingTime() { return leavingTime; }
    public String getStatus() { return status; }
    public String getIsApproved() { return isApproved; }
    public String getIsOfficial() { return isOfficial; }
    public String getADate() { return aDate; }
    public String getMemNote() { return memNote; }
    public String getAdminNote() { return adminNote; }

    // Setters (필요한 것들 위주로 추가)
    public void setMemNote(String memNote) { this.memNote = memNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public void setStatus(String status) { this.status = status; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public void setLeavingTime(String leavingTime) { this.leavingTime = leavingTime; }
    public void setIsApproved(String isApproved) { this.isApproved = isApproved; }
    public void setIsOfficial(String isOfficial) { this.isOfficial = isOfficial; }
}
