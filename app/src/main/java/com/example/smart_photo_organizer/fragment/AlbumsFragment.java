package com.example.smart_photo_organizer.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.FolderItemAdapter;
import com.example.smart_photo_organizer.model.FolderItem;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.LoadingImage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AlbumsFragment extends Fragment {

    RecyclerView recyclerView;
    FolderItemAdapter adapter;
    List<FolderItem> folderList = new ArrayList<>();

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

        loadImageFolders();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImageFolders();
    }

    private void loadImageFolders() {
        folderList.clear();

        ArrayList<HashItem> allImages =
                LoadingImage.loadAllImages(requireContext());

        HashMap<String, FolderItem> folderMap = new HashMap<>();

        for (HashItem item : allImages) {

            String folderName = item.bucketName;
            if (folderName == null) folderName = "Unknown";

            FolderItem folderItem = folderMap.get(folderName);

            if (folderItem == null) {
                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(item.uri);

                folderItem = new FolderItem(
                        folderName,
                        item.uri,   // preview
                        uris
                );

                folderMap.put(folderName, folderItem);
            } else {
                folderItem.getImageUris().add(item.uri);
            }
        }

        folderList.addAll(folderMap.values());

        adapter = new FolderItemAdapter(
                requireContext(),
                folderList,
                folderItem -> {

                    ImageGridFragment fragment = new ImageGridFragment();
                    Bundle args = new Bundle();
                    args.putParcelableArrayList(
                            "images",
                            folderItem.getImageUris()
                    );
                    fragment.setArguments(args);

                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
        );

        recyclerView.setAdapter(adapter);
    }


}
