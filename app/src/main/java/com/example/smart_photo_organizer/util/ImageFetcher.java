package com.example.smart_photo_organizer.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.example.smart_photo_organizer.model.HashItem;
import java.util.ArrayList;

public class ImageFetcher {

    public static ArrayList<HashItem> loadAllImages(Context context) {
        ArrayList<HashItem> items = new ArrayList<>();

        // Klasör adını almak için BUCKET_DISPLAY_NAME ekledik
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };

        Uri collectionUri = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) ?
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) :
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try (Cursor cursor = context.getContentResolver().query(
                collectionUri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String bucketName = cursor.getString(bucketColumn);
                    if (bucketName == null) bucketName = "Root";

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // ÖNEMLİ: Hash hesaplama ağır bir işlemdir.
                    // İlk yüklemede null bırakıp, sadece Duplicate araması yapıldığında hesaplatmak daha performanslıdır.
                    // Ancak senin DuplicatePhotoAlbumActivity kodun item.hash'in dolu olmasını bekliyor:
                    String hash = ImagePHash.calculateHash(context, contentUri);

                    items.add(new HashItem(hash, contentUri, bucketName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}