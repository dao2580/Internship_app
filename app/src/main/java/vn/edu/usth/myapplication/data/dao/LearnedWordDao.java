package vn.edu.usth.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

@Dao
public interface LearnedWordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(LearnedWordEntity word);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(LearnedWordEntity word);

    @Update
    void update(LearnedWordEntity word);

    /*
     * Old email-based queries.
     * Keep them temporarily for backward compatibility.
     */

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY lastSeenAt DESC")
    LiveData<List<LearnedWordEntity>> getAllLive(String email);

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY lastSeenAt DESC")
    List<LearnedWordEntity> getAll(String email);

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY lastSeenAt DESC " +
            "LIMIT 50")
    LiveData<List<LearnedWordEntity>> getHistoryLive(String email);

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) AND isFavorite = 1 " +
            "ORDER BY lastSeenAt DESC " +
            "LIMIT 50")
    LiveData<List<LearnedWordEntity>> getFavoritesLive(String email);

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "AND LOWER(labelEn) = LOWER(:labelEn) " +
            "AND LOWER(targetLang) = LOWER(:targetLang) " +
            "LIMIT 1")
    LearnedWordEntity findByLabelAndLang(String email, String labelEn, String targetLang);

    @Query("SELECT COUNT(*) FROM learned_words WHERE LOWER(userEmail) = LOWER(:email)")
    int countAll(String email);

    @Query("SELECT COUNT(*) FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) AND isFavorite = 1")
    int countFavorites(String email);

    @Query("SELECT * FROM learned_words " +
            "WHERE LOWER(userEmail) = LOWER(:email) AND timesWrong > 0 " +
            "ORDER BY (timesWrong * 1.0 / (timesCorrect + timesWrong + 1)) DESC, lastSeenAt DESC " +
            "LIMIT :limit")
    List<LearnedWordEntity> getWeakWords(String email, int limit);

    @Query("DELETE FROM learned_words WHERE LOWER(userEmail) = LOWER(:email)")
    void deleteAllByEmail(String email);

    @Query("UPDATE learned_words SET userEmail = :newEmail WHERE LOWER(userEmail) = LOWER(:oldEmail)")
    void migrateUserEmail(String oldEmail, String newEmail);

    /*
     * New userId-based queries.
     * These should be used after moving to Supabase Auth.
     */

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId " +
            "ORDER BY lastSeenAt DESC")
    LiveData<List<LearnedWordEntity>> getAllLiveByUserId(String userId);

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId " +
            "ORDER BY lastSeenAt DESC")
    List<LearnedWordEntity> getAllByUserId(String userId);

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId " +
            "ORDER BY lastSeenAt DESC " +
            "LIMIT 50")
    LiveData<List<LearnedWordEntity>> getHistoryLiveByUserId(String userId);

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId AND isFavorite = 1 " +
            "ORDER BY lastSeenAt DESC " +
            "LIMIT 50")
    LiveData<List<LearnedWordEntity>> getFavoritesLiveByUserId(String userId);

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId " +
            "AND LOWER(labelEn) = LOWER(:labelEn) " +
            "AND LOWER(targetLang) = LOWER(:targetLang) " +
            "LIMIT 1")
    LearnedWordEntity findByLabelAndLangByUserId(String userId, String labelEn, String targetLang);

    @Query("SELECT COUNT(*) FROM learned_words WHERE userId = :userId")
    int countAllByUserId(String userId);

    @Query("SELECT COUNT(*) FROM learned_words WHERE userId = :userId AND isFavorite = 1")
    int countFavoritesByUserId(String userId);

    @Query("SELECT * FROM learned_words " +
            "WHERE userId = :userId AND timesWrong > 0 " +
            "ORDER BY (timesWrong * 1.0 / (timesCorrect + timesWrong + 1)) DESC, lastSeenAt DESC " +
            "LIMIT :limit")
    List<LearnedWordEntity> getWeakWordsByUserId(String userId, int limit);

    @Query("SELECT * FROM learned_words WHERE userId = :userId AND isSynced = 0")
    List<LearnedWordEntity> getPendingSyncWords(String userId);

    @Query("UPDATE learned_words " +
            "SET remoteId = :remoteId, isSynced = 1, updatedAt = :updatedAt " +
            "WHERE id = :localId")
    void markWordSynced(int localId, String remoteId, long updatedAt);

    @Query("UPDATE learned_words " +
            "SET isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :localId")
    void markWordPendingSync(int localId, long updatedAt);

    @Query("UPDATE learned_words " +
            "SET userId = :userId, isSynced = 0, updatedAt = :now " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "AND (userId IS NULL OR userId = '')")
    void attachOldWordsToUserId(String email, String userId, long now);

    @Query("DELETE FROM learned_words WHERE userId = :userId")
    void deleteAllByUserId(String userId);

    /*
     * Updates should mark the row as pending sync.
     */

    @Query("UPDATE learned_words " +
            "SET timesCorrect = timesCorrect + 1, " +
            "timesSeen = timesSeen + 1, " +
            "lastSeenAt = :now, " +
            "updatedAt = :now, " +
            "isSynced = 0 " +
            "WHERE id = :id")
    void markCorrect(int id, long now);

    @Query("UPDATE learned_words " +
            "SET timesWrong = timesWrong + 1, " +
            "timesSeen = timesSeen + 1, " +
            "lastSeenAt = :now, " +
            "updatedAt = :now, " +
            "isSynced = 0 " +
            "WHERE id = :id")
    void markWrong(int id, long now);

    @Query("UPDATE learned_words " +
            "SET isFavorite = :isFavorite, " +
            "lastSeenAt = :now, " +
            "updatedAt = :now, " +
            "isSynced = 0 " +
            "WHERE id = :id")
    void updateFavorite(int id, boolean isFavorite, long now);
}