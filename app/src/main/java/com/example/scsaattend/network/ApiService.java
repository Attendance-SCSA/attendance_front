package com.example.scsaattend.network;

import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceTypeRequest;
import com.example.scsaattend.dto.AttendanceTypeResponse;
import com.example.scsaattend.dto.BatchUpdateRequest;
import com.example.scsaattend.dto.BatchUpdateResponse;
import com.example.scsaattend.dto.CheckInApiResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.dto.LoginRequest;
import com.example.scsaattend.dto.LoginResponse;
import com.example.scsaattend.dto.SearchAttendanceRequest;
import com.example.scsaattend.dto.MemberRegisterRequest;
import com.example.scsaattend.dto.MemberResponse;
import com.example.scsaattend.dto.MemberUpdateRequest;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("attendance_info/search")
    Call<List<AttendanceInfoResponse>> searchAttendance(@Body SearchAttendanceRequest request);

    @GET("members")
    Call<List<com.example.scsaattend.dto.MemberResponse>> getMembers();

    @PATCH("attendance_info")
    Call<BatchUpdateResponse> batchUpdateAttendance(@Body BatchUpdateRequest request);

    @PATCH("attendance_info/{aInfoId}/arrival")
    Call<CheckInApiResponse> checkIn(@Path("aInfoId") int aInfoId, @Body Object emptyBody);

    @PATCH("attendance_info/{aInfoId}/leaving")
    Call<CheckInApiResponse> checkOut(@Path("aInfoId") int aInfoId, @Body CheckOutRequest request);

    @POST("members")
    Call<Void> registerMember(@Header("userId") Long userId, @Body com.example.scsaattend.dto.MemberRegisterRequest request);

    @DELETE("members/{memberId}")
    Call<Void> deleteMember(@Header("userId") Long userId, @Path("memberId") Long memberId);

    @PATCH("members/{memberId}")
    Call<Void> updateMember(@Header("userId") Long userId, @Path("memberId") Long memberId, @Body com.example.scsaattend.dto.MemberUpdateRequest request);

    @GET("attendance_types")
    Call<List<AttendanceTypeResponse>> getAttendanceTypes();

    @POST("attendance_types")
    Call<AttendanceTypeResponse> addAttendanceType(@Body AttendanceTypeRequest request);

    @DELETE("attendance_types/{typeId}")
    Call<ResponseBody> deleteAttendanceType(@Path("typeId") long typeId);
}