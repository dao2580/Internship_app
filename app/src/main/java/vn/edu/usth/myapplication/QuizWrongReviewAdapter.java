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

public class QuizWrongReviewAdapter
        extends RecyclerView.Adapter<QuizWrongReviewAdapter.ViewHolder> {

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
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_wrong_review, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        QuizResultEntity item = items.get(position);

        String questionType = item.questionType != null ? item.questionType : "";
        String targetLang = item.targetLang != null ? item.targetLang : "";

        holder.txtType.setText(questionType + " | " + targetLang);
        holder.txtQuestion.setText(item.question != null ? item.question : "");

        holder.txtUserAnswer.setText(
                holder.itemView.getContext().getString(
                        R.string.quiz_user_answer_prefix,
                        item.userAnswer != null ? item.userAnswer : ""
                )
        );

        holder.txtCorrectAnswer.setText(
                holder.itemView.getContext().getString(
                        R.string.quiz_correct_answer_prefix,
                        item.correctAnswer != null ? item.correctAnswer : ""
                )
        );

        holder.btnSpeakCorrectAnswer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpeakCorrectAnswer(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtType;
        TextView txtQuestion;
        TextView txtUserAnswer;
        TextView txtCorrectAnswer;
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