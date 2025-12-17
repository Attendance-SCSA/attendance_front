package com.example.scsaattend.network;

import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceTypeDto;
import com.example.scsaattend.dto.BatchUpdateRequest;
import com.example.scsaattend.dto.BatchUpdateResponse;
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.dto.LoginRequest;
import com.example.scsaattend.dto.LoginResponse;
import com.example.scsaattend.dto.MemberDto;
import com.example.scsaattend.dto.SearchAttendanceRequest;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("attendance_info/search")
    Call<List<AttendanceInfoResponse>> searchAttendance(@Body SearchAttendanceRequest request);

    @GET("members")
    Call<List<MemberDto>> getMembers();

    @GET("attendance_types")
    Call<List<AttendanceTypeDto>> getAttendanceTypes();

    @PATCH("attendance_info")
    Call<BatchUpdateResponse> batchUpdateAttendance(@Body BatchUpdateRequest request);

    @PATCH("attendance_info/{aInfoId}/arrival")
    Call<CheckInResponse> checkIn(@Path("aInfoId") int aInfoId, @Body Object emptyBody);

    @PATCH("attendance_info/{aInfoId}/departure")
    Call<CheckInResponse> checkOut(@Path("aInfoId") int aInfoId, @Body CheckOutRequest request);
}
