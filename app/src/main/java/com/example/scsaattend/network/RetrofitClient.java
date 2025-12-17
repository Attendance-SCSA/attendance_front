package com.example.scsaattend.network;

import static com.example.scsaattend.common.Config.BASE_URL;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static Context appContext;

    // Application Context를 받기 위한 초기화 메서드
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    private static OkHttpClient getOkHttpClient() {
        if (appContext == null) {
            throw new IllegalStateException("RetrofitClient not initialized. Call initialize(context) in Application class.");
        }

        return new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Request originalRequest = chain.request();

                        // 로그인 요청(/auth/login)에는 헤더를 추가하지 않음
                        if (originalRequest.url().encodedPath().contains("auth/login")) {
                            return chain.proceed(originalRequest);
                        }

                        // 그 외 요청에는 userId 헤더 추가
                        SharedPreferences prefs = appContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                        // LoginActivity에서 저장한 PK값(mem_pk)을 가져옴 (int형)
                        int userPk = prefs.getInt("mem_pk", -1);

                        Request.Builder builder = originalRequest.newBuilder();
                        if (userPk != -1) {
                            builder.header("userId", String.valueOf(userPk));
                        }

                        Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    }
                })
                .build();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient()) // OkHttpClient 적용
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}