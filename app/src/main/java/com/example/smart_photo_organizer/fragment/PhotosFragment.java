package com.example.smart_photo_organizer.fragment;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.PhotoViewerActivity;
import com.example.smart_photo_organizer.adapter.SimilarGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.util.Notification;

import java.util.ArrayList;
import java.util.List;

public class PhotosFragment extends Fragment {
    private SimilarGridAdapter adapter;
    private LinearLayout topBar;
    private CheckBox selectAll;
    private Button delete, cancel;
    private ProgressBar progressBar;
    private final ArrayList<Uri> imageUris = new ArrayList<>();
    private ContentObserver observer;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE = 600;
    private long lastDeletedSize = 0;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == -1) {
            Notification.showSuccessDialog(this.requireActivity(),Notification.formatSize(lastDeletedSize));
            adapter.removeSelectedImages();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.activity_grid_view, container, false
        );

        RecyclerView recyclerView = view.findViewById(R.id.recyclerGrid);
        topBar = view.findViewById(R.id.topBar);
        progressBar = view.findViewById(R.id.progressBar);
        selectAll = view.findViewById(R.id.cbSelectAll);
        delete = view.findViewById(R.id.btnDelete);
        cancel = view.findViewById(R.id.btnCancel);
        topBar.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);

        recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), 4)
        );

        adapter = new SimilarGridAdapter(imageUris, count -> updateUI(count));
        adapter.setSelectionListener(count -> updateUI(count));

        adapter.setOnImageClickListener((uri, position) -> {
            Intent intent = new Intent(requireContext(), PhotoViewerActivity.class);
            intent.putParcelableArrayListExtra("images", new ArrayList<>(imageUris));
            intent.putExtra("position", position);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        cancel.setOnClickListener(v -> {

            adapter.clearSelection();

            delete.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);

        });

        delete.setOnClickListener(v -> {

            List<Uri> selected = adapter.getSelectedImages();
            if (selected.isEmpty()) return;
            lastDeletedSize = Notification.calculateTotalSize(requireContext(), selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    PendingIntent pi =
                            MediaStore.createDeleteRequest(
                                    requireContext().getContentResolver(),
                                    selected
                            );
                    startIntentSenderForResult(
                            pi.getIntentSender(),
                            100,
                            null,
                            0,
                            0,
                            0,
                            null
                    );
                } catch (Exception e) {
                    e.printStackTrace();

                }
            } else {
                for (Uri uri : selected) {
                    requireContext()
                            .getContentResolver()
                            .delete(uri,null,null);

                }
                Notification.showSuccessDialog(this.requireActivity(), Notification.formatSize(lastDeletedSize));
                adapter.removeSelectedImages();
            }
        });

        loadImages();

        return view;
    }

    private void updateUI(int count) {

        if (count > 0) {
            topBar.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);
            delete.setText("Delete (" + count + ")");
        } else {
            topBar.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
        }

        selectAll.setOnCheckedChangeListener(null);

        selectAll.setChecked(count == adapter.getItemCount());

        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });
    }

    private void loadImages() {
        if (!isAdded()) return;
        imageUris.clear();
        if (adapter != null) {
            adapter.clearSelection();
        }

        ImageFetcher.loadAllImagesAsync(
                requireContext(),
                20,
                new ImageFetcher.ImageBatchCallback() {

                    @Override
                    public void onBatch(List<HashItem> batch) {
                        if (!isAdded()) return;
                        if (adapter != null) {
                            adapter.clearSelection();
                        }
                        int start = imageUris.size();
                        for (HashItem item : batch) {
                            imageUris.add(item.uri);
                        }
                        adapter.notifyItemRangeInserted(
                                start,
                                batch.size()
                        );
                    }

                    @Override
                    public void onComplete() {
                        if (isAdded()) {
                            progressBar.setVisibility(View.GONE);
                            showAppropriateFragment();
                        }
                    }
                }
        );
    }

    private void showAppropriateFragment() {
        if (imageUris.isEmpty()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NoImageFragment())
                    .commit();
        } else {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, this)
                    .commit();
        }
    }


    private void setupObserver() {
        if (observer != null) return;

        observer = new ContentObserver(
                new Handler(Looper.getMainLooper())
        ) {
            @Override
            public void onChange(boolean selfChange) {
                debounceHandler.removeCallbacksAndMessages(null);
                debounceHandler.postDelayed(() -> loadImages(), DEBOUNCE);
            }
        };

        requireContext().getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true,
                        observer
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getContentResolver()
                    .registerContentObserver(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            true,
                            observer
                    );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (observer != null) {
            requireContext().getContentResolver()
                    .unregisterContentObserver(observer);
            observer = null;
        }
        debounceHandler.removeCallbacksAndMessages(null);
    }
}