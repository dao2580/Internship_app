package vn.edu.usth.myapplication.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;

public class AppRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void saveLearnedWord(String email, String labelEn, String labelVi,
                                String translated, String targetLang, String mode) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty()) return;
            if (labelEn == null || labelEn.trim().isEmpty()) return;

            String safeEmail = email.trim();
            String safeLabelEn = labelEn.trim();
            String safeLabelVi = labelVi != null ? labelVi.trim() : "";
            String safeTranslated = translated != null ? translated.trim() : "";
            String safeTargetLang = (targetLang != null && !targetLang.trim().isEmpty()) ? targetLang.trim() : "vi";
            String safeMode = (mode != null && !mode.trim().isEmpty()) ? mode.trim() : "manual";

            LearnedWordEntity existing = db.learnedWordDao()
                    .findByLabelAndLang(safeEmail, safeLabelEn, safeTargetLang);

            if (existing != null) {
                existing.timesSeen++;
                existing.lastSeenAt = System.currentTimeMillis();

                if (!safeLabelVi.isEmpty()) existing.labelVi = safeLabelVi;
                if (!safeTranslated.isEmpty()) existing.translated = safeTranslated;

                existing.targetLang = safeTargetLang;
                existing.mode = safeMode;

                db.learnedWordDao().update(existing);
            } else {
                LearnedWordEntity w = new LearnedWordEntity();
                w.userEmail = safeEmail;
                w.labelEn = safeLabelEn;
                w.labelVi = safeLabelVi;
                w.translated = safeTranslated;
                w.targetLang = safeTargetLang;
                w.mode = safeMode;
                db.learnedWordDao().insert(w);
            }
        });
    }

    public LiveData<List<LearnedWordEntity>> getAllWordsLive(String email) {
        return db.learnedWordDao().getAllLive(email);
    }

    public void getAllWords(String email, Callback<List<LearnedWordEntity>> cb) {
        executor.execute(() -> cb.onResult(db.learnedWordDao().getAll(email)));
    }

    public void getWordCount(String email, Callback<Integer> cb) {
        executor.execute(() -> cb.onResult(db.learnedWordDao().countAll(email)));
    }

    public void getWeakWords(String email, int limit, Callback<List<LearnedWordEntity>> cb) {
        executor.execute(() -> cb.onResult(db.learnedWordDao().getWeakWords(email, limit)));
    }

    public void markCorrect(int wordId) {
        executor.execute(() -> db.learnedWordDao().markCorrect(wordId, System.currentTimeMillis()));
    }

    public void markWrong(int wordId) {
        executor.execute(() -> db.learnedWordDao().markWrong(wordId, System.currentTimeMillis()));
    }

    public void saveQuizResult(String email, String question,
                               String correctAnswer, String userAnswer, boolean isCorrect) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty()) return;

            QuizResultEntity r = new QuizResultEntity();
            r.userEmail = email.trim();
            r.question = question != null ? question : "";
            r.correctAnswer = correctAnswer != null ? correctAnswer : "";
            r.userAnswer = userAnswer != null ? userAnswer : "";
            r.isCorrect = isCorrect;
            db.quizResultDao().insert(r);
        });
    }

    public LiveData<List<QuizResultEntity>> getRecentQuizResultsLive(String email) {
        return db.quizResultDao().getRecentLive(email);
    }

    public void getQuizStats(String email, Callback<int[]> cb) {
        executor.execute(() -> {
            int correct = db.quizResultDao().countCorrect(email);
            int total = db.quizResultDao().countTotal(email);
            cb.onResult(new int[]{correct, total});
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}