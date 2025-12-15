package com.example.scsaattend.network;

import com.example.scsaattend.network.dto.LoginRequest;
import com.example.scsaattend.network.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    // URL 경로 수정: BaseUrl이 '/'로 끝나게 설정할 것이므로 여기서는 '/'를 뺍니다.
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}