package com.example.smart_photo_organizer.fragment;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;

import java.io.File;
import java.util.ArrayList;

public class PhotosFragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<String> allImages = new ArrayList<>();
    private ContentObserver mediaObserver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photos, container, false);
        recyclerView = view.findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        setupContentObserver();
        loadAllImages();

        return view;
    }

    private void setupContentObserver() {
        mediaObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                requireActivity().runOnUiThread(() -> loadAllImages());
            }
        };

        // Images koleksiyonu
        requireContext().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
        );

        // Downloads koleksiyonu (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getContentResolver().registerContentObserver(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    true,
                    mediaObserver
            );
        }
    }

    private void loadAllImages() {
        allImages.clear();

        // MediaStore Images
        allImages.addAll(queryMediaStore(MediaStore.Images.Media.getContentUri(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                        MediaStore.VOLUME_EXTERNAL : MediaStore.VOLUME_EXTERNAL_PRIMARY
        )));

        // MediaStore Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            allImages.addAll(queryMediaStore(MediaStore.Downloads.EXTERNAL_CONTENT_URI));
        }

        // Dosya sisteminden Downloads klasörü (Android 10+ cihazlarda MediaStore bazen gecikebilir)
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });
            if (files != null) {
                for (File f : files) {
                    // MediaScanner ile Content URI oluştur
                    MediaScannerConnection.scanFile(
                            requireContext(),
                            new String[]{f.getAbsolutePath()},
                            null,
                            (path, uri) -> {
                                if (uri != null) {
                                    allImages.add(uri.toString());
                                    // Adapter güncellemesi
                                    requireActivity().runOnUiThread(() ->
                                            recyclerView.setAdapter(new ImageGridAdapter(requireContext(), allImages, this))
                                    );
                                }
                            });
                }
            }
        }

        // Adapter güncelle (MediaStore’dan gelenler için)
        recyclerView.setAdapter(new ImageGridAdapter(requireContext(), allImages, this));
    }

    private ArrayList<String> queryMediaStore(Uri collectionUri) {
        ArrayList<String> imageList = new ArrayList<>();
        String[] projection = {MediaStore.Images.Media._ID};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = requireContext().getContentResolver().query(
                collectionUri,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(collectionUri, String.valueOf(id));
                    imageList.add(contentUri.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load images.", Toast.LENGTH_SHORT).show();
        }

        return imageList;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllImages();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaObserver != null) {
            requireContext().getContentResolver().unregisterContentObserver(mediaObserver);
        }
    }
}
