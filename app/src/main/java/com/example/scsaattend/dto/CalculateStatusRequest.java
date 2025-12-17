package com.example.scsaattend.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CalculateStatusRequest {

    @SerializedName("aInfoIdList")
    private List<Long> aInfoIdList;

    public CalculateStatusRequest(List<Long> aInfoIdList) {
        this.aInfoIdList = aInfoIdList;
    }
}
