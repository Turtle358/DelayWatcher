package com.example.delaywatcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewTocs;
    private ImageButton btnSettings;
    private TextView liveIndicator;
    private TOCAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TOCBrandHelper.init(this);
        recyclerViewTocs = findViewById(R.id.recyclerViewTocs);
        btnSettings = findViewById(R.id.btnSettings);
        liveIndicator = findViewById(R.id.liveIndicator);

        recyclerViewTocs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TOCAdapter(new ArrayList<>(), this);
        recyclerViewTocs.setAdapter(adapter);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAuthFlow();
    }

    private void startAuthFlow() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String key = prefs.getString("CUSTOMER_KEY", "");

        if (key.isEmpty()) {
            liveIndicator.setText("◉ Offline - No API Key");
            Toast.makeText(this, "Please set your Key in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        long lastSync = prefs.getLong("LAST_SYNC_TIME", 0);
        long currentTime = System.currentTimeMillis();
        long oneHourMillis = 3600000;
        if (currentTime - lastSync < oneHourMillis && lastSync != 0) {
            String cachedJson = prefs.getString("CACHED_TOC_DATA", null);

            if (cachedJson != null) {
                try {
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ArrayList<DisruptionResponce.ServiceIndicator>>(){}.getType();
                    List<DisruptionResponce.ServiceIndicator> cachedData = new com.google.gson.Gson().fromJson(cachedJson, type);
                    adapter.updateData(cachedData);

                    TextView goodServiceSubtext = findViewById(R.id.goodServiceSubtext);
                    goodServiceSubtext.setText(cachedData.isEmpty() ? "On all lines" : "All other lines");

                    liveIndicator.setText("◉ Live (Cached)");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        NationalRailAPI api = APIClient.getClient().create(NationalRailAPI.class);
        fetchRailData(api, key);
    }

    private void fetchRailData(NationalRailAPI api, String customerKey) {
        liveIndicator.setText("◉ Syncing Real-Time Delays...");
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedTocs = prefs.getString("WIDGET_TRACKED_TOCS", "");
        List<String> tocFilter = new ArrayList<>();

        if (!selectedTocs.trim().isEmpty()) {
            for (String s : selectedTocs.split(",")) {
                if (!s.trim().isEmpty()) {
                    tocFilter.add(s.trim());
                }
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        String tomorrow = sdf.format(c.getTime());
        IncidentSearchRequest request = new IncidentSearchRequest(today, tomorrow, tocFilter);
        api.searchIncidents(customerKey, request).enqueue(new Callback<List<IncidentResponse>>() {
            @Override
            public void onResponse(Call<List<IncidentResponse>> call, Response<List<IncidentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, DisruptionResponce.ServiceIndicator> tocMap = new LinkedHashMap<>();

                    for (IncidentResponse incident : response.body()) {
                        if (incident.affectedOperators == null) continue;
                        if ("Cleared".equalsIgnoreCase(incident.status)) continue;

                        for (IncidentResponse.AffectedOperator op : incident.affectedOperators) {
                            if (!tocFilter.isEmpty() && !tocFilter.contains(op.tocCode)) continue;
                            String currentSummary = incident.summary.toLowerCase();
                            String simplifiedStatus = "Minor Disruption";
                            if (currentSummary.contains("major") || currentSummary.contains("suspended") || currentSummary.contains("closed")) {
                                simplifiedStatus = "Major Disruption";
                            } else if (currentSummary.contains("bus") || currentSummary.contains("amended") || currentSummary.contains("engineering") || currentSummary.contains("maintenance")) {
                                simplifiedStatus = "Planned Works";
                            }

                            // Map and De-duplicate
                            if (!tocMap.containsKey(op.tocCode)) {
                                DisruptionResponce.ServiceIndicator si = new DisruptionResponce.ServiceIndicator();
                                si.tocCode = op.tocCode;
                                si.tocName = TOCBrandHelper.getNameForCode(op.tocCode);
                                si.status = simplifiedStatus;
                                si.description = incident.description;
                                tocMap.put(op.tocCode, si);
                            } else {
                                DisruptionResponce.ServiceIndicator existing = tocMap.get(op.tocCode);
                                if (simplifiedStatus.equals("Major Disruption")) {
                                    existing.status = "Major Disruption";
                                }
                                existing.description += "<br><br><hr><br>" + incident.description;
                            }
                        }
                    }

                    List<DisruptionResponce.ServiceIndicator> finalData = new ArrayList<>(tocMap.values());
                    adapter.updateData(finalData);

                    TextView goodServiceSubtext = findViewById(R.id.goodServiceSubtext);
                    goodServiceSubtext.setText(finalData.isEmpty() ? "On all lines" : "All other lines");
                    liveIndicator.setText("◉ Live");

                    String json = new com.google.gson.Gson().toJson(finalData);
                    prefs.edit()
                            .putString("CACHED_TOC_DATA", json)
                            .putLong("LAST_SYNC_TIME", System.currentTimeMillis())
                            .apply();

                    android.appwidget.AppWidgetManager appWidgetManager = android.appwidget.AppWidgetManager.getInstance(getApplicationContext());
                    android.content.ComponentName thisWidget = new android.content.ComponentName(getApplicationContext(), AppWidgetProvider.class);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

                    android.content.Intent intent = new android.content.Intent(MainActivity.this, AppWidgetProvider.class);
                    intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    sendBroadcast(intent);

                } else {
                    liveIndicator.setText("◉ API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<IncidentResponse>> call, Throwable t) {
                liveIndicator.setText("◉ Data Sync Failed");
            }
        });
    }
}