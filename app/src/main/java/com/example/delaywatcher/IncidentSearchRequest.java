package com.example.delaywatcher;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class IncidentSearchRequest {
    @SerializedName("startDate")
    public String startDate;

    @SerializedName("endDate")
    public String endDate;

    @SerializedName("tocCodes")
    public List<String> tocCodes;
    public IncidentSearchRequest(String start, String end, List<String> tocCodes) {
        this.startDate = start;
        this.endDate = end;
        if (tocCodes != null) {
            this.tocCodes = tocCodes;
        } else {
            this.tocCodes = new ArrayList<>();
        }
    }
}