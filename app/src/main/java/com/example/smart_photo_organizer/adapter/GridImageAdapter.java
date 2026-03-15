package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.HashItem;

import java.util.List;

public class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.ViewHolder> {

    private Context context;
    private List<HashItem> photos;

    public GridImageAdapter(Context context, List<HashItem> photos) {
        this.context = context;
        this.photos = photos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.grid_image_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        HashItem item = photos.get(position);
        Uri uri = item.uri;

        Glide.with(context)
                .load(uri)
                .centerCrop()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.grid_image_item);
        }
    }
}
