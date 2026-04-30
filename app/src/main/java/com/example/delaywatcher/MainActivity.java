package com.example.delaywatcher;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ImageButton btnSettings;
    private TextView liveIndicator;
    private DisruptionAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NationalRailAPI api;
    private String customerKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOCBrandHelper.init(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);

        liveIndicator = findViewById(R.id.liveIndicator);
        btnSettings = findViewById(R.id.btnSettings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewTocs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DisruptionAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        api = APIClient.getClient().create(NationalRailAPI.class);
        customerKey = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("CUSTOMER_KEY", "");

        swipeRefreshLayout.setOnRefreshListener(() -> fetchRailData(api, customerKey));

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCacheAndLoad();
    }

    private void checkCacheAndLoad() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String key = prefs.getString("CUSTOMER_KEY", "");

        if (key.isEmpty()) {
            liveIndicator.setText("◉ Offline - No API Key");
            return;
        }

        long lastSync = prefs.getLong("LAST_SYNC_TIME", 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSync < 900000 && lastSync != 0) {
            String cachedJson = prefs.getString("CACHED_TOC_DATA_ALL", null);
            if (cachedJson != null) {
                try {
                    java.lang.reflect.Type type = new TypeToken<ArrayList<DisruptionResponce.ServiceIndicator>>(){}.getType();
                    List<DisruptionResponce.ServiceIndicator> cachedData = new Gson().fromJson(cachedJson, type);
                    adapter.updateData(cachedData);
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String syncTime = timeFormat.format(new Date(lastSync));

                    liveIndicator.setText("◉ Cached (" + syncTime + ")");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        fetchRailData(api, key);
    }

    private void fetchRailData(NationalRailAPI api, String key) {
        if (key == null || key.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        liveIndicator.setText("◉ Syncing Real-Time Delays...");

        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedTocs = prefs.getString("WIDGET_TRACKED_TOCS", "");

        Set<String> trackedSet = new HashSet<>();
        if (!selectedTocs.trim().isEmpty()) {
            for (String s : selectedTocs.split(",")) {
                if (!s.trim().isEmpty()) trackedSet.add(s.trim());
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        IncidentSearchRequest request = new IncidentSearchRequest(today, today, new ArrayList<>());

        api.searchIncidents(key, request).enqueue(new Callback<List<IncidentResponse>>() {
            @Override
            public void onResponse(Call<List<IncidentResponse>> call, Response<List<IncidentResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, DisruptionResponce.ServiceIndicator> trackedMap = new LinkedHashMap<>();
                    Map<String, DisruptionResponce.ServiceIndicator> othersMap = new LinkedHashMap<>();

                    for (IncidentResponse incident : response.body()) {
                        if (incident.affectedOperators == null || "Cleared".equalsIgnoreCase(incident.status)) continue;

                        for (IncidentResponse.AffectedOperator op : incident.affectedOperators) {
                            String currentSummary = incident.summary != null ? incident.summary.toLowerCase() : "";
                            String currentDesc = incident.description != null ? incident.description.toLowerCase() : "";

                            String simplifiedStatus = "Minor Disruption";
                            boolean isEngineering = (currentSummary.contains("engineering") || currentDesc.contains("engineering"))
                                    && !currentSummary.contains("overrun") && !currentDesc.contains("overrun");

                            if (isEngineering) {
                                simplifiedStatus = "Planned Works";
                            } else if (currentSummary.contains("major") || currentSummary.contains("suspended") || currentSummary.contains("closed") || currentSummary.contains("blocked")) {
                                simplifiedStatus = "Major Disruption";
                            } else if (currentSummary.contains("bus") || currentSummary.contains("amended")) {
                                simplifiedStatus = "Planned Works";
                            }

                            Map<String, DisruptionResponce.ServiceIndicator> targetMap =
                                    trackedSet.contains(op.tocCode) ? trackedMap : othersMap;

                            if (!targetMap.containsKey(op.tocCode)) {
                                DisruptionResponce.ServiceIndicator si = new DisruptionResponce.ServiceIndicator();
                                si.tocCode = op.tocCode;
                                si.tocName = TOCBrandHelper.getNameForCode(op.tocCode);
                                si.status = simplifiedStatus;

                                if (simplifiedStatus.equals("Planned Works")) {
                                    si.plannedDescription = incident.description;
                                } else {
                                    si.description = incident.description;
                                }
                                targetMap.put(op.tocCode, si);
                            } else {
                                DisruptionResponce.ServiceIndicator existing = targetMap.get(op.tocCode);

                                if (simplifiedStatus.equals("Major Disruption")) {
                                    existing.status = "Major Disruption";
                                } else if (simplifiedStatus.equals("Minor Disruption") && existing.status.equals("Planned Works")) {
                                    existing.status = "Minor Disruption";
                                }

                                if (simplifiedStatus.equals("Planned Works")) {
                                    if (existing.plannedDescription.isEmpty()) {
                                        existing.plannedDescription = incident.description;
                                    } else {
                                        existing.plannedDescription += "<br><br><hr><br>" + incident.description;
                                    }
                                } else {
                                    if (existing.description.isEmpty()) {
                                        existing.description = incident.description;
                                    } else {
                                        existing.description += "<br><br><hr><br>" + incident.description;
                                    }
                                }
                            }
                        }
                    }

                    List<DisruptionResponce.ServiceIndicator> trackedLive = new ArrayList<>();
                    List<DisruptionResponce.ServiceIndicator> othersLive = new ArrayList<>();
                    List<DisruptionResponce.ServiceIndicator> plannedWorks = new ArrayList<>();

                    for (DisruptionResponce.ServiceIndicator si : trackedMap.values()) {
                        if (si.status.equals("Planned Works")) plannedWorks.add(si);
                        else trackedLive.add(si);
                    }

                    for (DisruptionResponce.ServiceIndicator si : othersMap.values()) {
                        if (si.status.equals("Planned Works")) plannedWorks.add(si);
                        else othersLive.add(si);
                    }

                    Collections.sort(trackedLive, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));
                    Collections.sort(othersLive, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));
                    Collections.sort(plannedWorks, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));

                    List<DisruptionResponce.ServiceIndicator> finalData = new ArrayList<>(trackedLive);

                    if (!trackedLive.isEmpty() && !othersLive.isEmpty()) {
                        DisruptionResponce.ServiceIndicator sep = new DisruptionResponce.ServiceIndicator();
                        sep.tocCode = "SEPARATOR";
                        sep.tocName = "OTHER OPERATORS";
                        finalData.add(sep);
                    }
                    finalData.addAll(othersLive);

                    if (!plannedWorks.isEmpty()) {
                        DisruptionResponce.ServiceIndicator sep = new DisruptionResponce.ServiceIndicator();
                        sep.tocCode = "SEPARATOR";
                        sep.tocName = "ENGINEERING WORKS";
                        finalData.add(sep);
                        finalData.addAll(plannedWorks);
                    }

                    adapter.updateData(finalData);

                    TextView goodServiceSubtext = findViewById(R.id.goodServiceSubtext);
                    goodServiceSubtext.setText(trackedLive.isEmpty() ? "On all lines" : "All other lines");
                    liveIndicator.setText("◉ Live");

                    String widgetJson = new Gson().toJson(new ArrayList<>(trackedLive));
                    String appJson = new Gson().toJson(finalData);

                    prefs.edit()
                            .putString("CACHED_TOC_DATA", widgetJson)
                            .putString("CACHED_TOC_DATA_ALL", appJson)
                            .putLong("LAST_SYNC_TIME", System.currentTimeMillis())
                            .apply();

                    nudgeWidget();

                } else {
                    liveIndicator.setText("◉ API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<IncidentResponse>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                liveIndicator.setText("◉ Data Sync Failed");
            }
        });
    }

    private void nudgeWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName thisWidget = new ComponentName(getApplicationContext(), AppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Intent intent = new Intent(this, AppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);
    }
}