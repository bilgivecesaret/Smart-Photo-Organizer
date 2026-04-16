package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoAlbumCreator {

    private static final long TIME_THRESHOLD = 14400; // 4 hours

    public static List<AutoAlbum> createAutoAlbums(Context context, List<HashItem> allPhotos) {

        List<List<HashItem>> clusters = clusterPhotos(allPhotos);
        List<AutoAlbum> result = new ArrayList<>();

        for (List<HashItem> cluster : clusters) {
            String title = generateAlbumTitle(context, cluster);
            result.add(new AutoAlbum(title, cluster));
        }

        return result;
    }

    private static List<List<HashItem>> clusterPhotos(List<HashItem> allPhotos) {

        List<List<HashItem>> clusters = new ArrayList<>();
        if (allPhotos == null || allPhotos.isEmpty()) return clusters;

        Collections.sort(allPhotos, (a, b) ->
                Long.compare(a.timestamp, b.timestamp));

        List<HashItem> current = new ArrayList<>();
        current.add(allPhotos.get(0));

        for (int i = 1; i < allPhotos.size(); i++) {

            HashItem cur = allPhotos.get(i);
            HashItem prev = allPhotos.get(i - 1);

            long diff = Math.abs(cur.timestamp - prev.timestamp);

            boolean closeLocation = false;

            if (cur.latitude != 0 && prev.latitude != 0) {
                double dist =
                        Math.sqrt(Math.pow(cur.latitude - prev.latitude, 2)
                                + Math.pow(cur.longitude - prev.longitude, 2));

                closeLocation = dist < 0.005;
            }

            if (diff <= TIME_THRESHOLD || closeLocation) {
                current.add(cur);
            } else {
                clusters.add(new ArrayList<>(current));
                current.clear();
                current.add(cur);
            }
        }

        if (!current.isEmpty()) clusters.add(current);

        return clusters;
    }

    private static String generateAlbumTitle(Context context, List<HashItem> cluster) {

        HashItem rep = cluster.get(0);

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

        String dateStr =
                sdf.format(new Date(rep.timestamp * 1000));

        String locationStr = "";

        if (rep.latitude != 0 && rep.longitude != 0) {

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            try {
                List<Address> addresses =
                        geocoder.getFromLocation(rep.latitude, rep.longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getAdminArea();
                    locationStr = city != null ? " - " + city : "";
                }

            } catch (IOException ignored) {}
        }

        return dateStr + locationStr;
    }
}