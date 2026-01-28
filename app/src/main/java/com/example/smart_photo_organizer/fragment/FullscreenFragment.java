package com.example.smart_photo_organizer.fragment;

import android.net.Uri;
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

    private ArrayList<Uri> images;
    private int startPosition;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_fullscreen, container, false);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        // Verileri alırken güvenlik kontrolü
        if (getArguments() != null) {
            images = getArguments().getParcelableArrayList("images");
            startPosition = getArguments().getInt("position", 0);
        }

        if (images == null || images.isEmpty()) {
            return view;
        }

        // Adaptörü kur ve tıklanan fotoğraftan başlat
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

    // Alt menü gizleme - null kontrolü eklendi (Çökmeyi engeller)
    private void hideBottomBar() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottomNavigationView);
            if (nav != null) {
                nav.setVisibility(View.GONE);
            }
        }
    }

    // Alt menü gösterme - null kontrolü eklendi
    private void showBottomBar() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottomNavigationView);
            if (nav != null) {
                nav.setVisibility(View.VISIBLE);
            }
        }
    }
}
