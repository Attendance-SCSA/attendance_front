package com.example.scsaattend.network;

import com.example.scsaattend.network.dto.AttendanceInfoResponse;
import com.example.scsaattend.network.dto.AttendanceRequest;
import com.example.scsaattend.network.dto.LoginRequest;
import com.example.scsaattend.network.dto.LoginResponse;
import com.example.scsaattend.network.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("members")
    Call<List<MemberDto>> getMembers();

    @POST("attendance_info/search")
    Call<List<AttendanceInfoResponse>> searchAttendance(@Body AttendanceRequest request);
}