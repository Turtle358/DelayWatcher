package com.example.delaywatcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    private final String[] tocNames = {"Thameslink", "Southeastern", "Southern", "Gatwick Express", "London Overground", "Great Northern", "South Western Railway", "Avanti West Coast", "LNER", "CrossCountry",

            "East Midlands Railway", "Chiltern Railways", "Greater Anglia", "Northern", "TransPennine Express", "ScotRail", "Transport for Wales", "Merseyrail", "c2c", "London Overground",

            "Elizabeth Line", "Heathrow Express", "Caledonian Sleeper", "Grand Central"};

    private final String[] tocCodes = {"TL", "SE", "SN", "GX", "LO", "GN", "SW", "VT", "GR", "XC", "EM", "CH", "LE", "NT", "TP", "SR", "AW", "ME", "CC", "LO", "XR", "HX", "CS", "LN"};
    private boolean[] checkedItems;
    private EditText etCustomerKey;
    private Button btnSaveSettings;
    private Button btnSelectTocs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etCustomerKey = findViewById(R.id.etCustomerKey);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnSelectTocs = findViewById(R.id.btnSelectTocs);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedKey = prefs.getString("CUSTOMER_KEY", "");
        etCustomerKey.setText(savedKey);
        String savedTocs = prefs.getString("WIDGET_TRACKED_TOCS", "");
        checkedItems = new boolean[tocCodes.length];
        for (int i = 0; i < tocCodes.length; i++) {
            if (savedTocs.contains(tocCodes[i])) {
                checkedItems[i] = true;
            }
        }

        btnSelectTocs.setOnClickListener(v -> showTocDialog());

        btnSaveSettings.setOnClickListener(v -> {
            prefs.edit().putString("CUSTOMER_KEY", etCustomerKey.getText().toString().trim()).apply();
            Toast.makeText(this, "Credentials Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showTocDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select TOCs for Widget");
        builder.setMultiChoiceItems(tocNames, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });
        builder.setPositiveButton("Save", (dialog, which) -> {
            ArrayList<String> toSave = new ArrayList<>();
            for (int i = 0; i < tocCodes.length; i++) {
                if (checkedItems[i]) toSave.add(tocCodes[i]);
            }
            String joined = String.join(",", toSave);
            getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().putString("WIDGET_TRACKED_TOCS", joined).apply();
            android.appwidget.AppWidgetManager appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this);
            android.content.ComponentName thisWidget = new android.content.ComponentName(this, AppWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            android.content.Intent intent = new android.content.Intent(this, AppWidgetProvider.class);
            intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            sendBroadcast(intent);

            Toast.makeText(this, "Widget Filter Updated", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
}