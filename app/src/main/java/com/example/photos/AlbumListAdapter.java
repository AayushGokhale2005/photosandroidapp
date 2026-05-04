package com.example.photos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;

import java.util.List;

public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.ViewHolder> {

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public interface OnAlbumLongClickListener {
        void onAlbumLongClick(Album album);
    }

    private final List<Album> albums;
    private final OnAlbumClickListener clickListener;
    private final OnAlbumLongClickListener longClickListener;

    public AlbumListAdapter(List<Album> albums,
                            OnAlbumClickListener clickListener,
                            OnAlbumLongClickListener longClickListener) {
        this.albums = albums;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.tvAlbumName.setText(album.getName());
        int count = album.getPhotoCount();
        holder.tvPhotoCount.setText(count + (count == 1 ? " photo" : " photos"));
        holder.itemView.setOnClickListener(v -> clickListener.onAlbumClick(album));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAlbumLongClick(album);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlbumName;
        TextView tvPhotoCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlbumName  = itemView.findViewById(R.id.tvAlbumName);
            tvPhotoCount = itemView.findViewById(R.id.tvPhotoCount);
        }
    }
}
