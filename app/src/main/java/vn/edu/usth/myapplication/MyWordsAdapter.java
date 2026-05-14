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

public class MyWordsAdapter extends RecyclerView.Adapter<MyWordsAdapter.ViewHolder> {

    public interface OnSpeakClickListener {
        void onSpeakClick(LearnedWordEntity item);
    }

    private final List<LearnedWordEntity> items;
    private final OnSpeakClickListener listener;

    public MyWordsAdapter(List<LearnedWordEntity> items, OnSpeakClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_word, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LearnedWordEntity w = items.get(position);

        h.txtEn.setText(w.labelEn != null ? w.labelEn : "");

        h.txtVi.setText(
                w.translated != null && !w.translated.isEmpty()
                        ? w.translated
                        : w.labelVi
        );

        h.txtTimes.setText("Đã học " + w.timesSeen + " lần");
        h.txtMode.setText(w.mode != null ? w.mode : "");

        h.btnSpeak.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpeakClick(w);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtEn;
        TextView txtVi;
        TextView txtTimes;
        TextView txtMode;
        ImageButton btnSpeak;

        ViewHolder(@NonNull View v) {
            super(v);

            txtEn = v.findViewById(R.id.txt_word_en);
            txtVi = v.findViewById(R.id.txt_word_vi);
            txtTimes = v.findViewById(R.id.txt_times_seen);
            txtMode = v.findViewById(R.id.txt_mode);
            btnSpeak = v.findViewById(R.id.btn_speak_word);
        }
    }
}