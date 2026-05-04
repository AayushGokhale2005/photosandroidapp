package com.example.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Photo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.ViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    public interface OnPhotoLongClickListener {
        void onPhotoLongClick(int position);
    }

    private final List<Photo> photos;
    private final Context context;
    private final OnPhotoClickListener clickListener;
    private OnPhotoLongClickListener longClickListener;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PhotoGridAdapter(List<Photo> photos, Context context,
                            OnPhotoClickListener clickListener) {
        this.photos = photos;
        this.context = context;
        this.clickListener = clickListener;
    }

    public void setLongClickListener(OnPhotoLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_thumb, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Enforce square cells after the RecyclerView has been laid out
        RecyclerView rv = (RecyclerView) holder.itemView.getParent();
        if (rv != null && rv.getWidth() > 0) {
            GridLayoutManager glm = (GridLayoutManager) rv.getLayoutManager();
            int columns = glm != null ? glm.getSpanCount() : 3;
            int cellSize = rv.getWidth() / columns;
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            lp.width  = cellSize;
            lp.height = cellSize;
            holder.itemView.setLayoutParams(lp);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.imageThumb.setImageBitmap(null);
        holder.imageThumb.setBackgroundColor(0xFF1A1A1A);

        String uriStr = photo.getUriString();
        executor.execute(() -> {
            Bitmap bmp = ImageLoader.loadScaled(context, uriStr, 300);
            mainHandler.post(() -> {
                if (holder.getAdapterPosition() == position) {
                    if (bmp != null) {
                        holder.imageThumb.setImageBitmap(bmp);
                        holder.imageThumb.setBackgroundColor(0x00000000);
                    } else {
                        holder.imageThumb.setBackgroundColor(0xFF333333);
                    }
                }
            });
        });

        holder.itemView.setOnClickListener(v -> clickListener.onPhotoClick(position));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPhotoLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
        }
    }
}
