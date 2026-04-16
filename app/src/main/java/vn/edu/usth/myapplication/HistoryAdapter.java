package vn.edu.usth.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnFavoriteClickListener {
        void onFavoriteClick(LearnedWordEntity item);
    }

    private final List<LearnedWordEntity> items;
    private final OnFavoriteClickListener favoriteListener;

    public HistoryAdapter(List<LearnedWordEntity> items, OnFavoriteClickListener favoriteListener) {
        this.items = items;
        this.favoriteListener = favoriteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearnedWordEntity entry = items.get(position);

        holder.sourceText.setText(entry.labelEn);
        holder.translatedText.setText(
                entry.translated != null && !entry.translated.isEmpty()
                        ? entry.translated
                        : entry.labelVi
        );

        if (holder.btnFavorite != null) {
            holder.btnFavorite.setImageResource(
                    entry.isFavorite
                            ? android.R.drawable.btn_star_big_on
                            : android.R.drawable.btn_star_big_off
            );

            holder.btnFavorite.setOnClickListener(v -> {
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(entry);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sourceText;
        TextView translatedText;
        ImageButton btnFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            sourceText = itemView.findViewById(R.id.et_source_text);
            translatedText = itemView.findViewById(R.id.et_translated_text);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_word);
        }
    }
}