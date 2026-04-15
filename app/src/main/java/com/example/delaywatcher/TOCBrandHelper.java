package com.example.delaywatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class TOCBrandHelper {
    private static JSONObject userColoursJson;
    private static final Map<String, String> officialColours = new HashMap<>();

    static {
        officialColours.put("Thameslink", "#FF5AA5");
        officialColours.put("Southern", "#8CC63F");
        officialColours.put("Great Northern", "#0099FF");
        officialColours.put("Gatwick Express", "#EB1C24");
        officialColours.put("South Western Railway", "#243D8F");
        officialColours.put("Southeastern", "#00AFEF");
        officialColours.put("Great Western Railway", "#0A493E");
        officialColours.put("Avanti West Coast", "#004354");
        officialColours.put("LNER", "#CE0F20");
        officialColours.put("CrossCountry", "#660F21");
        officialColours.put("East Midlands Railway", "#4C2F48");
        officialColours.put("Chiltern Railways", "#00A8E0");
        officialColours.put("Greater Anglia", "#D70428");
        officialColours.put("Northern", "#0F0A5C");
        officialColours.put("TransPennine Express", "#05B2ED");
        officialColours.put("ScotRail", "#002664"); 
        officialColours.put("Transport for Wales", "#FF0000");
        officialColours.put("Merseyrail", "#FFF200"); 
        officialColours.put("c2c", "#B21082"); 
        officialColours.put("London Overground", "#E86A10");
        officialColours.put("Elizabeth line", "#6950A1"); 
        officialColours.put("Heathrow Express", "#532E63");
    }

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String jsonString = prefs.getString("TOC_COLORS_USER", "{}");
        try {
            userColoursJson = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static int getColorForToc(String tocName) {
        try {
            if (userColoursJson != null && userColoursJson.has(tocName)) {
                return Color.parseColor(userColoursJson.getString(tocName));
            }
            if (officialColours.containsKey(tocName)) {
                return Color.parseColor(officialColours.get(tocName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Color.parseColor("#333333");
    }

    public static String getNameForCode(String code) {
        if (code == null) return "Unknown";
        switch (code.toUpperCase()) {
            case "TL": return "Thameslink";
            case "SN": return "Southern";
            case "GN": return "Great Northern";
            case "GX": return "Gatwick Express";
            case "SW": return "South Western Railway";
            case "SE": return "Southeastern";
            case "GW": return "Great Western Railway";
            case "VT": return "Avanti West Coast";
            case "GR": return "LNER";
            case "XC": return "CrossCountry";
            case "EM": return "East Midlands Railway";
            case "CH": return "Chiltern Railways";
            case "LE": return "Greater Anglia";
            case "NT": return "Northern";
            case "TP": return "TransPennine Express";
            case "SR": return "ScotRail";
            case "AW": return "Transport for Wales";
            case "ME": return "Merseyrail";
            case "CC": return "c2c";
            case "LO": return "London Overground";
            case "XR": return "Elizabeth line";
            case "HX": return "Heathrow Express";
            case "CS": return "Caledonian Sleeper";
            case "LN": return "Grand Central";
            default:
                return code;
        }
    }
}