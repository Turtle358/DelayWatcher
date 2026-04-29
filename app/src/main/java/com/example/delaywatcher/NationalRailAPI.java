package com.example.delaywatcher;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface NationalRailAPI {

    @POST("1010-disruptions-experience-api-11_0/disruptions/incidents/search")
    Call<List<IncidentResponse>> searchIncidents(
            @Header("x-apikey") String customerKey,
            @Body IncidentSearchRequest requestBody
    );
}