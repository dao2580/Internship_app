package vn.edu.usth.myapplication.data.sync;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.UserDatabase;
import vn.edu.usth.myapplication.data.AppDatabase;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;
import vn.edu.usth.myapplication.data.remote.SupabaseQuizService;
import vn.edu.usth.myapplication.data.remote.SupabaseWordService;

public class SyncManager {

    private static final String TAG = "SyncManager";

    private final AppDatabase db;
    private final UserDatabase userDatabase;
    private final SupabaseWordService wordService;
    private final SupabaseQuizService quizService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SyncManager(Context context) {
        Context appContext = context.getApplicationContext();

        db = AppDatabase.getInstance(appContext);
        userDatabase = new UserDatabase(appContext);
        wordService = new SupabaseWordService(appContext);
        quizService = new SupabaseQuizService(appContext);
    }

    public void syncAfterLogin() {
        executor.execute(() -> {
            try {
                attachOldLocalDataToCurrentUser();
            } catch (Exception e) {
                Log.e(TAG, "attachOldLocalDataToCurrentUser failed", e);
            }

            try {
                restoreWordsFromCloudBlocking();
            } catch (Exception e) {
                Log.e(TAG, "restoreWordsFromCloud failed", e);
            }

            try {
                restoreQuizFromCloudBlocking();
            } catch (Exception e) {
                Log.e(TAG, "restoreQuizFromCloud failed", e);
            }

            try {
                syncPendingWordsBlocking();
            } catch (Exception e) {
                Log.e(TAG, "syncPendingWords failed", e);
            }

            try {
                syncPendingQuizBlocking();
            } catch (Exception e) {
                Log.e(TAG, "syncPendingQuiz failed", e);
            }
        });
    }

    public void syncPendingWords() {
        executor.execute(() -> {
            try {
                syncPendingWordsBlocking();
            } catch (Exception e) {
                Log.e(TAG, "syncPendingWords failed", e);
            }
        });
    }

    public void syncPendingQuiz() {
        executor.execute(() -> {
            try {
                syncPendingQuizBlocking();
            } catch (Exception e) {
                Log.e(TAG, "syncPendingQuiz failed", e);
            }
        });
    }

    public void restoreWordsFromCloud() {
        executor.execute(() -> {
            try {
                restoreWordsFromCloudBlocking();
            } catch (Exception e) {
                Log.e(TAG, "restoreWordsFromCloud failed", e);
            }
        });
    }

    public void restoreQuizFromCloud() {
        executor.execute(() -> {
            try {
                restoreQuizFromCloudBlocking();
            } catch (Exception e) {
                Log.e(TAG, "restoreQuizFromCloud failed", e);
            }
        });
    }

    private void attachOldLocalDataToCurrentUser() {
        String email = userDatabase.getLoggedInEmail();
        String userId = userDatabase.getCurrentUserId();

        if (email == null || email.trim().isEmpty()) return;
        if (userId == null || userId.trim().isEmpty()) return;

        long now = System.currentTimeMillis();

        db.learnedWordDao().attachOldWordsToUserId(email.trim(), userId.trim(), now);
        db.quizSessionDao().attachOldSessionsToUserId(email.trim(), userId.trim(), now);
        db.quizResultDao().attachOldResultsToUserId(email.trim(), userId.trim(), now);

        Log.d(TAG, "Attached old local data to userId: " + userId);
    }

    private void syncPendingWordsBlocking() {
        String userId = userDatabase.getCurrentUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "Skip word sync: no logged in user");
            return;
        }

        List<LearnedWordEntity> pending =
                db.learnedWordDao().getPendingSyncWords(userId.trim());

        Log.d(TAG, "Pending words count = " + pending.size());

        for (LearnedWordEntity word : pending) {
            try {
                String remoteId = wordService.upsertWordBlocking(word);

                db.learnedWordDao().markWordSynced(
                        word.id,
                        remoteId,
                        System.currentTimeMillis()
                );

                Log.d(TAG, "Synced word: " + word.labelEn);

            } catch (Exception e) {
                Log.e(TAG, "Failed to sync word: " + word.labelEn, e);
            }
        }
    }

    private void syncPendingQuizBlocking() {
        String userId = userDatabase.getCurrentUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "Skip quiz sync: no logged in user");
            return;
        }

        List<QuizSessionEntity> pendingSessions =
                db.quizSessionDao().getPendingSyncSessions(userId.trim());

        Log.d(TAG, "Pending quiz sessions count = " + pendingSessions.size());

        for (QuizSessionEntity session : pendingSessions) {
            try {
                String remoteId = quizService.upsertSessionBlocking(session);

                db.quizSessionDao().markSessionSynced(
                        session.sessionId,
                        remoteId,
                        System.currentTimeMillis()
                );

                Log.d(TAG, "Synced quiz session: " + session.sessionId);

            } catch (Exception e) {
                Log.e(TAG, "Failed to sync quiz session: " + session.sessionId, e);
            }
        }

        List<QuizResultEntity> pendingResults =
                db.quizResultDao().getPendingSyncResults(userId.trim());

        Log.d(TAG, "Pending quiz results count = " + pendingResults.size());

        for (QuizResultEntity result : pendingResults) {
            try {
                String remoteId = quizService.upsertResultBlocking(result);

                db.quizResultDao().markResultSynced(
                        result.id,
                        remoteId,
                        System.currentTimeMillis()
                );

                Log.d(TAG, "Synced quiz result id: " + result.id);

            } catch (Exception e) {
                Log.e(TAG, "Failed to sync quiz result id: " + result.id, e);
            }
        }
    }

    private void restoreWordsFromCloudBlocking() throws Exception {
        String email = userDatabase.getLoggedInEmail();

        if (email == null || email.trim().isEmpty()) {
            Log.d(TAG, "Skip word restore: no logged in email");
            return;
        }

        List<LearnedWordEntity> words =
                wordService.downloadWordsBlocking(email.trim());

        Log.d(TAG, "Downloaded words count = " + words.size());

        for (LearnedWordEntity word : words) {
            try {
                db.learnedWordDao().insertOrReplace(word);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert restored word: " + word.labelEn, e);
            }
        }
    }

    private void restoreQuizFromCloudBlocking() throws Exception {
        String email = userDatabase.getLoggedInEmail();

        if (email == null || email.trim().isEmpty()) {
            Log.d(TAG, "Skip quiz restore: no logged in email");
            return;
        }

        List<QuizSessionEntity> sessions =
                quizService.downloadSessionsBlocking(email.trim());

        Log.d(TAG, "Downloaded quiz sessions count = " + sessions.size());

        for (QuizSessionEntity session : sessions) {
            try {
                db.quizSessionDao().insertOrReplace(session);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert restored session: " + session.sessionId, e);
            }
        }

        List<QuizResultEntity> results =
                quizService.downloadResultsBlocking(email.trim());

        Log.d(TAG, "Downloaded quiz results count = " + results.size());

        for (QuizResultEntity result : results) {
            try {
                db.quizResultDao().insertOrReplace(result);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert restored quiz result: " + result.id, e);
            }
        }
    }
}