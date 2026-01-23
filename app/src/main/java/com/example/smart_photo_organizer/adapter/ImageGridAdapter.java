package com.example.smart_photo_organizer.adapter;

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
import com.example.smart_photo_organizer.fragment.FullscreenFragment;

import java.util.ArrayList;
import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ViewHolder> {

    private final Context context;
    private final List<Uri> images;
    private final Fragment fragment;

    public ImageGridAdapter(Context context, List<Uri> images, Fragment fragment) {
        this.context = context;
        this.images = images;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.grid_image_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(holder.img)
                .load(images.get(position))
                .override(300, 300)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontAnimate()
                .into(holder.img);



        holder.img.setOnClickListener(v -> {
            FullscreenFragment fullscreenFragment = new FullscreenFragment();

            Bundle b = new Bundle();
            b.putParcelableArrayList("images", new ArrayList<>(images));
            b.putInt("position", position);
            fullscreenFragment.setArguments(b);

            fragment.getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fullscreenFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.grid_image_item);
        }
    }

    @Override
    public int getItemCount() { return images.size(); }
}
