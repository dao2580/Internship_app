package vn.edu.usth.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

@Dao
public interface QuizSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(QuizSessionEntity session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(QuizSessionEntity session);

    /*
     * Old email-based queries.
     * Kept temporarily for backward compatibility.
     */

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizSessionEntity>> getRecentLive(String email);

    @Query("DELETE FROM quiz_sessions WHERE LOWER(userEmail) = LOWER(:email)")
    void deleteAllByEmail(String email);

    @Query("UPDATE quiz_sessions SET userEmail = :newEmail WHERE LOWER(userEmail) = LOWER(:oldEmail)")
    void migrateUserEmail(String oldEmail, String newEmail);

    /*
     * New userId-based queries.
     */

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE userId = :userId " +
            "ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizSessionEntity>> getRecentLiveByUserId(String userId);

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE userId = :userId " +
            "ORDER BY createdAt DESC")
    List<QuizSessionEntity> getAllByUserId(String userId);

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE userId = :userId AND isSynced = 0")
    List<QuizSessionEntity> getPendingSyncSessions(String userId);

    @Query("UPDATE quiz_sessions " +
            "SET remoteId = :remoteId, isSynced = 1, updatedAt = :updatedAt " +
            "WHERE sessionId = :sessionId")
    void markSessionSynced(String sessionId, String remoteId, long updatedAt);

    @Query("UPDATE quiz_sessions " +
            "SET isSynced = 0, updatedAt = :updatedAt " +
            "WHERE sessionId = :sessionId")
    void markSessionPendingSync(String sessionId, long updatedAt);

    @Query("UPDATE quiz_sessions " +
            "SET userId = :userId, isSynced = 0, updatedAt = :now " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "AND (userId IS NULL OR userId = '')")
    void attachOldSessionsToUserId(String email, String userId, long now);

    @Query("DELETE FROM quiz_sessions WHERE userId = :userId")
    void deleteAllByUserId(String userId);
}