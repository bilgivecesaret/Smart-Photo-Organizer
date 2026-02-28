package com.example.smart_photo_organizer.util;

public class UnionFind {

    private int[] parent;

    public UnionFind(int size) {
        parent = new int[size];
        for (int i = 0; i < size; i++) parent[i] = i;
    }

    public int find(int x) {
        if (parent[x] != x)
            parent[x] = find(parent[x]);
        return parent[x];
    }

    public void union(int x, int y) {
        int px = find(x);
        int py = find(y);
        if (px != py) parent[px] = py;
    }
}