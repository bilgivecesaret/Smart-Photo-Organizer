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

    // Eşik Değerler
    private static final long TIME_THRESHOLD = 14400; // 4 Saat (Saniye cinsinden)

    /**
     * Ana Fabrika Metodu: Fotoğrafları gruplar, isimlendirir ve AutoAlbum listesi döner.
     */
    public static List<AutoAlbum> createAutoAlbums(Context context, List<HashItem> allPhotos) {
        List<AutoAlbum> autoAlbums = new ArrayList<>();
        if (allPhotos == null || allPhotos.isEmpty()) return autoAlbums;

        // 1. Gruplandırma mantığını çalıştır (List<List<HashItem>> döner)
        List<List<HashItem>> clusters = clusterPhotos(allPhotos);

        // 2. Her grubu bir AutoAlbum nesnesine dönüştür ve isimlendir
        for (List<HashItem> cluster : clusters) {
            String title = generateAlbumTitle(context, cluster);
            autoAlbums.add(new AutoAlbum(title, cluster));
        }

        return autoAlbums;
    }

    /**
     * Fotoğrafları zaman ve konuma göre listeler halinde gruplar.
     */
    private static List<List<HashItem>> clusterPhotos(List<HashItem> allPhotos) {
        List<List<HashItem>> clusters = new ArrayList<>();
        if (allPhotos == null || allPhotos.isEmpty()) return clusters;

        // Tarihe göre sıralama (Eskiden yeniye)
        Collections.sort(allPhotos, (a, b) -> Long.compare(a.timestamp, b.timestamp));

        List<HashItem> currentCluster = new ArrayList<>();
        currentCluster.add(allPhotos.get(0));

        for (int i = 1; i < allPhotos.size(); i++) {
            HashItem current = allPhotos.get(i);
            HashItem previous = allPhotos.get(i - 1);

            long timeDiff = Math.abs(current.timestamp - previous.timestamp);

            // Konum farkı kontrolü
            boolean isCloseLocally = false;
            if (current.latitude != 0.0 && previous.latitude != 0.0) {
                double dist = Math.sqrt(Math.pow(current.latitude - previous.latitude, 2) +
                        Math.pow(current.longitude - previous.longitude, 2));
                if (dist < 0.005) isCloseLocally = true; // Yaklaşık 500m
            }

            if (timeDiff <= TIME_THRESHOLD || isCloseLocally) {
                currentCluster.add(current);
            } else {
                clusters.add(new ArrayList<>(currentCluster));
                currentCluster.clear();
                currentCluster.add(current);
            }
        }

        if (!currentCluster.isEmpty()) clusters.add(currentCluster);
        return clusters;
    }

    /**
     * Albüm için tarih ve (varsa) konum bazlı isim üretir.
     */
    private static String generateAlbumTitle(Context context, List<HashItem> cluster) {
        HashItem representative = cluster.get(0);

        // Tarih formatı: 27 Ocak 2026
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(representative.timestamp * 1000));

        String locationStr = "";
        if (representative.latitude != 0.0 && representative.longitude != 0.0) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(representative.latitude, representative.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getAdminArea();
                    String district = addresses.get(0).getSubAdminArea();
                    locationStr = " - " + (district != null ? district : city);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dateStr + locationStr;
    }
}
