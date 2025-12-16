package com.example.smart_photo_organizer.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.FullscreenPagerAdapter;

import java.util.ArrayList;

public class FullscreenFragment extends Fragment {

    private ArrayList<String> images;
    private int startPosition;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_fullscreen, container, false);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        images = getArguments() != null ? getArguments().getStringArrayList("images") : null;
        startPosition = getArguments() != null ? getArguments().getInt("position", 0) : 0;

        if (images == null || images.isEmpty()) return view;

        viewPager.setAdapter(new FullscreenPagerAdapter(this, images));
        viewPager.setCurrentItem(startPosition, false);

        hideBottomBar();
        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomBar();
    }

    private void hideBottomBar() {
        requireActivity()
                .findViewById(R.id.bottomNavigationView)
                .setVisibility(View.GONE);
    }

    private void showBottomBar() {
        requireActivity()
                .findViewById(R.id.bottomNavigationView)
                .setVisibility(View.VISIBLE);
    }
}



