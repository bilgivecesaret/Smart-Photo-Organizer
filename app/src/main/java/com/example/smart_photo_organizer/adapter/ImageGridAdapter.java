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
import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ViewHolder> {

    private final Context context;
    private final List<Uri> images;
    private final FragmentManager fragmentManager;

    public ImageGridAdapter(Context context, List<Uri> images, FragmentManager fragmentManager) {
        this.context = context;
        this.images = images;
        this.fragmentManager = fragmentManager;
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

            if (context instanceof AutoAlbumDetailActivity) {
                ((AutoAlbumDetailActivity) context).showFullscreenContainer();
            }

            FullscreenFragment fullscreenFragment = new FullscreenFragment();

            Bundle b = new Bundle();
            b.putParcelableArrayList("images", new ArrayList<>(images));
            b.putInt("position", position);
            fullscreenFragment.setArguments(b);

            fragmentManager.beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.fragment_container, fullscreenFragment)
                    .addToBackStack("fullscreen")
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
