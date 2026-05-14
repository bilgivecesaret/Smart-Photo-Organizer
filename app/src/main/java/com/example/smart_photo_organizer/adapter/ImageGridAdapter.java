package com.example.smart_photo_organizer.adapter;
import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.AutoAlbumDetailActivity;
import com.example.smart_photo_organizer.fragment.FullscreenFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private final Context context;
    private final List<Uri> images;
    private final FragmentManager fragmentManager;

    // Seçim için gerekli değişkenler
    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public ImageGridAdapter(Context context, List<Uri> images, FragmentManager fragmentManager) {
        this.context = context;
        this.images = images;
        this.fragmentManager = fragmentManager;
    }

    public void setSelectionListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.grid_image_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = images.get(position);
        boolean isSelected = selectedPositions.contains(position);

        Glide.with(context)
                .load(imageUri)
                .override(300, 300)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.img);

        // Seçim görsellerini güncelle
        holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.imgSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(position);
            } else {
                openFullscreen(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                toggleSelection(position);
                return true;
            }
            return false;
        });
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
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    // ImageGridAdapter.java içine ekle
    public void removeSelectedImages() {
        List<Integer> positions = new ArrayList<>(selectedPositions);
        // Pozisyonları büyükten küçüğe sıralıyoruz ki silerken index kaymasın
        Collections.sort(positions, Collections.reverseOrder());

        for (int pos : positions) {
            images.remove(pos);
            notifyItemRemoved(pos);
        }
        clearSelection();
    }

    public void selectAll(boolean select) {
        selectedPositions.clear();
        if (select) {
            for (int i = 0; i < images.size(); i++) {
                selectedPositions.add(i);
            }
        } else {
            selectionMode = false;
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelection() {
        selectedPositions.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    public List<Uri> getSelectedUris() {
        List<Uri> selectedUris = new ArrayList<>();
        for (int pos : selectedPositions) {
            selectedUris.add(images.get(pos));
        }
        return selectedUris;
    }

    private void openFullscreen(int position) {
        if (context instanceof AutoAlbumDetailActivity) {
            ((AutoAlbumDetailActivity) context).showFullscreenContainer();
        }

        FullscreenFragment fullscreenFragment = new FullscreenFragment();
        Bundle b = new Bundle();
        b.putParcelableArrayList("images", new ArrayList<>(images));
        b.putInt("position", position);
        fullscreenFragment.setArguments(b);

        fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fullscreenFragment)
                .addToBackStack("fullscreen")
                .commit();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, imgSelected;
        View selectionOverlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.grid_image_item);
            imgSelected = itemView.findViewById(R.id.imgSelected);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
        }
    }

    @Override
    public int getItemCount() { return images.size(); }
}