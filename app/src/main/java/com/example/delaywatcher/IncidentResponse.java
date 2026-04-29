package com.example.delaywatcher;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class IncidentResponse {
    @SerializedName("summary")
    public String summary;
    @SerializedName("description")
    public String description;

    @SerializedName("status")
    public String status;

    @SerializedName("affectedOperators")
    public List<AffectedOperator> affectedOperators;

    public static class AffectedOperator {
        @SerializedName("tocCode")
        public String tocCode;
    }
}