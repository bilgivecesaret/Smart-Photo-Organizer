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

public class DuplicateGridAdapter extends RecyclerView.Adapter<DuplicateGridAdapter.ViewHolder> {

    private final List<Uri> images;
    private final List<Uri> selectedImages = new ArrayList<>();
    private SelectionListener selectionListener;

    public DuplicateGridAdapter(List<Uri> images, SelectionListener selectionListener) {
        this.images = images != null ? images : new ArrayList<>();
        this.selectionListener = selectionListener;
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
        boolean isSelected = selectedImages.contains(uri);

        Glide.with(holder.imageView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.imageView);

        holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imgCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imageView.setScaleX(isSelected ? 0.95f : 1f);
        holder.imageView.setScaleY(isSelected ? 0.95f : 1f);

        holder.itemView.setOnClickListener(v -> {
            boolean selected = selectedImages.contains(uri);

            if (selectedImages.isEmpty()) {
                selectedImages.add(uri);
            } else {
                if (selected) selectedImages.remove(uri);
                else selectedImages.add(uri);
            }

            notifyItemChanged(holder.getAdapterPosition());

            if (selectionListener != null)
                selectionListener.onSelectionChanged(selectedImages.size());
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectedImages.contains(uri)) {
                selectedImages.add(uri);
                notifyItemChanged(holder.getAdapterPosition());
            }

            if (selectionListener != null)
                selectionListener.onSelectionChanged(selectedImages.size());

            return true;
        });
    }


    @Override
    public int getItemCount() {
        return images.size();
    }

    public void selectAll(boolean select) {
        selectedImages.clear();
        if (select) selectedImages.addAll(images);
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(selectedImages.size());
    }

    public void clearSelection() {
        selectedImages.clear();
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(0);
    }

    public void removeSelectedImages() {
        if (selectedImages.isEmpty()) return;
        List<Uri> toRemove = new ArrayList<>(selectedImages);
        images.removeAll(toRemove);
        selectedImages.clear();
        notifyDataSetChanged();
        if (selectionListener != null)
            selectionListener.onSelectionChanged(0);
    }

    public List<Uri> getSelectedImages() {
        return new ArrayList<>(selectedImages);
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
