package com.traveling.app.travelpath.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.traveling.app.R;
import com.traveling.app.travelpath.models.TripStep;

import java.util.List;

public class TripStepAdapter extends RecyclerView.Adapter<TripStepAdapter.ViewHolder> {

    private List<TripStep> steps;

    public TripStepAdapter(List<TripStep> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TripStep step = steps.get(position);
        holder.tvTimeSlot.setText(step.getTimeSlot());
        holder.tvTitle.setText(step.getTitle());
        holder.tvDescription.setText(step.getDescription());

        // Emoji-enriched detail text
        String typeEmoji = getTypeEmoji(step.getActivityType());
        holder.tvStepDetails.setText("💰 " + step.getBudget() + "€ • " + typeEmoji + " " + step.getActivityType());

        if (step.getTransportInfo() != null && !step.getTransportInfo().isEmpty()) {
            holder.tvTransportInfo.setVisibility(View.VISIBLE);
            holder.tvTransportInfo.setText(step.getTransportInfo());
        } else {
            holder.tvTransportInfo.setVisibility(View.GONE);
        }

        // Color-coded background based on activity type
        if (step.getActivityType() != null) {
            String type = step.getActivityType().toLowerCase();
            if (type.contains("restaurant") || type.contains("cafe")) {
                holder.ivStepImage.setBackgroundColor(0xFFFFCC80); // Orange
            } else if (type.contains("park") || type.contains("garden") || type.contains("nature")) {
                holder.ivStepImage.setBackgroundColor(0xFFAED581); // Green
            } else if (type.contains("museum") || type.contains("theatre") || type.contains("culture")) {
                holder.ivStepImage.setBackgroundColor(0xFFCE93D8); // Purple
            } else if (type.contains("attraction") || type.contains("viewpoint")) {
                holder.ivStepImage.setBackgroundColor(0xFF90CAF9); // Blue
            } else if (type.contains("shop") || type.contains("mall")) {
                holder.ivStepImage.setBackgroundColor(0xFFF48FB1); // Pink
            } else if (type.contains("historic")) {
                holder.ivStepImage.setBackgroundColor(0xFFFFAB91); // Deep orange
            } else {
                holder.ivStepImage.setBackgroundColor(0xFF80DEEA); // Cyan
            }
        }
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "📍";
        String lower = type.toLowerCase();
        if (lower.contains("restaurant") || lower.contains("cafe")) return "🍽️";
        if (lower.contains("museum") || lower.contains("theatre")) return "🏛️";
        if (lower.contains("park") || lower.contains("garden")) return "🌿";
        if (lower.contains("attraction") || lower.contains("viewpoint")) return "🎯";
        if (lower.contains("shop") || lower.contains("mall")) return "🛍️";
        if (lower.contains("historic")) return "🏰";
        if (lower.contains("playground")) return "🎪";
        return "📍";
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot, tvTitle, tvDescription, tvStepDetails, tvTransportInfo;
        ImageView ivStepImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStepDetails = itemView.findViewById(R.id.tvStepDetails);
            tvTransportInfo = itemView.findViewById(R.id.tvTransportInfo);
            ivStepImage = itemView.findViewById(R.id.ivStepImage);
        }
    }
}
