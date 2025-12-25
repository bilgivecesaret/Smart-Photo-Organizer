package com.example.smart_photo_organizer.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smart_photo_organizer.R;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGridAdapter
        extends RecyclerView.Adapter<DuplicateGridAdapter.ViewHolder> {

    private final List<Uri> images;
    private final List<Uri> selectedImages = new ArrayList<>();
    private SelectionListener selectionListener;

    public DuplicateGridAdapter(List<Uri> images) {
        this.images = images;
    }

    public interface SelectionListener {
        void onSelectionChanged(int count);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
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

        Glide.with(holder.imageView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.imageView);

        boolean isSelected = selectedImages.contains(uri);

        holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imgCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imageView.setScaleX(isSelected ? 0.95f : 1f);
        holder.imageView.setScaleY(isSelected ? 0.95f : 1f);

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelected) {
                selectedImages.add(uri);
                notifyItemChanged(position);
            }
            if (selectionListener != null) selectionListener.onSelectionChanged(selectedImages.size());
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (selectedImages.size() > 0) { // seçim modunda tek tık ile ekle/çıkar
                if (isSelected) selectedImages.remove(uri);
                else selectedImages.add(uri);
                notifyItemChanged(position);
                if (selectionListener != null) selectionListener.onSelectionChanged(selectedImages.size());
            }
        });
    }


    private void toggleSelection(Uri uri) {
        if (selectedImages.contains(uri)) {
            selectedImages.remove(uri);
        } else {
            selectedImages.add(uri);
        }
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(selectedImages.size());
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    // 🔹 Select All
    public void selectAll(boolean select) {
        selectedImages.clear();
        if (select) selectedImages.addAll(images);
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(selectedImages.size());
    }

    // 🔹 Clear selection
    public void clearSelection() {
        selectedImages.clear();
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(0);
    }

    // 🔹 Delete seçilenler
    public void removeSelectedImages() {
        for (Uri uri : new ArrayList<>(selectedImages)) {
            images.remove(uri);
            selectedImages.remove(uri);
        }
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(selectedImages.size());
    }

    public List<Uri> getSelectedImages() {
        return selectedImages;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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
