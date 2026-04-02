package com.example.smart_photo_organizer.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.SimilarPhotoAlbumActivity;
import com.example.smart_photo_organizer.adapter.CleanUpAdapter;
import java.util.ArrayList;
import java.util.List;

public class CleanupFragment extends Fragment {

    RecyclerView recyclerView;
    CleanUpAdapter adapter;
    List<String> folderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewFolders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderList.add(getString(R.string.folder1));
        folderList.add(getString(R.string.folder2));
        folderList.add(getString(R.string.folder3));

        adapter = new CleanUpAdapter(getContext(), folderList, item -> {
            if (item.equals(folderList.get(0))) {
                Intent intent = new Intent(requireContext(), SimilarPhotoAlbumActivity.class);
                startActivity(intent);
            }
            else if (item.equals(folderList.get(1))) {
                Intent intent = new Intent(requireContext(), com.example.smart_photo_organizer.activity.BlurredPhotoActivity.class);
                startActivity(intent);
            }
            else if (item.equals(folderList.get(2))) {
                Intent intent = new Intent(requireContext(), com.example.smart_photo_organizer.activity.AutoAlbumActivity.class);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
