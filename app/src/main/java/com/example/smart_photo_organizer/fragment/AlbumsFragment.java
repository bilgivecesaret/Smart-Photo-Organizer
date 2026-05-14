package com.example.smart_photo_organizer.fragment;

import android.app.Activity;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.smart_photo_organizer.util.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderItemAdapter adapter;
    private View topBar;
    private Button btnDelete, btnCancel;
    private CheckBox cbSelectAll;
    private final List<FolderItem> folderList = new ArrayList<>();
    private final Map<String, FolderItem> folderMap = new HashMap<>();
    private boolean loading = false;
    private long lastDeletedSize = 0;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            adapter.clearSelection();
                            Notification.showSuccessDialog(
                                    requireActivity(),
                                    Notification.formatSize(lastDeletedSize),
                                    () -> loadImageFolders()
                            );
                        }
                    });
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

        topBar      = view.findViewById(R.id.topBar);
        btnDelete   = view.findViewById(R.id.btnDelete);
        btnCancel   = view.findViewById(R.id.btnCancel);
        cbSelectAll = view.findViewById(R.id.cbSelectAll);
        recyclerView = view.findViewById(R.id.recyclerViewFolders);

        topBar.setVisibility(View.GONE);

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

        adapter.setSelectionListener(count -> {
            if (count > 0) {
                topBar.setVisibility(View.VISIBLE);
                btnDelete.setText("Delete (" + count + ")");
                btnDelete.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
            } else {
                topBar.setVisibility(View.GONE);
            }

            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(count == folderList.size());
            cbSelectAll.setOnCheckedChangeListener((btn, checked) ->
                    adapter.selectAll(checked));
        });

        btnCancel.setOnClickListener(v -> adapter.clearSelection());

        btnDelete.setOnClickListener(v -> deleteSelectedFolders());

        recyclerView.setAdapter(adapter);
        loadImageFolders();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void deleteSelectedFolders() {
        List<Uri> uris = adapter.getSelectedUris();
        if (uris.isEmpty()) return;

        // Önce kullanıcıya onay sor
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Folder Photos")
                .setMessage(uris.size() + " photos will be permanently deleted. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> performDelete(uris))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(List<Uri> uris) {
        lastDeletedSize = Notification.calculateTotalSize(requireContext(), uris);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ → sistem onay ekranı
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(), uris);
                deleteLauncher.launch(
                        new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Delete failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Android 10 ve altı → direkt sil
            for (Uri uri : uris) {
                try {
                    requireContext().getContentResolver().delete(uri, null, null);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            adapter.clearSelection();
            Notification.showSuccessDialog(
                    requireActivity(),
                    Notification.formatSize(lastDeletedSize),
                    () -> loadImageFolders()
            );
        }
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
