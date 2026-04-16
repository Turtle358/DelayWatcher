package com.example.delaywatcher;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TOCDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toc_detail);
        String tocName = getIntent().getStringExtra("TOC_NAME");
        String tocCode = getIntent().getStringExtra("TOC_CODE");
        String tocStatus = getIntent().getStringExtra("TOC_STATUS");
        String tocDescription = getIntent().getStringExtra("TOC_DESCRIPTION");
        String tocPlanned = getIntent().getStringExtra("TOC_PLANNED");
        View headerColorBar = findViewById(R.id.headerColorBar);
        TextView tvName = findViewById(R.id.detailTocName);
        TextView tvCode = findViewById(R.id.detailTocCode);
        TextView tvStatus = findViewById(R.id.detailTocStatus);
        TextView headerActive = findViewById(R.id.headerActive);
        TextView tvDetailText = findViewById(R.id.detailIncidentsText);
        TextView headerPlanned = findViewById(R.id.headerPlanned);
        TextView tvPlannedText = findViewById(R.id.detailPlannedText);
        tvName.setText(tocName != null ? tocName : "Unknown Operator");
        tvCode.setText(tocCode != null ? tocCode : "--");
        tvStatus.setText(tocStatus != null ? tocStatus : "Status Unavailable");
        if (tocName != null) {
            int brandColor = TOCBrandHelper.getColorForToc(tocName);
            headerColorBar.setBackgroundColor(brandColor);
            tvCode.setBackgroundTintList(ColorStateList.valueOf(brandColor));
        }
        if (tocDescription != null && !tocDescription.trim().isEmpty()) {
            tvDetailText.setText(Html.fromHtml(tocDescription, Html.FROM_HTML_MODE_COMPACT));
            tvDetailText.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            headerActive.setVisibility(View.GONE);
            tvDetailText.setVisibility(View.GONE);
        }

        if (tocPlanned != null && !tocPlanned.trim().isEmpty()) {
            tvPlannedText.setText(Html.fromHtml(tocPlanned, Html.FROM_HTML_MODE_COMPACT));
            tvPlannedText.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            headerPlanned.setVisibility(View.GONE);
            tvPlannedText.setVisibility(View.GONE);
        }
    }
}