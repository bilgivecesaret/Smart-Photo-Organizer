package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.SimilarPhotoGridActivity;
import com.example.smart_photo_organizer.model.DuplicateGroup;

import java.util.ArrayList;
import java.util.List;

public class SimilarAlbumAdapter extends RecyclerView.Adapter<SimilarAlbumAdapter.ViewHolder> {

    Context context;
    List<DuplicateGroup> groups;

    private final OnAlbumClick callback;

    public interface OnAlbumClick {
        void openGrid(Intent intent, int position);
    }

    public SimilarAlbumAdapter(Context context, List<DuplicateGroup> groups, OnAlbumClick callback) {
        this.context = context;
        this.groups = groups;
        this.callback = callback;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_duplicate_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DuplicateGroup group = groups.get(position);

        holder.count.setText(group.images.size() + " Similar");

        Glide.with(context)
                .load(group.images.get(0))
                .into(holder.cover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SimilarPhotoGridActivity.class);
            intent.putParcelableArrayListExtra(
                    "images",
                    new ArrayList<>(group.images)
            );
            callback.openGrid(intent, position);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView count;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Yeni tasarım (item_duplicate_album.xml) ile eşleşen ID'ler
            cover = itemView.findViewById(R.id.imgCover);
            count = itemView.findViewById(R.id.txtCount);
        }
    }
}
