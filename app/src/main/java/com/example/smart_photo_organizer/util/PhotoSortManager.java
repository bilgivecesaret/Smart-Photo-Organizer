package com.example.smart_photo_organizer.util;

import com.example.smart_photo_organizer.model.HashItem;

import java.util.ArrayList;
import java.util.List;

public class PhotoSortManager {

    public static List<HashItem> copy(List<HashItem> list) {
        return new ArrayList<>(list);
    }

    public static List<HashItem> sortNewest(List<HashItem> list) {
        list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return list;
    }

    public static List<HashItem> sortOldest(List<HashItem> list) {
        list.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        return list;
    }
}
