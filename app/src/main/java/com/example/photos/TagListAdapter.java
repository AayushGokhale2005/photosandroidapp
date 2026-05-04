package com.example.photos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Tag;

import java.util.List;

public class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {

    public interface OnTagLongClickListener {
        void onTagLongClick(int position);
    }

    private List<Tag> tags;
    private OnTagLongClickListener longClickListener;

    public TagListAdapter(List<Tag> tags, OnTagLongClickListener longClickListener) {
        this.tags = tags;
        this.longClickListener = longClickListener;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void setLongClickListener(OnTagLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tag tag = tags.get(position);
        holder.tvTag.setText(tag.toString());
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onTagLongClick(position);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return tags != null ? tags.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tvTag);
        }
    }
}
