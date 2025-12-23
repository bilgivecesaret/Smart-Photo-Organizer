package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smart_photo_organizer.R;

import java.util.List;

public class CleanUpAdapter extends RecyclerView.Adapter<CleanUpAdapter.ViewHolder> {

    private final Context context;
    private final List<String> folderList;

    public CleanUpAdapter(Context context, List<String> folderList) {
        this.context = context;
        this.folderList = folderList;
    }

    @NonNull
    @Override
    public CleanUpAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.folder_item, parent, false);
        return new CleanUpAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = folderList.get(position);

        holder.txtFolderName.setText(item);

    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView folderIcon;
        TextView txtFolderName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
        }
    }
}