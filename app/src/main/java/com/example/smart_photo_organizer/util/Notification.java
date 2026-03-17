package com.example.smart_photo_organizer.util;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.List;

public class Notification {
    public static long calculateTotalSize(Context context, List<Uri> uris) {
        long totalSize = 0;
        for (Uri uri : uris) {
            try (Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.Media.SIZE}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    totalSize += cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return totalSize;
    }

    // Boyutu okunabilir hale getirmek için (MB/KB)
    public static String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void showSuccessDialog(Activity activity, String savedSpace) {
        if (activity == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                .setTitle("Cleaning is complete!")
                .setMessage(savedSpace + " space was successfully freed up.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    public static void showAutoCleanupInfoDialog(Activity activity, int count) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                .setTitle("Information")
                .setMessage("AutoCleanUp has identified " + count + " similar photos.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
