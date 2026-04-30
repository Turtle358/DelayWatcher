package com.example.delaywatcher;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private String[] tocNames;
    private String[] tocCodes;
    private boolean[] checkedItems;

    private EditText etCustomerKey;
    private Button btnSaveSettings;
    private Button btnSelectTocs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_settings);
        TOCBrandHelper.init(this);
        List<Toc> tocList = TOCBrandHelper.getAllTocs();
        tocNames = new String[tocList.size()];
        tocCodes = new String[tocList.size()];
        checkedItems = new boolean[tocList.size()];

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedTocs = prefs.getString("WIDGET_TRACKED_TOCS", "");

        for (int i = 0; i < tocList.size(); i++) {
            tocNames[i] = tocList.get(i).name;
            tocCodes[i] = tocList.get(i).code;
            if (savedTocs.contains(tocCodes[i])) {
                checkedItems[i] = true;
            }
        }

        etCustomerKey = findViewById(R.id.etCustomerKey);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnSelectTocs = findViewById(R.id.btnSelectTocs);

        etCustomerKey.setText(prefs.getString("CUSTOMER_KEY", ""));

        btnSelectTocs.setOnClickListener(v -> showTocDialog());

        btnSaveSettings.setOnClickListener(v -> {
            prefs.edit().putString("CUSTOMER_KEY", etCustomerKey.getText().toString().trim()).apply();
            Toast.makeText(this, "Credentials Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showTocDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select TOCs for Widget")
                .setMultiChoiceItems(tocNames, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    ArrayList<String> toSave = new ArrayList<>();
                    for (int i = 0; i < tocCodes.length; i++) {
                        if (checkedItems[i]) toSave.add(tocCodes[i]);
                    }

                    String joined = String.join(",", toSave);
                    getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("WIDGET_TRACKED_TOCS", joined)
                            .apply();

                    refreshWidget();
                    Toast.makeText(this, "Widget Filter Updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, AppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Intent intent = new Intent(this, AppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);
    }
}