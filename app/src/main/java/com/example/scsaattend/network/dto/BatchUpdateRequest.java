package com.example.scsaattend.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class BatchUpdateRequest {

    @SerializedName("aInfoIdList")
    private List<Long> aInfoIdList;

    @SerializedName("updateData")
    private Map<String, Object> updateData;

    public BatchUpdateRequest(List<Long> aInfoIdList, Map<String, Object> updateData) {
        this.aInfoIdList = aInfoIdList;
        this.updateData = updateData;
    }
}
