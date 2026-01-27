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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.FolderItem;

import java.util.List;

public class FolderItemAdapter extends RecyclerView.Adapter<FolderItemAdapter.ViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem item);
    }

    private final Context context;
    private final List<FolderItem> folderList;
    private final OnFolderClickListener listener;

    public FolderItemAdapter(Context context,
                             List<FolderItem> folderList,
                             OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.folder_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        FolderItem item = folderList.get(position);

        holder.txtFolderName.setText(item.getFolderName());

        Glide.with(context)
                .load(item.getPreviewUri())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.imgPreview);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView txtFolderName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.folderIcon);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
        }
    }
}
