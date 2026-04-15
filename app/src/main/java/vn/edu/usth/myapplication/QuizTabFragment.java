package vn.edu.usth.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.myapplication.data.AppRepository;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

public class QuizTabFragment extends Fragment {

    // ===== Model nội bộ =====
    private static class QuizQuestion {
        final int    wordId;
        final String question;
        final String correctAnswer;
        final List<String> options;

        QuizQuestion(int id, String q, String correct, List<String> opts) {
            wordId = id; question = q; correctAnswer = correct; options = opts;
        }
    }

    // ===== Views =====
    private TextView         txtScore, txtQuestion, txtQNumber, txtEmpty;
    private MaterialButton   btnGenerate, btnA, btnB, btnC, btnD;
    private View             cardQuiz;
    private ProgressBar      progressBar;

    // ===== State =====
    private List<QuizQuestion> questions    = new ArrayList<>();
    private int                currentIndex = 0;
    private int                correctCount = 0;

    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_quiz, container, false);

        repository  = new AppRepository(requireContext());
        txtScore    = v.findViewById(R.id.txt_score);
        txtQuestion = v.findViewById(R.id.txt_question);
        txtQNumber  = v.findViewById(R.id.txt_question_number);
        txtEmpty    = v.findViewById(R.id.txt_empty);
        btnGenerate = v.findViewById(R.id.btn_generate_quiz);
        cardQuiz    = v.findViewById(R.id.card_quiz);
        progressBar = v.findViewById(R.id.progress_quiz);
        btnA = v.findViewById(R.id.btn_option_a);
        btnB = v.findViewById(R.id.btn_option_b);
        btnC = v.findViewById(R.id.btn_option_c);
        btnD = v.findViewById(R.id.btn_option_d);

        loadScore();
        btnGenerate.setOnClickListener(x -> generateQuiz());

        return v;
    }

    // ===== Load điểm tích lũy =====
    private void loadScore() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;
        repository.getQuizStats(email, stats ->
                requireActivity().runOnUiThread(() ->
                        txtScore.setText("Điểm tích lũy: " + stats[0] + "/" + stats[1])));
    }

    // ===== Sinh quiz từ learned words =====
    private void generateQuiz() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        txtEmpty.setVisibility(View.GONE);
        cardQuiz.setVisibility(View.GONE);

        repository.getAllWords(email, words -> {
            // Cần ít nhất 4 từ
            if (words.size() < 4) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    txtEmpty.setText("Cần ít nhất 4 từ để tạo quiz.\nHãy học thêm từ vựng!");
                    txtEmpty.setVisibility(View.VISIBLE);
                });
                return;
            }

            List<QuizQuestion> generated = new ArrayList<>();
            Collections.shuffle(words);
            int count = Math.min(words.size(), 10); // tối đa 10 câu

            for (int i = 0; i < count; i++) {
                LearnedWordEntity word = words.get(i);
                String answer = (word.translated != null && !word.translated.isEmpty())
                        ? word.translated : word.labelVi;
                if (answer == null || answer.isEmpty()) continue;

                // Lấy 3 đáp án sai từ các từ khác
                List<String> wrongs = new ArrayList<>();
                for (int j = 0; j < words.size() && wrongs.size() < 3; j++) {
                    if (j == i) continue;
                    String wrong = (words.get(j).translated != null && !words.get(j).translated.isEmpty())
                            ? words.get(j).translated : words.get(j).labelVi;
                    if (wrong != null && !wrong.isEmpty() && !wrong.equals(answer))
                        wrongs.add(wrong);
                }
                if (wrongs.size() < 3) continue;

                List<String> options = new ArrayList<>(wrongs);
                options.add(answer);
                Collections.shuffle(options);

                generated.add(new QuizQuestion(
                        word.id,
                        "'" + word.labelEn + "' nghĩa là gì?",
                        answer,
                        options));
            }

            questions    = generated;
            currentIndex = 0;
            correctCount = 0;

            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnGenerate.setEnabled(true);
                if (questions.isEmpty()) {
                    txtEmpty.setText("Không đủ dữ liệu để tạo quiz.");
                    txtEmpty.setVisibility(View.VISIBLE);
                } else {
                    cardQuiz.setVisibility(View.VISIBLE);
                    showQuestion();
                }
            });
        });
    }

    // ===== Hiển thị câu hỏi =====
    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            // Kết thúc quiz
            cardQuiz.setVisibility(View.GONE);
            txtEmpty.setText("🎉 Hoàn thành! " + correctCount + "/" + questions.size() + " câu đúng");
            txtEmpty.setVisibility(View.VISIBLE);
            loadScore();
            return;
        }

        QuizQuestion q = questions.get(currentIndex);
        txtQNumber.setText("Câu " + (currentIndex + 1) + "/" + questions.size());
        txtQuestion.setText(q.question);

        MaterialButton[] btns = {btnA, btnB, btnC, btnD};
        for (int i = 0; i < 4 && i < q.options.size(); i++) {
            MaterialButton btn = btns[i];
            btn.setText(q.options.get(i));
            btn.setEnabled(true);
            // Reset màu về mặc định
            btn.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1D9E75")));
            btn.setTextColor(getResources().getColor(
                    android.R.color.tab_indicator_text, null));

            final String chosen = q.options.get(i);
            btn.setOnClickListener(x -> handleAnswer(chosen, q));
        }
    }

    // ===== Xử lý câu trả lời =====
    private void handleAnswer(String chosen, QuizQuestion q) {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        boolean correct = chosen.equals(q.correctAnswer);
        if (correct) correctCount++;

        // Highlight đúng/sai
        MaterialButton[] btns = {btnA, btnB, btnC, btnD};
        for (MaterialButton btn : btns) {
            btn.setOnClickListener(null); // vô hiệu hóa click
            String txt = btn.getText().toString();
            if (txt.equals(q.correctAnswer)) {
                // Đáp án đúng → xanh lá
                btn.setStrokeColor(android.content.res.ColorStateList
                        .valueOf(Color.parseColor("#1D9E75")));
                btn.setTextColor(Color.parseColor("#1D9E75"));
            } else if (txt.equals(chosen)) {
                // Đáp án user chọn mà sai → đỏ
                btn.setStrokeColor(android.content.res.ColorStateList
                        .valueOf(Color.parseColor("#E24B4A")));
                btn.setTextColor(Color.parseColor("#E24B4A"));
            }
        }

        // Lưu kết quả + cập nhật mastery
        if (email != null) {
            repository.saveQuizResult(email, q.question,
                    q.correctAnswer, chosen, correct);
            if (correct) repository.markCorrect(q.wordId);
            else         repository.markWrong(q.wordId);
        }

        // Chuyển câu sau 1.2 giây
        cardQuiz.postDelayed(() -> {
            currentIndex++;
            showQuestion();
        }, 1200);
    }
}