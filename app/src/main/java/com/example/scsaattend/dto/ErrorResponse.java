package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}
