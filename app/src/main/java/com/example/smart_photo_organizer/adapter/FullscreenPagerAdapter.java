package com.example.smart_photo_organizer.adapter;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.smart_photo_organizer.fragment.FullscreenImageFragment;

import java.util.List;

public class FullscreenPagerAdapter extends FragmentStateAdapter {

    private final List<Uri> images;

    public FullscreenPagerAdapter(
            @NonNull Fragment fragment,
            List<Uri> images) {
        super(fragment);
        this.images = images;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FullscreenImageFragment fragment =
                new FullscreenImageFragment();

        Bundle b = new Bundle();
        b.putParcelable("imageUri", images.get(position));
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}

