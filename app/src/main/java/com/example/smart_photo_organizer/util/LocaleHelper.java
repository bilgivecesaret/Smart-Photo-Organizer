package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class LocaleHelper {
    public static void setLocale(String langCode) {
        LocaleListCompat appLocale =
                LocaleListCompat.forLanguageTags(langCode);

        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    public static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        prefs.edit().putString("lang", lang).apply();
    }
}
