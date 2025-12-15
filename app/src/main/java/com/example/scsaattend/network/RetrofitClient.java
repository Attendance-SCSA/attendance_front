package com.example.scsaattend.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            // baseUrl이 반드시 '/'로 끝나도록 보정
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            // Lenient 모드 활성화: JSON 형식이 약간 어긋나도 최대한 읽어들이도록 설정
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}