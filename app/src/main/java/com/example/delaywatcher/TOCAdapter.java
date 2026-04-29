package com.example.delaywatcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TOCAdapter extends RecyclerView.Adapter<TOCAdapter.TocViewHolder> {

    private List<DisruptionResponce.ServiceIndicator> tocList;
    private Context context;

    public TOCAdapter(List<DisruptionResponce.ServiceIndicator> tocList, Context context) {
        this.tocList = tocList;
        this.context = context;
    }

    public void updateData(List<DisruptionResponce.ServiceIndicator> newTocs) {
        this.tocList = newTocs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_toc_status, parent, false);
        return new TocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TocViewHolder holder, int position) {
        DisruptionResponce.ServiceIndicator currentToc = tocList.get(position);

        holder.tocName.setText(currentToc.tocName);
        String actualStatus = currentToc.getBestStatus();
        holder.tocStatusText.setText(actualStatus);

        int brandColor = TOCBrandHelper.getColorForToc(currentToc.tocName);
        holder.tocColorBar.setBackgroundColor(brandColor);

        holder.itemView.setOnClickListener(v -> {
            Intent detailIntent = new Intent(context, TOCDetailActivity.class);
            detailIntent.putExtra("TOC_NAME", currentToc.tocName);
            detailIntent.putExtra("TOC_CODE", currentToc.tocCode);
            detailIntent.putExtra("TOC_STATUS", actualStatus);
            detailIntent.putExtra("TOC_DESCRIPTION", currentToc.description);
            context.startActivity(detailIntent);
        });
    }

    @Override
    public int getItemCount() {
        return tocList != null ? tocList.size() : 0;
    }
    public static class TocViewHolder extends RecyclerView.ViewHolder {
        View tocColorBar;
        TextView tocName;
        TextView tocStatusText;
        public TocViewHolder(@NonNull View itemView) {
            super(itemView);
            tocColorBar = itemView.findViewById(R.id.tocColorBar);
            tocName = itemView.findViewById(R.id.tocName);
            tocStatusText = itemView.findViewById(R.id.tocStatusText);
        }
    }
}