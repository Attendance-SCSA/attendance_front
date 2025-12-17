package com.example.scsaattend.network;

import com.example.scsaattend.dto.TodayMyAttendanceRequest;
import com.example.scsaattend.dto.TodayMyAttendanceResponse;
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.dto.LoginRequest;
import com.example.scsaattend.dto.LoginResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("attendance_info/search")
    Call<List<TodayMyAttendanceResponse>> getAttendance(@Body TodayMyAttendanceRequest request);

    // 출근 요청 (빈 JSON 바디 전송)
    @PATCH("attendance_info/{aInfoId}/arrival")
    Call<CheckInResponse> checkIn(@Path("aInfoId") int aInfoId, @Body Object emptyBody);

    // 퇴근 요청
    @PATCH("attendance_info/{aInfoId}/leaving")
    Call<CheckInResponse> checkOut(@Path("aInfoId") int aInfoId, @Body CheckOutRequest request);
}