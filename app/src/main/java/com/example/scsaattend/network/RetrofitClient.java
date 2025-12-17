package com.example.scsaattend.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://10.10.0.125:8888";
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

                        // Don't add header to login requests
                        if (originalRequest.url().encodedPath().contains("auth/login")) {
                            return chain.proceed(originalRequest);
                        }

                        // For other requests, add userId header
                        SharedPreferences prefs = appContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                        long userId = prefs.getLong("user_numeric_id", -1);

                        Request.Builder builder = originalRequest.newBuilder();
                        if (userId != -1) {
                            builder.header("userId", String.valueOf(userId));
                        }

                        Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    }
                })
                .build();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient()) // Use OkHttpClient with interceptor
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
