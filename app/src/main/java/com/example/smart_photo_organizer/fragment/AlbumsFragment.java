package com.example.smart_photo_organizer.fragment;

import android.net.Uri;
import android.os.Bundle;
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
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderItemAdapter adapter;

    private final List<FolderItem> folderList = new ArrayList<>();
    private final Map<String, FolderItem> folderMap = new HashMap<>();

    private boolean loading = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_albums, container, false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewFolders);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new FolderItemAdapter(
                requireContext(),
                folderList,
                folderItem -> {

                    ImageGridFragment fragment = new ImageGridFragment();
                    Bundle args = new Bundle();
                    args.putParcelableArrayList(
                            "images",
                            new ArrayList<>(folderItem.getImageUris())
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

        loadImageFolders();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImageFolders();
    }

    private void loadImageFolders() {

        if (loading) return;
        loading = true;

        folderList.clear();
        folderMap.clear();
        adapter.notifyDataSetChanged();

        ImageFetcher.loadAllImagesAsync(
                requireContext(),
                50, // batch size (önemli değil, klasör için birikir)
                new ImageFetcher.ImageBatchCallback() {

                    @Override
                    public void onBatch(List<HashItem> batch) {

                        for (HashItem item : batch) {
                            String folderName = item.bucketName;

                            FolderItem folder =
                                    folderMap.get(folderName);

                            if (folder == null) {
                                ArrayList<Uri> uris =
                                        new ArrayList<>();
                                uris.add(item.uri);

                                folder = new FolderItem(
                                        folderName,
                                        item.uri,
                                        uris
                                );

                                folderMap.put(folderName, folder);
                            } else {
                                folder.getImageUris()
                                        .add(item.uri);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {

                        folderList.addAll(
                                folderMap.values()
                        );

                        adapter.notifyDataSetChanged();
                        loading = false;
                    }
                }
        );
    }
}
