package com.example.scsaattend.network;

import com.example.scsaattend.network.dto.LoginRequest;
import com.example.scsaattend.network.dto.LoginResponse;
import com.example.scsaattend.network.dto.MemberRegisterRequest;
import com.example.scsaattend.network.dto.MemberResponse;
import com.example.scsaattend.network.dto.MemberUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    // URL 경로 수정: BaseUrl이 '/'로 끝나게 설정할 것이므로 여기서는 '/'를 뺍니다.
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("members")
    Call<List<MemberResponse>> getMembers(@Header("userId") Long userId);

    @POST("members")
    Call<Void> registerMember(@Header("userId") Long userId, @Body MemberRegisterRequest request);

    @DELETE("members/{memberId}")
    Call<Void> deleteMember(@Header("userId") Long userId, @Path("memberId") Long memberId);

    @PATCH("members/{memberId}")
    Call<Void> updateMember(@Header("userId") Long userId, @Path("memberId") Long memberId, @Body MemberUpdateRequest request);
}