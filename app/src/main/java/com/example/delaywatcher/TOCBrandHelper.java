package com.example.delaywatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TOCBrandHelper {
    private static final List<Toc> allTocs = new ArrayList<>();
    private static JSONObject userColoursJson;

    public static void init(Context context) {
        try {
            InputStream is = context.getAssets().open("tocs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONArray array = new JSONArray(json);
            allTocs.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Toc t = new Toc();
                t.code = obj.getString("code");
                t.name = obj.getString("name");
                t.colour = obj.getString("colour");
                allTocs.add(t);
            }
            Collections.sort(allTocs, (t1, t2) -> t1.name.compareToIgnoreCase(t2.name));

        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String jsonString = prefs.getString("TOC_COLORS_USER", "{}");
        try {
            userColoursJson = new JSONObject(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Toc> getAllTocs() {
        return allTocs;
    }

    public static String getNameForCode(String code) {
        if (code == null) return "Unknown";
        for (Toc t : allTocs) {
            if (t.code.equalsIgnoreCase(code)) return t.name;
        }
        return code;
    }

    public static int getColorForToc(String tocName) {
        try {
            if (userColoursJson != null && userColoursJson.has(tocName)) {
                return Color.parseColor(userColoursJson.getString(tocName));
            }
            for (Toc t : allTocs) {
                if (t.name.equalsIgnoreCase(tocName)) {
                    return Color.parseColor(t.colour);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Color.parseColor("#333333");
    }
}