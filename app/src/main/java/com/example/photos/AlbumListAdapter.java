package com.example.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.ViewHolder> {

    public interface OnAlbumClickListener     { void onAlbumClick(Album album); }
    public interface OnAlbumLongClickListener { void onAlbumLongClick(Album album); }

    private final List<Album> albums;
    private final Context context;
    private final OnAlbumClickListener clickListener;
    private final OnAlbumLongClickListener longClickListener;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AlbumListAdapter(List<Album> albums, Context context,
                            OnAlbumClickListener click,
                            OnAlbumLongClickListener longClick) {
        this.albums = albums;
        this.context = context;
        this.clickListener = click;
        this.longClickListener = longClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Enforce square cells based on the recycler's current width
        RecyclerView rv = (RecyclerView) holder.itemView.getParent();
        if (rv != null && rv.getWidth() > 0) {
            int columns = ((GridLayoutManager) rv.getLayoutManager()).getSpanCount();
            int cellSize = (rv.getWidth() - (columns + 1)) / columns; // 1dp gap
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            lp.height = cellSize;
            holder.itemView.setLayoutParams(lp);

            FrameLayout frame = holder.itemView.findViewById(R.id.frameAlbum);
            if (frame != null) {
                ViewGroup.LayoutParams flp = frame.getLayoutParams();
                flp.height = cellSize;
                frame.setLayoutParams(flp);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);
        int count = album.getPhotoCount();
        holder.tvAlbumName.setText(album.getName());
        holder.tvPhotoCount.setText(count + (count == 1 ? " photo" : " photos"));
        holder.imageCover.setImageBitmap(null);
        holder.imageCover.setBackgroundColor(0xFFCCCCCC);

        // Load cover (first photo) asynchronously
        List<Photo> photos = album.getPhotos();
        if (!photos.isEmpty()) {
            String uriStr = photos.get(0).getUriString();
            executor.execute(() -> {
                Bitmap bmp = ImageLoader.loadScaled(context, uriStr, 400);
                mainHandler.post(() -> {
                    if (holder.getAdapterPosition() == position && bmp != null) {
                        holder.imageCover.setImageBitmap(bmp);
                        holder.imageCover.setBackgroundColor(0x00000000);
                    }
                });
            });
        }

        holder.itemView.setOnClickListener(v -> clickListener.onAlbumClick(album));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAlbumLongClick(album);
            return true;
        });
    }

    @Override
    public int getItemCount() { return albums.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCover;
        TextView  tvAlbumName;
        TextView  tvPhotoCount;

        ViewHolder(@NonNull View v) {
            super(v);
            imageCover   = v.findViewById(R.id.imageCover);
            tvAlbumName  = v.findViewById(R.id.tvAlbumName);
            tvPhotoCount = v.findViewById(R.id.tvPhotoCount);
        }
    }
}
