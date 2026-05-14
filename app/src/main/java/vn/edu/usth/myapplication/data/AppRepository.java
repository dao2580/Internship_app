package vn.edu.usth.myapplication.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.UserDatabase;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;
import vn.edu.usth.myapplication.data.sync.SyncManager;

public class AppRepository {

    private final Context appContext;
    private final AppDatabase db;
    private final UserDatabase userDatabase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppRepository(Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(appContext);
        userDatabase = new UserDatabase(appContext);
    }

    private String currentUserId() {
        return userDatabase.getCurrentUserId();
    }

    private String currentEmail() {
        return userDatabase.getLoggedInEmail();
    }

    private boolean hasUserId(String userId) {
        return userId != null && !userId.trim().isEmpty();
    }

    public void saveLearnedWord(String email,
                                String labelEn,
                                String labelVi,
                                String translated,
                                String targetLang,
                                String mode) {
        executor.execute(() -> {
            String userId = currentUserId();
            String userEmail = currentEmail();

            if (!hasUserId(userId)) return;
            if (labelEn == null || labelEn.trim().isEmpty()) return;

            String safeUserId = userId.trim();
            String safeEmail = userEmail != null ? userEmail.trim() : "";
            String safeLabelEn = labelEn.trim();
            String safeLabelVi = labelVi != null ? labelVi.trim() : "";
            String safeTranslated = translated != null ? translated.trim() : "";
            String safeTargetLang = (targetLang != null && !targetLang.trim().isEmpty())
                    ? targetLang.trim()
                    : "vi";
            String safeMode = (mode != null && !mode.trim().isEmpty())
                    ? mode.trim()
                    : "manual";

            long now = System.currentTimeMillis();

            LearnedWordEntity existing = db.learnedWordDao()
                    .findByLabelAndLangByUserId(safeUserId, safeLabelEn, safeTargetLang);

            if (existing != null) {
                existing.userId = safeUserId;
                existing.userEmail = safeEmail;

                existing.timesSeen++;
                existing.lastSeenAt = now;
                existing.updatedAt = now;
                existing.isSynced = false;

                if (!safeLabelVi.isEmpty()) existing.labelVi = safeLabelVi;
                if (!safeTranslated.isEmpty()) existing.translated = safeTranslated;

                existing.targetLang = safeTargetLang;
                existing.mode = safeMode;

                db.learnedWordDao().update(existing);
            } else {
                LearnedWordEntity w = new LearnedWordEntity();

                w.userId = safeUserId;
                w.userEmail = safeEmail;

                w.labelEn = safeLabelEn;
                w.labelVi = safeLabelVi;
                w.translated = safeTranslated;
                w.targetLang = safeTargetLang;
                w.mode = safeMode;

                w.createdAt = now;
                w.lastSeenAt = now;
                w.updatedAt = now;
                w.isSynced = false;
                w.deletedAt = 0;

                db.learnedWordDao().insert(w);
            }

            new SyncManager(appContext).syncPendingWords();
        });
    }

    public LiveData<List<LearnedWordEntity>> getAllWordsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.learnedWordDao().getAllLiveByUserId("__NO_USER__");
        }

        return db.learnedWordDao().getAllLiveByUserId(userId);
    }

    public void getAllWords(String email, Callback<List<LearnedWordEntity>> cb) {
        executor.execute(() -> {
            String userId = currentUserId();

            if (!hasUserId(userId)) {
                cb.onResult(new ArrayList<>());
                return;
            }

            cb.onResult(db.learnedWordDao().getAllByUserId(userId));
        });
    }

    public void getWordCount(String email, Callback<Integer> cb) {
        executor.execute(() -> {
            String userId = currentUserId();

            if (!hasUserId(userId)) {
                cb.onResult(0);
                return;
            }

            cb.onResult(db.learnedWordDao().countAllByUserId(userId));
        });
    }

    public void getWeakWords(String email, int limit, Callback<List<LearnedWordEntity>> cb) {
        executor.execute(() -> {
            String userId = currentUserId();

            if (!hasUserId(userId)) {
                cb.onResult(new ArrayList<>());
                return;
            }

            cb.onResult(db.learnedWordDao().getWeakWordsByUserId(userId, limit));
        });
    }

    public void markCorrect(int wordId) {
        executor.execute(() -> {
            db.learnedWordDao().markCorrect(wordId, System.currentTimeMillis());
            new SyncManager(appContext).syncPendingWords();
        });
    }

    public void markWrong(int wordId) {
        executor.execute(() -> {
            db.learnedWordDao().markWrong(wordId, System.currentTimeMillis());
            new SyncManager(appContext).syncPendingWords();
        });
    }

    public LiveData<List<LearnedWordEntity>> getHistoryWordsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.learnedWordDao().getHistoryLiveByUserId("__NO_USER__");
        }

        return db.learnedWordDao().getHistoryLiveByUserId(userId);
    }

    public LiveData<List<LearnedWordEntity>> getFavoriteWordsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.learnedWordDao().getFavoritesLiveByUserId("__NO_USER__");
        }

        return db.learnedWordDao().getFavoritesLiveByUserId(userId);
    }

    public void setFavorite(int wordId, boolean isFavorite) {
        executor.execute(() -> {
            db.learnedWordDao().updateFavorite(wordId, isFavorite, System.currentTimeMillis());
            new SyncManager(appContext).syncPendingWords();
        });
    }

    public void saveQuizSession(String sessionId,
                                String email,
                                String targetLang,
                                String sourceMode,
                                int totalQuestions,
                                int correctAnswers,
                                int earnedPoints,
                                int maxPoints) {
        executor.execute(() -> {
            String userId = currentUserId();
            String userEmail = currentEmail();

            if (!hasUserId(userId)) return;
            if (sessionId == null || sessionId.trim().isEmpty()) return;

            long now = System.currentTimeMillis();

            QuizSessionEntity s = new QuizSessionEntity();

            s.sessionId = sessionId.trim();
            s.userId = userId.trim();
            s.userEmail = userEmail != null ? userEmail.trim() : "";

            s.targetLang = targetLang != null ? targetLang : "";
            s.sourceMode = sourceMode != null ? sourceMode : "";

            s.totalQuestions = totalQuestions;
            s.correctAnswers = correctAnswers;
            s.earnedPoints = earnedPoints;
            s.maxPoints = maxPoints;

            s.createdAt = now;
            s.updatedAt = now;
            s.deletedAt = 0;
            s.isSynced = false;

            db.quizSessionDao().insert(s);

            new SyncManager(appContext).syncPendingQuiz();
        });
    }

    public void saveQuizResult(String email,
                               String question,
                               String correctAnswer,
                               String userAnswer,
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

    public void saveQuizResult(String email,
                               String sessionId,
                               String questionType,
                               String targetLang,
                               String wordLabelEn,
                               String question,
                               String correctAnswer,
                               String userAnswer,
                               boolean isCorrect,
                               int pointsEarned,
                               int maxPoints) {
        executor.execute(() -> {
            String userId = currentUserId();
            String userEmail = currentEmail();

            if (!hasUserId(userId)) return;

            long now = System.currentTimeMillis();

            QuizResultEntity r = new QuizResultEntity();

            r.userId = userId.trim();
            r.userEmail = userEmail != null ? userEmail.trim() : "";

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

            r.createdAt = now;
            r.updatedAt = now;
            r.deletedAt = 0;
            r.isSynced = false;

            long localId = db.quizResultDao().insert(r);

            if (localId > 0) {
                db.quizResultDao().markResultPendingSync((int) localId, System.currentTimeMillis());
            }

            new SyncManager(appContext).syncPendingQuiz();
        });
    }

    public LiveData<List<QuizResultEntity>> getRecentQuizResultsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.quizResultDao().getRecentLiveByUserId("__NO_USER__");
        }

        return db.quizResultDao().getRecentLiveByUserId(userId);
    }

    public LiveData<List<QuizResultEntity>> getRecentWrongQuizResultsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.quizResultDao().getRecentWrongLiveByUserId("__NO_USER__");
        }

        return db.quizResultDao().getRecentWrongLiveByUserId(userId);
    }

    public LiveData<List<QuizResultEntity>> getWrongResultsBySessionLive(String sessionId) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.quizResultDao().getWrongBySessionLiveByUserId("__NO_USER__", sessionId);
        }

        return db.quizResultDao().getWrongBySessionLiveByUserId(userId, sessionId);
    }

    public LiveData<List<QuizSessionEntity>> getRecentQuizSessionsLive(String email) {
        String userId = currentUserId();

        if (!hasUserId(userId)) {
            return db.quizSessionDao().getRecentLiveByUserId("__NO_USER__");
        }

        return db.quizSessionDao().getRecentLiveByUserId(userId);
    }

    public void getQuizStats(String email, Callback<int[]> cb) {
        executor.execute(() -> {
            String userId = currentUserId();

            if (!hasUserId(userId)) {
                cb.onResult(new int[]{0, 0});
                return;
            }

            int correct = db.quizResultDao().countCorrectByUserId(userId);
            int total = db.quizResultDao().countTotalByUserId(userId);

            cb.onResult(new int[]{correct, total});
        });
    }

    public void clearHistory(String email) {
        executor.execute(() -> {
            String userId = currentUserId();

            if (!hasUserId(userId)) return;

            db.learnedWordDao().deleteAllByUserId(userId);
            db.quizResultDao().deleteAllByUserId(userId);
            db.quizSessionDao().deleteAllByUserId(userId);
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

    public interface Callback<T> {
        void onResult(T result);
    }
}