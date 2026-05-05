package com.example.delaywatcher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;

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

public class AppWidgetProvider extends android.appwidget.AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        TOCBrandHelper.init(context);
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedTocs = prefs.getString("WIDGET_TRACKED_TOCS", "");
        List<String> tocFilter = new ArrayList<>();
        if (!selectedTocs.trim().isEmpty()) {
            for (String s : selectedTocs.split(",")) {
                if (!s.trim().isEmpty()) tocFilter.add(s.trim());
            }
        }
        String cachedJson = prefs.getString("CACHED_TOC_DATA", null);
        List<DisruptionResponce.ServiceIndicator> filteredData = new ArrayList<>();

        if (cachedJson != null) {
            java.lang.reflect.Type type = new TypeToken<ArrayList<DisruptionResponce.ServiceIndicator>>(){}.getType();
            List<DisruptionResponce.ServiceIndicator> allCached = new Gson().fromJson(cachedJson, type);

            for (DisruptionResponce.ServiceIndicator si : allCached) {
                if (tocFilter.isEmpty() || tocFilter.contains(si.tocCode)) {
                    filteredData.add(si);
                }
            }
        }
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, filteredData);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager manager, int appWidgetId, List<DisruptionResponce.ServiceIndicator> data) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_disruption);

        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        views.setTextViewText(R.id.widgetTime, timeSdf.format(new Date()));

        renderWidgetVisuals(views, data);
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent piApp = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.titleContainer, piApp);

        Intent refreshIntent = new Intent(context, AppWidgetProvider.class);
        refreshIntent.setAction("com.example.delaywatcher.ACTION_REFRESH_WIDGET");
        refreshIntent.putExtra("IS_GOOD_SERVICE", data.isEmpty());

        PendingIntent piRefresh = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetRefresh, piRefresh);

        manager.updateAppWidget(appWidgetId, views);
    }

    private void renderWidgetVisuals(RemoteViews views, List<DisruptionResponce.ServiceIndicator> disruptedTocs) {
        views.setViewVisibility(R.id.widgetRefresh, View.VISIBLE);
        views.setViewVisibility(R.id.widgetProgressDark, View.GONE);
        views.setViewVisibility(R.id.widgetProgressLight, View.GONE);

        if (disruptedTocs.isEmpty()) {
            views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_widget_blue);
            views.setTextViewText(R.id.titleLine1, "Good service");
            views.setTextViewText(R.id.titleLine2, "on selected lines");
            views.setTextColor(R.id.titleLine1, Color.WHITE);
            views.setTextColor(R.id.titleLine2, Color.WHITE);
            views.setTextColor(R.id.widgetTime, Color.WHITE);
            views.setInt(R.id.widgetRefresh, "setColorFilter", Color.WHITE);
            views.setInt(R.id.widgetLogo, "setColorFilter", Color.WHITE);

            views.setViewVisibility(R.id.layoutPills, View.GONE);
            views.setViewVisibility(R.id.layoutBars, View.GONE);
            views.setViewVisibility(R.id.iconAlert, View.GONE);
        } else {
            views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_widget_rounded);
            views.setTextViewText(R.id.titleLine1, "Status");
            views.setTextViewText(R.id.titleLine2, "Disruption");

            int dark = Color.parseColor("#111111");
            views.setTextColor(R.id.titleLine1, dark);
            views.setTextColor(R.id.titleLine2, dark);
            views.setTextColor(R.id.widgetTime, dark);
            views.setInt(R.id.widgetRefresh, "setColorFilter", dark);
            views.setInt(R.id.widgetLogo, "setColorFilter", dark);

            views.setViewVisibility(R.id.iconAlert, View.VISIBLE);

            if (disruptedTocs.size() <= 2) {
                views.setViewVisibility(R.id.layoutPills, View.VISIBLE);
                views.setViewVisibility(R.id.layoutBars, View.GONE);
                updatePill(views, R.id.badge1_container, R.id.badge1_bg, R.id.badge1_text, disruptedTocs.get(0));

                if (disruptedTocs.size() == 2) {
                    updatePill(views, R.id.badge2_container, R.id.badge2_bg, R.id.badge2_text, disruptedTocs.get(1));
                } else {
                    views.setViewVisibility(R.id.badge2_container, View.GONE);
                }
            } else {
                views.setViewVisibility(R.id.layoutPills, View.GONE);
                views.setViewVisibility(R.id.layoutBars, View.VISIBLE);

                int[] barIds = {R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5};
                for (int id : barIds) views.setViewVisibility(id, View.GONE);

                int maxBars = Math.min(disruptedTocs.size(), 5);
                for (int i = 0; i < maxBars; i++) {
                    int color = TOCBrandHelper.getColorForToc(disruptedTocs.get(i).tocName);
                    views.setInt(barIds[i], "setColorFilter", color);
                    views.setViewVisibility(barIds[i], View.VISIBLE);
                }
            }
        }
    }

    private void updatePill(RemoteViews views, int containerId, int bgId, int textId, DisruptionResponce.ServiceIndicator toc) {
        int color = TOCBrandHelper.getColorForToc(toc.tocName);
        views.setInt(bgId, "setColorFilter", color);
        views.setTextViewText(textId, toc.tocCode);
        views.setViewVisibility(containerId, View.VISIBLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("com.example.delaywatcher.ACTION_REFRESH_WIDGET".equals(intent.getAction())) {
            TOCBrandHelper.init(context);
            boolean isGoodService = intent.getBooleanExtra("IS_GOOD_SERVICE", false);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, AppWidgetProvider.class);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_disruption);
            views.setViewVisibility(R.id.widgetRefresh, View.GONE);
            if (isGoodService) {
                views.setViewVisibility(R.id.widgetProgressLight, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widgetProgressDark, View.VISIBLE);
            }
            appWidgetManager.updateAppWidget(thisWidget, views);
            final PendingResult pendingResult = goAsync();
            new Thread(() -> {
                performBackgroundSync(context);
                pendingResult.finish();
            }).start();
        }
    }

    private void performBackgroundSync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String key = prefs.getString("CUSTOMER_KEY", "");
        if (key.isEmpty()) return;
        TOCBrandHelper.init(context);
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

        NationalRailAPI api = APIClient.getClient().create(NationalRailAPI.class);

        try {
            retrofit2.Response<List<IncidentResponse>> response = api.searchIncidents(key, request).execute();

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
                            simplifiedStatus = "Planned Engineering Works";
                        } else if (currentSummary.contains("major") || currentSummary.contains("suspended") || currentSummary.contains("closed") || currentSummary.contains("blocked")) {
                            simplifiedStatus = "Major Disruption";
                        } else if (currentSummary.contains("bus") || currentSummary.contains("amended")) {
                            simplifiedStatus = "Planned Engineering Works";
                        }
                        Map<String, DisruptionResponce.ServiceIndicator> targetMap;
                        if (trackedSet.isEmpty() || trackedSet.contains(op.tocCode)) {
                            targetMap = trackedMap;
                        } else {
                            targetMap = othersMap;
                        }

                        if (!targetMap.containsKey(op.tocCode)) {
                            DisruptionResponce.ServiceIndicator si = new DisruptionResponce.ServiceIndicator();
                            si.tocCode = op.tocCode;
                            si.tocName = TOCBrandHelper.getNameForCode(op.tocCode);
                            si.status = simplifiedStatus;
                            if (simplifiedStatus.equals("Planned Engineering Works")) {
                                si.plannedDescription = incident.description;
                            } else {
                                si.description = incident.description;
                            }
                            targetMap.put(op.tocCode, si);
                        } else {
                            DisruptionResponce.ServiceIndicator existing = targetMap.get(op.tocCode);
                            if (simplifiedStatus.equals("Major Disruption")) {
                                existing.status = "Major Disruption";
                            } else if (simplifiedStatus.equals("Minor Disruption") && existing.status.equals("Planned Engineering Works")) {
                                existing.status = "Minor Disruption";
                            }
                            if (simplifiedStatus.equals("Planned Engineering Works")) {
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
                    if (si.status.equals("Planned Engineering Works")) plannedWorks.add(si);
                    else trackedLive.add(si);
                }
                for (DisruptionResponce.ServiceIndicator si : othersMap.values()) {
                    if (si.status.equals("Planned Engineering Works")) plannedWorks.add(si);
                    else othersLive.add(si);
                }
                Collections.sort(trackedLive, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));
                Collections.sort(othersLive, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));
                Collections.sort(plannedWorks, (a, b) -> a.tocName.compareToIgnoreCase(b.tocName));
                List<DisruptionResponce.ServiceIndicator> fullAppData = new ArrayList<>(trackedLive);
                if (!trackedLive.isEmpty() && !othersLive.isEmpty()) {
                    DisruptionResponce.ServiceIndicator sep = new DisruptionResponce.ServiceIndicator();
                    sep.tocCode = "SEPARATOR";
                    sep.tocName = "OTHER OPERATORS";
                    fullAppData.add(sep);
                }
                fullAppData.addAll(othersLive);
                if (!plannedWorks.isEmpty()) {
                    DisruptionResponce.ServiceIndicator sep = new DisruptionResponce.ServiceIndicator();
                    sep.tocCode = "SEPARATOR";
                    sep.tocName = "ENGINEERING WORKS";
                    fullAppData.add(sep);
                    fullAppData.addAll(plannedWorks);
                }
                String widgetJson = new com.google.gson.Gson().toJson(trackedLive);
                String appJson = new com.google.gson.Gson().toJson(fullAppData);

                prefs.edit()
                        .putString("CACHED_TOC_DATA", widgetJson)
                        .putString("CACHED_TOC_DATA_ALL", appJson)
                        .putLong("LAST_SYNC_TIME", System.currentTimeMillis())
                        .apply();
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, AppWidgetProvider.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

                Intent updateIntent = new Intent(context, AppWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}