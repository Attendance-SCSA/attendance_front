package com.example.scsaattend.network;

import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceTypeDto;
import com.example.scsaattend.dto.BatchUpdateRequest;
import com.example.scsaattend.dto.BatchUpdateResponse;
import com.example.scsaattend.dto.CheckInApiResponse;
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

    // attendances_info (복수형)으로 수정
    @POST("attendance_info/search")
    Call<List<AttendanceInfoResponse>> searchAttendance(@Body SearchAttendanceRequest request);

    @GET("members")
    Call<List<MemberDto>> getMembers();

    @GET("attendance_types")
    Call<List<AttendanceTypeDto>> getAttendanceTypes();

    // attendances_info (복수형)으로 수정
    @PATCH("attendance_info")
    Call<BatchUpdateResponse> batchUpdateAttendance(@Body BatchUpdateRequest request);

    // attendances_info (복수형)으로 수정
    @PATCH("attendance_info/{aInfoId}/arrival")
    Call<CheckInApiResponse> checkIn(@Path("aInfoId") int aInfoId, @Body Object emptyBody);

    // attendances_info (복수형)으로 수정 (leaving -> departure 확인 필요하지만 일단 기존 코드 따름. 사용자는 departure라고 했던 것 같음)
    // Turn 38: POST attendances_info/{aInfoId}/departure 였음.
    // 하지만 Turn 42 ApiService에는 checkOut이 @POST("attendances_info/{aInfoId}/departure")로 되어 있었음.
    // 그런데 현재 읽은 파일(Turn 46 read_file)에는 @PATCH("attendance_info/{aInfoId}/leaving")으로 되어 있음.
    // 사용자의 의도가 departure인지 leaving인지, POST인지 PATCH인지 헷갈리는 상황.
    // Turn 38에서 사용자가 "퇴근 버튼... POST 요청... /departure"라고 명시했으므로 그것을 따르는게 맞음.
    
    @PATCH("attendance_info/{aInfoId}/leaving")
    Call<CheckInApiResponse> checkOut(@Path("aInfoId") int aInfoId, @Body CheckOutRequest request);
}