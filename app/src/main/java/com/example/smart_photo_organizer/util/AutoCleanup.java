package com.example.smart_photo_organizer.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.DuplicateGroup;
import com.example.smart_photo_organizer.model.HashItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoCleanup {

    private static final int HAMMING_THRESHOLD = 8;
    public static void runAutoCleanup(
            Activity activity
    ) {

        List<HashItem> allImages = new ArrayList<>();

        ImageFetcher.loadAllImagesAsync(
                activity,
                50,
                new ImageFetcher.ImageBatchCallback() {

                    @Override
                    public void onBatch(List<HashItem> batch) {
                        allImages.addAll(batch);
                    }

                    @Override
                    public void onComplete() {
                        processCleanup(activity, allImages);
                    }
                }
        );
    }

    private static void processCleanup(
            Activity activity,
            List<HashItem> allImages
    ) {

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors()
                );

        for (HashItem item : allImages) {
            executor.execute(() -> {
                if (item.hash == 0L) {
                    item.hash = ImagePHash.calculateHash(
                            activity,
                            item.uri
                    );
                }
            });
        }

        executor.shutdown();

        Executors.newSingleThreadExecutor().execute(() -> {
            while (!executor.isTerminated()) {}

            List<DuplicateGroup> groups =
                    buildGroups(allImages);

            deleteDuplicates(activity, groups);
        });
    }

    private static List<DuplicateGroup> buildGroups(
            List<HashItem> allImages
    ) {

        List<List<HashItem>> temp = new ArrayList<>();
        List<DuplicateGroup> result = new ArrayList<>();

        for (HashItem item : allImages) {

            boolean added = false;

            for (List<HashItem> group : temp) {

                boolean fits = true;

                for (HashItem member : group) {
                    if (ImagePHash.hammingDistance(
                            item.hash,
                            member.hash
                    ) > HAMMING_THRESHOLD) {
                        fits = false;
                        break;
                    }
                }

                if (fits) {
                    group.add(item);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<HashItem> newGroup =
                        new ArrayList<>();
                newGroup.add(item);
                temp.add(newGroup);
            }
        }

        for (List<HashItem> g : temp) {
            if (g.size() > 1) {

                List<Uri> uris = new ArrayList<>();
                for (HashItem h : g)
                    uris.add(h.uri);

                result.add(
                        new DuplicateGroup(
                                g.get(0).hash,
                                uris
                        )
                );
            }
        }

        return result;
    }

    private static void deleteDuplicates(
            Activity activity,
            List<DuplicateGroup> groups
    ) {

        List<Uri> toDelete = new ArrayList<>();

        for (DuplicateGroup group : groups) {

            List<Uri> uris = group.getUris();

            for (int i = 1; i < uris.size(); i++) {
                toDelete.add(uris.get(i));
            }
        }

        if (toDelete.isEmpty()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {

                PendingIntent pendingIntent =
                        MediaStore.createDeleteRequest(
                                activity.getContentResolver(),
                                toDelete
                        );

                activity.startIntentSenderForResult(
                        pendingIntent.getIntentSender(),
                        9999,
                        null,
                        0,
                        0,
                        0
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            // Android 9 ve altı
            ContentResolver resolver =
                    activity.getContentResolver();

            for (Uri uri : toDelete) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {}
            }
        }
    }
}