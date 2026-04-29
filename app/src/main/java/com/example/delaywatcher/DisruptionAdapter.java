package com.example.delaywatcher;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DisruptionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    private List<DisruptionResponce.ServiceIndicator> itemList;
    private Context context;

    public DisruptionAdapter(Context context, List<DisruptionResponce.ServiceIndicator> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        if ("SEPARATOR".equals(itemList.get(position).tocCode)) {
            return TYPE_SEPARATOR;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SEPARATOR) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_separator, parent, false);
            return new SeparatorViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_disruption, parent, false);
            return new DisruptionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisruptionResponce.ServiceIndicator item = itemList.get(position);

        if (holder instanceof DisruptionViewHolder) {
            DisruptionViewHolder dHolder = (DisruptionViewHolder) holder;
            dHolder.tvTocName.setText(item.tocName);
            dHolder.tvTocCode.setText(item.tocCode);
            dHolder.tvStatus.setText(item.status);

            int brandColor = TOCBrandHelper.getColorForToc(item.tocName);
            dHolder.tvTocCode.setBackgroundTintList(ColorStateList.valueOf(brandColor));

            dHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, TOCDetailActivity.class);
                intent.putExtra("TOC_NAME", item.tocName);
                intent.putExtra("TOC_CODE", item.tocCode);
                intent.putExtra("TOC_STATUS", item.status);
                intent.putExtra("TOC_DESCRIPTION", item.description);
                intent.putExtra("TOC_PLANNED", item.plannedDescription);
                context.startActivity(intent);
            });
        } else if (holder instanceof SeparatorViewHolder) {
            SeparatorViewHolder sHolder = (SeparatorViewHolder) holder;
            sHolder.tvSeparatorTitle.setText(item.tocName);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateData(List<DisruptionResponce.ServiceIndicator> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    public static class DisruptionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTocName, tvTocCode, tvStatus;
        public DisruptionViewHolder(View v) {
            super(v);
            tvTocName = v.findViewById(R.id.tvTocName);
            tvTocCode = v.findViewById(R.id.tvTocCode);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeparatorTitle;
        public SeparatorViewHolder(View v) {
            super(v);
            tvSeparatorTitle = v.findViewById(R.id.tvSeparatorTitle);
        }
    }
}