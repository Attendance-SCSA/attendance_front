package com.example.scsaattend.network;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.scsaattend.MyApplication;
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

    public static Retrofit getClient(String baseUrl) {
        // AuthInterceptor 생성 시 Context 전달
        AuthInterceptor authInterceptor = new AuthInterceptor(MyApplication.getContext());

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build();

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // Retrofit 인스턴스는 매번 새로 생성하여 다른 baseUrl에 대응
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    static class AuthInterceptor implements Interceptor {
        private final SharedPreferences prefs;

        public AuthInterceptor(Context context) {
            prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            String token = prefs.getString("access_token", null);
            long userId = prefs.getLong("user_numeric_id", -1);

            Request.Builder newRequest = chain.request().newBuilder();

            if (token != null) {
                newRequest.addHeader("Authorization", "Bearer " + token);
            }
            if (userId != -1) {
                newRequest.addHeader("userId", String.valueOf(userId));
            }

            return chain.proceed(newRequest.build());
        }
    }
}