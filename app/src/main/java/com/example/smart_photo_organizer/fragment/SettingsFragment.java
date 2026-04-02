package com.example.smart_photo_organizer.fragment;

import static com.example.smart_photo_organizer.util.LocaleHelper.setLocale;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.SpinnerAdapter;
import com.example.smart_photo_organizer.model.ListData;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Locale;


public class SettingsFragment extends Fragment {

    private MaterialSwitch permissionSwitch, autoCleanupForSimilarImagesSwitch, autoCleanupForBlurredImagesSwitch;
    private Spinner mSpinner;
    ArrayList<ListData> languages;
    private boolean isFirstSelection = true;
    public static SharedPreferences prefs;
    public static final String PREFS_NAME = "app_prefs";
    private static final String KEY_PERMISSION_SWITCH = "permission_switch";
    public static final String KEY_AUTO_CLEANUP_SIMILAR = "auto_cleanup_similar";
    public static final String KEY_AUTO_CLEANUP_BLURRED = "auto_cleanup_blurred";
    public static final String KEY_LANGUAGE = "app_language";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(getContext(), R.string.notification_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.notification_permission_denied, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        permissionSwitch = root.findViewById(R.id.permissionSwitch);
        autoCleanupForSimilarImagesSwitch = root.findViewById(R.id.autoCleanupForSimilarImagesSwitch);
        autoCleanupForBlurredImagesSwitch = root.findViewById(R.id.autoCleanupForBlurredImagesSwitch);

        boolean isBlurredActive = prefs.getBoolean(KEY_AUTO_CLEANUP_BLURRED, false);
        autoCleanupForBlurredImagesSwitch.setChecked(isBlurredActive);

        boolean isAutoCleanupActive = prefs.getBoolean(KEY_AUTO_CLEANUP_SIMILAR, false);
        autoCleanupForSimilarImagesSwitch.setChecked(isAutoCleanupActive);

        syncSwitchWithPermissions();

        languages = new ArrayList<ListData>();
        prepareData();
        mSpinner = root.findViewById(R.id.spinnerLanguage);
        mSpinner.setAdapter(new SpinnerAdapter(this.getContext(), R.layout.spinner_layout, languages));

        String savedLang = prefs.getString(KEY_LANGUAGE, "en");

        if (savedLang.equals("tr")) {
            mSpinner.setSelection(1);
        } else {
            mSpinner.setSelection(0);
        }

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {

                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }
                String langCode = (index == 0) ? "en" : "tr";
                String currentLang = prefs.getString(KEY_LANGUAGE, "en");
                if (currentLang.equals(langCode)) return;
                prefs.edit().putString(KEY_LANGUAGE, langCode).apply();
                setLocale(langCode);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

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
                            R.string.permission_storage_switched,
                            Toast.LENGTH_LONG).show();
                    PermissionHelper.openAppSettings(getContext());
                }
            }
        });

        autoCleanupForSimilarImagesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AUTO_CLEANUP_SIMILAR, isChecked).apply();
                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
            }
        });

        autoCleanupForBlurredImagesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                prefs.edit().putBoolean(KEY_AUTO_CLEANUP_BLURRED, isChecked).commit();

                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
            }
        });
        return root;
    }

    public void prepareData() {
        ListData prepare_data;
        prepare_data = new ListData();
        prepare_data.setImage(R.drawable.usa);
        prepare_data.setCountry("USD");
        languages.add(prepare_data);

        prepare_data = new ListData();
        prepare_data.setImage(R.drawable.turkey);
        prepare_data.setCountry("TR");
        languages.add(prepare_data);
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