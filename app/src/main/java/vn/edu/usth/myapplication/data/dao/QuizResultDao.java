package vn.edu.usth.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.QuizResultEntity;

@Dao
public interface QuizResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(QuizResultEntity result);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(QuizResultEntity result);

    /*
     * Old email-based queries.
     * Kept temporarily for backward compatibility.
     */

    @Query("SELECT * FROM quiz_results " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizResultEntity>> getRecentLive(String email);

    @Query("SELECT * FROM quiz_results " +
            "WHERE LOWER(userEmail) = LOWER(:email) AND isCorrect = 0 " +
            "ORDER BY createdAt DESC LIMIT 10")
    LiveData<List<QuizResultEntity>> getRecentWrongLive(String email);

    @Query("SELECT * FROM quiz_results " +
            "WHERE sessionId = :sessionId AND isCorrect = 0 " +
            "ORDER BY createdAt DESC")
    LiveData<List<QuizResultEntity>> getWrongBySessionLive(String sessionId);

    @Query("SELECT COUNT(*) FROM quiz_results " +
            "WHERE LOWER(userEmail) = LOWER(:email) AND isCorrect = 1")
    int countCorrect(String email);

    @Query("SELECT COUNT(*) FROM quiz_results " +
            "WHERE LOWER(userEmail) = LOWER(:email)")
    int countTotal(String email);

    @Query("DELETE FROM quiz_results WHERE LOWER(userEmail) = LOWER(:email)")
    void deleteAllByEmail(String email);

    @Query("UPDATE quiz_results SET userEmail = :newEmail WHERE LOWER(userEmail) = LOWER(:oldEmail)")
    void migrateUserEmail(String oldEmail, String newEmail);

    /*
     * New userId-based queries.
     */

    @Query("SELECT * FROM quiz_results " +
            "WHERE userId = :userId " +
            "ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizResultEntity>> getRecentLiveByUserId(String userId);

    @Query("SELECT * FROM quiz_results " +
            "WHERE userId = :userId AND isCorrect = 0 " +
            "ORDER BY createdAt DESC LIMIT 10")
    LiveData<List<QuizResultEntity>> getRecentWrongLiveByUserId(String userId);

    @Query("SELECT * FROM quiz_results " +
            "WHERE userId = :userId AND sessionId = :sessionId AND isCorrect = 0 " +
            "ORDER BY createdAt DESC")
    LiveData<List<QuizResultEntity>> getWrongBySessionLiveByUserId(String userId, String sessionId);

    @Query("SELECT COUNT(*) FROM quiz_results " +
            "WHERE userId = :userId AND isCorrect = 1")
    int countCorrectByUserId(String userId);

    @Query("SELECT COUNT(*) FROM quiz_results " +
            "WHERE userId = :userId")
    int countTotalByUserId(String userId);

    @Query("SELECT * FROM quiz_results " +
            "WHERE userId = :userId " +
            "ORDER BY createdAt DESC")
    List<QuizResultEntity> getAllByUserId(String userId);

    @Query("SELECT * FROM quiz_results " +
            "WHERE userId = :userId AND isSynced = 0")
    List<QuizResultEntity> getPendingSyncResults(String userId);

    @Query("UPDATE quiz_results " +
            "SET remoteId = :remoteId, isSynced = 1, updatedAt = :updatedAt " +
            "WHERE id = :localId")
    void markResultSynced(int localId, String remoteId, long updatedAt);

    @Query("UPDATE quiz_results " +
            "SET isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :localId")
    void markResultPendingSync(int localId, long updatedAt);

    @Query("UPDATE quiz_results " +
            "SET userId = :userId, isSynced = 0, updatedAt = :now " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "AND (userId IS NULL OR userId = '')")
    void attachOldResultsToUserId(String email, String userId, long now);

    @Query("DELETE FROM quiz_results WHERE userId = :userId")
    void deleteAllByUserId(String userId);
}