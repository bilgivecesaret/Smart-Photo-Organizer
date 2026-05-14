package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;

import java.util.List;

public class CleanUpAdapter extends RecyclerView.Adapter<CleanUpAdapter.ViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(String item);
    }
    private final Context context;
    private final List<String> folderList;
    private final OnFolderClickListener listener;


    public CleanUpAdapter(Context context, List<String> folderList, CleanUpAdapter.OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList;
        this.listener = listener;
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
        holder.tvPhotoCount.setVisibility(View.GONE);
        holder.selectionOverlay.setVisibility(View.GONE);
        holder.cbFolderSelect.setVisibility(View.GONE);

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
        ImageView folderIcon, cbFolderSelect;
        TextView txtFolderName, tvPhotoCount;
        View selectionOverlay;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.imgPreview);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
            tvPhotoCount  = itemView.findViewById(R.id.txtPhotoCount);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            cbFolderSelect = itemView.findViewById(R.id.cbFolderSelect);
        }
    }
}
