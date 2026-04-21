package vn.edu.usth.myapplication.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

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
            String safeTargetLang = (targetLang != null && !targetLang.trim().isEmpty())
                    ? targetLang.trim() : "vi";
            String safeMode = (mode != null && !mode.trim().isEmpty())
                    ? mode.trim() : "manual";

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

    public void saveQuizSession(String sessionId, String email, String targetLang, String sourceMode,
                                int totalQuestions, int correctAnswers,
                                int earnedPoints, int maxPoints) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty()) return;
            if (sessionId == null || sessionId.trim().isEmpty()) return;

            QuizSessionEntity s = new QuizSessionEntity();
            s.sessionId = sessionId.trim();
            s.userEmail = email.trim();
            s.targetLang = targetLang != null ? targetLang : "";
            s.sourceMode = sourceMode != null ? sourceMode : "";
            s.totalQuestions = totalQuestions;
            s.correctAnswers = correctAnswers;
            s.earnedPoints = earnedPoints;
            s.maxPoints = maxPoints;

            db.quizSessionDao().insert(s);
        });
    }

    public void saveQuizResult(String email, String question,
                               String correctAnswer, String userAnswer,
                               boolean isCorrect) {
        saveQuizResult(
                email,
                "",
                "",
                "",
                "",
                question,
                correctAnswer,
                userAnswer,
                isCorrect,
                0,
                0
        );
    }

    public void saveQuizResult(String email, String sessionId, String questionType,
                               String targetLang, String wordLabelEn,
                               String question, String correctAnswer,
                               String userAnswer, boolean isCorrect,
                               int pointsEarned, int maxPoints) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty()) return;

            QuizResultEntity r = new QuizResultEntity();
            r.userEmail = email.trim();
            r.sessionId = sessionId != null ? sessionId : "";
            r.questionType = questionType != null ? questionType : "";
            r.targetLang = targetLang != null ? targetLang : "";
            r.wordLabelEn = wordLabelEn != null ? wordLabelEn : "";
            r.question = question != null ? question : "";
            r.correctAnswer = correctAnswer != null ? correctAnswer : "";
            r.userAnswer = userAnswer != null ? userAnswer : "";
            r.isCorrect = isCorrect;
            r.pointsEarned = pointsEarned;
            r.maxPoints = maxPoints;

            db.quizResultDao().insert(r);
        });
    }

    public LiveData<List<QuizResultEntity>> getRecentQuizResultsLive(String email) {
        return db.quizResultDao().getRecentLive(email);
    }

    public LiveData<List<QuizResultEntity>> getRecentWrongQuizResultsLive(String email) {
        return db.quizResultDao().getRecentWrongLive(email);
    }

    public LiveData<List<QuizResultEntity>> getWrongResultsBySessionLive(String sessionId) {
        return db.quizResultDao().getWrongBySessionLive(sessionId);
    }

    public LiveData<List<QuizSessionEntity>> getRecentQuizSessionsLive(String email) {
        return db.quizSessionDao().getRecentLive(email);
    }

    public void getQuizStats(String email, Callback<int[]> cb) {
        executor.execute(() -> {
            int correct = db.quizResultDao().countCorrect(email);
            int total = db.quizResultDao().countTotal(email);
            cb.onResult(new int[]{correct, total});
        });
    }

    public LiveData<List<LearnedWordEntity>> getHistoryWordsLive(String email) {
        return db.learnedWordDao().getHistoryLive(email);
    }

    public LiveData<List<LearnedWordEntity>> getFavoriteWordsLive(String email) {
        return db.learnedWordDao().getFavoritesLive(email);
    }

    public void setFavorite(int wordId, boolean isFavorite) {
        executor.execute(() ->
                db.learnedWordDao().updateFavorite(wordId, isFavorite, System.currentTimeMillis())
        );
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void clearHistory(String email) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty()) return;

            String safeEmail = email.trim();
            db.learnedWordDao().deleteAllByEmail(safeEmail);
            db.quizResultDao().deleteAllByEmail(safeEmail);
            db.quizSessionDao().deleteAllByEmail(safeEmail);
        });
    }

    public void migrateUserEmail(String oldEmail, String newEmail) {
        executor.execute(() -> {
            if (oldEmail == null || oldEmail.trim().isEmpty()) return;
            if (newEmail == null || newEmail.trim().isEmpty()) return;

            String safeOldEmail = oldEmail.trim();
            String safeNewEmail = newEmail.trim();

            db.learnedWordDao().migrateUserEmail(safeOldEmail, safeNewEmail);
            db.quizResultDao().migrateUserEmail(safeOldEmail, safeNewEmail);
            db.quizSessionDao().migrateUserEmail(safeOldEmail, safeNewEmail);
        });
    }
}