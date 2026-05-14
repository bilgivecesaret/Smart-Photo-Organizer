package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.FolderItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderItemAdapter extends RecyclerView.Adapter<FolderItemAdapter.ViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem item);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
    private final Context context;
    private final List<FolderItem> folderList;
    private final OnFolderClickListener listener;
    private OnSelectionChangedListener selectionListener;
    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();
    public FolderItemAdapter(Context context,
                             List<FolderItem> folderList,
                             OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList;
        this.listener = listener;
    }

    public void setSelectionListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
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
        holder.tvPhotoCount.setText(item.getImageUris().size() + " " + context.getString(R.string.photo));

        boolean isSelected = selectedPositions.contains(position);

        Glide.with(context)
                .load(item.getPreviewUri())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imgPreview);

        holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.cbFolderSelect.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(holder.getAdapterPosition());
            } else {
                listener.onFolderClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                toggleSelection(holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }
    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }

        if (selectedPositions.isEmpty()) {
            selectionMode = false;
        }

        notifyItemChanged(position);

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void selectAll(boolean select) {
        selectedPositions.clear();
        if (select) {
            for (int i = 0; i < folderList.size(); i++) {
                selectedPositions.add(i);
            }
        } else {
            selectionMode = false;
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelection() {
        selectedPositions.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    // Seçili klasörlerin tüm Uri'larını döndür
    public List<Uri> getSelectedUris() {
        List<Uri> uris = new ArrayList<>();
        for (int pos : selectedPositions) {
            uris.addAll(folderList.get(pos).getImageUris());
        }
        return uris;
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview, cbFolderSelect;
        TextView txtFolderName, tvPhotoCount;
        View selectionOverlay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
            tvPhotoCount  = itemView.findViewById(R.id.txtPhotoCount);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            cbFolderSelect = itemView.findViewById(R.id.cbFolderSelect);
        }
    }
}
