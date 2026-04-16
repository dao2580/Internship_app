package vn.edu.usth.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.myapplication.data.entity.QuizResultEntity;

public class QuizWrongReviewAdapter extends RecyclerView.Adapter<QuizWrongReviewAdapter.ViewHolder> {

    public interface OnSpeakCorrectAnswerListener {
        void onSpeakCorrectAnswer(QuizResultEntity item);
    }

    private final List<QuizResultEntity> items = new ArrayList<>();
    private final OnSpeakCorrectAnswerListener listener;

    public QuizWrongReviewAdapter(OnSpeakCorrectAnswerListener listener) {
        this.listener = listener;
    }

    public void submitList(List<QuizResultEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_wrong_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        QuizResultEntity item = items.get(position);

        h.txtType.setText(item.questionType + " | " + item.targetLang);
        h.txtQuestion.setText(item.question);
        h.txtUserAnswer.setText("Bạn trả lời: " + item.userAnswer);
        h.txtCorrectAnswer.setText("Đúng: " + item.correctAnswer);

        h.btnSpeakCorrectAnswer.setOnClickListener(v -> {
            if (listener != null) listener.onSpeakCorrectAnswer(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtType, txtQuestion, txtUserAnswer, txtCorrectAnswer;
        ImageButton btnSpeakCorrectAnswer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtType = itemView.findViewById(R.id.txt_wrong_type);
            txtQuestion = itemView.findViewById(R.id.txt_wrong_question);
            txtUserAnswer = itemView.findViewById(R.id.txt_wrong_user_answer);
            txtCorrectAnswer = itemView.findViewById(R.id.txt_wrong_correct_answer);
            btnSpeakCorrectAnswer = itemView.findViewById(R.id.btn_speak_correct_answer);
        }
    }
}