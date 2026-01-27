package com.example.smart_photo_organizer.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private MaterialSwitch permissionSwitch;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_PERMISSION_SWITCH = "permission_switch";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        permissionSwitch = root.findViewById(R.id.permissionSwitch);

        // Switch durumunu runtime izinleriyle senkronize et
        syncSwitchWithPermissions();

        // Switch listener
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

        return root;
    }

    // Switch'i gerçek izin durumuna göre güncelle
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
        // Kullanıcı ayarlardan izinleri değiştirmiş olabilir, switch'i güncelle
        syncSwitchWithPermissions();
    }
}
