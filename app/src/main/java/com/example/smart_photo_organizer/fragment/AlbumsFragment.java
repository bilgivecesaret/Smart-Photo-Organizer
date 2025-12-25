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

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = getContext()
                        .getContentResolver()
                        .query(uri, projection, null,null, sortOrder);
        if (cursor != null) {
            HashMap<String, ArrayList<String>> folderMap = new HashMap<>();
            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(0);
                File file = new File(imagePath);
                String folderPath = file.getParent();

                if (!folderMap.containsKey(folderPath)) {
                    folderMap.put(folderPath, new ArrayList<>());
                }
                folderMap.get(folderPath).add(imagePath);
            }
            cursor.close();

            for (String folderPath : folderMap.keySet()) {
                ArrayList<String> images = folderMap.get(folderPath);
                folderList.add(new FolderItem(folderPath, images.get(0), images));
            }

            adapter = new FolderItemAdapter(getContext(), folderList, folderItem -> {
                ImageGridFragment imageGridFragment = new ImageGridFragment();
                Bundle args = new Bundle();
                args.putStringArrayList("images", folderItem.getImageList());
                imageGridFragment.setArguments(args);

                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, imageGridFragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        }
    }
}
