package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smart_photo_organizer.model.FolderItem;
import com.example.smart_photo_organizer.activity.ImageGridActivity;
import com.example.smart_photo_organizer.R;

import java.util.List;

public class FolderItemAdapter extends RecyclerView.Adapter<FolderItemAdapter.ViewHolder> {

    Context context;
    List<FolderItem> folderList;

    public FolderItemAdapter(Context context, List<FolderItem> folderList) {
        this.context = context;
        this.folderList = folderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.folder_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderItem item = folderList.get(position);

        holder.txtFolderName.setText(item.getFolderName());

        Glide.with(context)
                .load(item.previewImage)
                .into(holder.imgPreview);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImageGridActivity.class);
            intent.putStringArrayListExtra("images", item.imageList);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPreview;
        TextView txtFolderName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
        }
    }
}

