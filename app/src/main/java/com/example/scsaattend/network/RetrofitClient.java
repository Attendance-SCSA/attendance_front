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

                        if (originalRequest.url().encodedPath().contains("auth/login")) {
                            return chain.proceed(originalRequest);
                        }

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
            Gson gson = new GsonBuilder()
                    .serializeNulls() // null 값을 직렬화하도록 설정
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
