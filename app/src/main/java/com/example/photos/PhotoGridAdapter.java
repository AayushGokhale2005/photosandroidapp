package com.example.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Photo;

import java.io.IOException;
import java.io.InputStream;
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
        // Force square cells: width = half the parent width
        int cellSize = parent.getWidth() / 2;
        if (cellSize > 0) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = cellSize;
            view.setLayoutParams(lp);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.imageThumb.setImageBitmap(null);
        holder.imageThumb.setBackgroundColor(0xFFCCCCCC);

        String uriStr = photo.getUriString();
        executor.execute(() -> {
            Bitmap bmp = loadThumbnail(uriStr, 400);
            mainHandler.post(() -> {
                if (holder.getAdapterPosition() == position) {
                    if (bmp != null) {
                        holder.imageThumb.setImageBitmap(bmp);
                    } else {
                        holder.imageThumb.setBackgroundColor(0xFF888888);
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

    private Bitmap loadThumbnail(String uriString, int targetPx) {
        try {
            Uri uri = Uri.parse(uriString);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, bounds);
            }

            int inSample = 1;
            if (bounds.outWidth > targetPx || bounds.outHeight > targetPx) {
                int halfW = bounds.outWidth / 2;
                int halfH = bounds.outHeight / 2;
                while ((halfW / inSample) >= targetPx && (halfH / inSample) >= targetPx) {
                    inSample *= 2;
                }
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = inSample;
            try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
                if (is2 == null) return null;
                return BitmapFactory.decodeStream(is2, null, opts);
            }
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
        }
    }
}
