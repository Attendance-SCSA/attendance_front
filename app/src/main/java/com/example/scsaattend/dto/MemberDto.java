package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

public class MemberDto {
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
