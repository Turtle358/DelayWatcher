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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppWidgetProvider extends android.appwidget.AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
        PendingIntent piApp = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.titleContainer, piApp);

        manager.updateAppWidget(appWidgetId, views);
    }

    private void renderWidgetVisuals(RemoteViews views, List<DisruptionResponce.ServiceIndicator> disruptedTocs) {
        if (disruptedTocs.isEmpty()) {
            views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_widget_blue);
            views.setTextViewText(R.id.titleLine1, "Good service");
            views.setTextViewText(R.id.titleLine2, "on all lines");
            views.setTextColor(R.id.titleLine1, Color.WHITE);
            views.setTextColor(R.id.titleLine2, Color.WHITE);
            views.setTextColor(R.id.widgetTime, Color.WHITE);
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
}