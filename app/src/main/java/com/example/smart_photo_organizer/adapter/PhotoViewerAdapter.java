package com.example.smart_photo_organizer.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class PhotoViewerAdapter
        extends RecyclerView.Adapter<PhotoViewerAdapter.ViewHolder> {

    private final List<Uri> images;

    public PhotoViewerAdapter(List<Uri> images){
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_viewer,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        Uri uri = images.get(position);

        Glide.with(holder.photoView.getContext())
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        PhotoView photoView;

        ViewHolder(View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}