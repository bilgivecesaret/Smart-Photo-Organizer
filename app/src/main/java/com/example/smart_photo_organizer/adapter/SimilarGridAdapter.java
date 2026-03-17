package com.example.smart_photo_organizer.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimilarGridAdapter extends RecyclerView.Adapter<SimilarGridAdapter.ViewHolder> {

    private final List<Uri> images;
    private final Set<Integer> selectedPositions = new HashSet<>();

    private SelectionListener selectionListener;
    private OnImageClickListener clickListener;

    private boolean selectionMode;

    public SimilarGridAdapter(List<Uri> images, SelectionListener selectionListener) {
        this.images = images != null ? images : new ArrayList<>();
        this.selectionListener = selectionListener;
    }

    public interface SelectionListener {
        void onSelectionChanged(int count);
    }

    public interface OnImageClickListener {
        void onImageClick(Uri uri, int position);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_duplicate_grid, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Uri uri = images.get(position);

        boolean isSelected = selectedPositions.contains(position);

        Glide.with(holder.imageView)
                .load(uri)
                .override(300, 300)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontAnimate()
                .into(holder.imageView);

        holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imgCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.imageView.setScaleX(isSelected ? 0.95f : 1f);
        holder.imageView.setScaleY(isSelected ? 0.95f : 1f);

        holder.itemView.setOnClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            if (selectionMode) {
                toggleSelection(pos);
            } else {
                if (clickListener != null)
                    clickListener.onImageClick(images.get(pos), pos);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return true;

            selectionMode = true;
            toggleSelection(pos);

            return true;
        });

    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    private void toggleSelection(int position) {

        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }

        if (selectedPositions.isEmpty()) {
            selectionMode = false;
        }

        notifyItemChanged(position);

        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null)
            selectionListener.onSelectionChanged(selectedPositions.size());
    }

    public void selectAll(boolean select) {

        selectedPositions.clear();

        if (select) {
            for (int i = 0; i < images.size(); i++) {
                selectedPositions.add(i);
            }
            selectionMode = true;
        } else {
            selectionMode = false;
        }

        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void clearSelection() {

        selectedPositions.clear();
        selectionMode = false;

        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void removeSelectedImages() {

        if (selectedPositions.isEmpty()) return;

        List<Uri> toRemove = new ArrayList<>();

        for (Integer pos : selectedPositions) {
            if (pos < images.size())
                toRemove.add(images.get(pos));
        }

        images.removeAll(toRemove);

        selectedPositions.clear();
        selectionMode = false;

        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<Uri> getSelectedImages() {

        List<Uri> selected = new ArrayList<>();

        for (Integer pos : selectedPositions) {
            if (pos < images.size())
                selected.add(images.get(pos));
        }

        return selected;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView, imgCheck;
        View selectionOverlay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imgPhoto);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            imgCheck = itemView.findViewById(R.id.imgCheck);
        }
    }
}