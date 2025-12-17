package com.example.scsaattend;

import android.app.Application;

import com.example.scsaattend.network.RetrofitClient;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.initialize(this);
    }
}