package com.example.smart_photo_organizer.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.google.android.material.materialswitch.MaterialSwitch;


public class SettingsFragment extends Fragment {

    private MaterialSwitch permissionSwitch, autoCleanupForSimilarImagesSwitch, autoCleanupForBlurredImagesSwitch, autoAlbumSwitch;
    public static SharedPreferences prefs;
    public static final String PREFS_NAME = "app_prefs";
    private static final String KEY_PERMISSION_SWITCH = "permission_switch";
    public static final String KEY_AUTO_CLEANUP_SIMILAR = "auto_cleanup_similar";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        permissionSwitch = root.findViewById(R.id.permissionSwitch);
        autoCleanupForSimilarImagesSwitch = root.findViewById(R.id.autoCleanupForSimilarImagesSwitch);

        boolean isAutoCleanupActive = prefs.getBoolean(KEY_AUTO_CLEANUP_SIMILAR, false);
        autoCleanupForSimilarImagesSwitch.setChecked(isAutoCleanupActive);

        syncSwitchWithPermissions();

        permissionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Durumu SharedPreferences'a kaydet
                prefs.edit().putBoolean(KEY_PERMISSION_SWITCH, isChecked).apply();

                if (isChecked) {
                    // Aktif: izin yoksa sor
                    PermissionHelper.checkStoragePermissions(getContext(), getActivity());
                } else {
                    // Pasif: izinleri uygulama kaldıramaz, kullanıcıyı ayarlara yönlendir
                    Toast.makeText(getContext(),
                            "Please remove storage permissions in app settings",
                            Toast.LENGTH_LONG).show();
                    PermissionHelper.openAppSettings(getContext());
                }
            }
        });

        autoCleanupForSimilarImagesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AUTO_CLEANUP_SIMILAR, isChecked).apply();
            }
        });

        return root;
    }

    private void syncSwitchWithPermissions() {
        boolean granted = true;
        for (String perm : PermissionHelper.getStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(getContext(), perm)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        permissionSwitch.setChecked(granted);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncSwitchWithPermissions();
    }
}