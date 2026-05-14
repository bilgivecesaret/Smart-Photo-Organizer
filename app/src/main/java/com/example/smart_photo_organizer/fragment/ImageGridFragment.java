package com.example.smart_photo_organizer.fragment;

import android.app.Activity;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.util.Notification;

import java.util.ArrayList;
import java.util.List;

public class ImageGridFragment extends Fragment {
    private ArrayList<Uri> images;
    private RecyclerView recyclerView;
    private ImageGridAdapter adapter;
    private View topBar;
    private Button btnDelete, btnCancel;
    private CheckBox cbSelectAll;

    private long lastDeletedSize = 0;
    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            finalizeDeletion();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_grid, container, false);

        // Görünümleri Bağla
        recyclerView = view.findViewById(R.id.recyclerImages);
        topBar = view.findViewById(R.id.topBarGrid);
        btnDelete = view.findViewById(R.id.btnDeleteGrid);
        btnCancel = view.findViewById(R.id.btnCancelGrid);
        cbSelectAll = view.findViewById(R.id.cbSelectAllGrid);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        images = getArguments() != null ? getArguments().getParcelableArrayList("images") : new ArrayList<>();
        adapter = new ImageGridAdapter(requireContext(), images, getParentFragmentManager());
        recyclerView.setAdapter(adapter);

        // Seçim Dinleyicisi (Activity'deki mantıkla aynı)
        adapter.setSelectionListener(count -> {
            if (count > 0) {
                topBar.setVisibility(View.VISIBLE);
                btnDelete.setText(getString(R.string.delete) + " (" + count + ")");
            } else {
                topBar.setVisibility(View.GONE);
            }

            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(count == adapter.getItemCount() && adapter.getItemCount() > 0);
            cbSelectAll.setOnCheckedChangeListener((b, checked) -> adapter.selectAll(checked));
        });

        btnCancel.setOnClickListener(v -> adapter.clearSelection());
        btnDelete.setOnClickListener(v -> startDeletionProcess());

        return view;
    }

    private void startDeletionProcess() {
        List<Uri> selectedUris = adapter.getSelectedUris();
        if (selectedUris.isEmpty()) return;

        // Boyutu hesapla (Notification util sınıfını kullanarak)
        lastDeletedSize = Notification.calculateTotalSize(requireContext(), selectedUris);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(requireContext().getContentResolver(), selectedUris);
                deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Android 10 ve altı
            for (Uri uri : selectedUris) {
                requireContext().getContentResolver().delete(uri, null, null);
            }
            finalizeDeletion();
        }
    }

    private void finalizeDeletion() {
        // Adaptörden sil ve arayüzü güncelle
        adapter.removeSelectedImages();

        // Başarı diyaloğunu göster
        Notification.showSuccessDialog(
                requireActivity(),
                Notification.formatSize(lastDeletedSize),
                () -> {
                    // Kullanıcı tamam dediğinde yapılacaklar (Örn: Klasöre geri dön)
                    if (images.isEmpty()) {
                        requireActivity().onBackPressed();
                    }
                }
        );
    }
}