package com.example.smart_photo_organizer.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;

import java.util.ArrayList;

public class ImageGridFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_image_grid, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        ArrayList<String> images = getArguments() != null ? getArguments().getStringArrayList("images") : new ArrayList<>();
        recyclerView.setAdapter(new ImageGridAdapter(requireContext(), images, this));


        return view;
    }
}

