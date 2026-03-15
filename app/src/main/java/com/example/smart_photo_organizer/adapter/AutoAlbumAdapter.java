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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.activity.AutoAlbumDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class AutoAlbumAdapter extends RecyclerView.Adapter<AutoAlbumAdapter.ViewHolder> {

    private Context context;
    private List<AutoAlbum> albumList;

    public AutoAlbumAdapter(Context context, List<AutoAlbum> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Grid görünümü için item_auto_album kullanıyoruz
        View view = LayoutInflater.from(context).inflate(R.layout.item_auto_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutoAlbum album = albumList.get(position);

        holder.tvTitle.setText(album.title);
        holder.tvCount.setText(album.photos.size() + " Fotoğraf");

        if (!album.photos.isEmpty()) {
            Glide.with(context)
                    .load(album.photos.get(0).uri)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .centerCrop()
                    .into(holder.ivCover);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AutoAlbumDetailActivity.class);
            intent.putExtra("ALBUM_TITLE", album.title);
            intent.putParcelableArrayListExtra("ALBUM_PHOTOS", new ArrayList<>(album.photos));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvCount = itemView.findViewById(R.id.tvPhotoCount);
        }
    }
}
