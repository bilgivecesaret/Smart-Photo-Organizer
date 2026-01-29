package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FullMediaScan {

    public static void rescanAllPublicMedia(Context context) {

        List<File> roots = new ArrayList<>();

        // Dahili storage
        roots.add(Environment.getExternalStorageDirectory());

        // Harici (SD Card varsa)
        File[] externalDirs = context.getExternalMediaDirs();
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir != null) {
                    File root = dir.getParentFile();
                    if (root != null) {
                        roots.add(root);
                    }
                }
            }
        }

        List<String> mediaFiles = new ArrayList<>();
        for (File root : roots) {
            collectMediaFiles(root, mediaFiles);
        }

        if (!mediaFiles.isEmpty()) {
            MediaScannerConnection.scanFile(
                    context,
                    mediaFiles.toArray(new String[0]),
                    null,
                    null
            );
        }
    }

    // 🔒 Kontrollü recursive scan
    private static void collectMediaFiles(File dir, List<String> out) {
        if (dir == null || !dir.exists() || !dir.canRead()) return;

        // Sistem ve private alanları atla
        String path = dir.getAbsolutePath();
        if (path.contains("/Android/data")
                || path.contains("/Android/obb")
                || path.contains("/."))
            return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectMediaFiles(file, out);
            } else if (isImage(file.getName())) {
                out.add(file.getAbsolutePath());
            }
        }
    }

    private static boolean isImage(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".jpg")
                || n.endsWith(".jpeg")
                || n.endsWith(".png")
                || n.endsWith(".webp")
                || n.endsWith(".heic");
    }
}
