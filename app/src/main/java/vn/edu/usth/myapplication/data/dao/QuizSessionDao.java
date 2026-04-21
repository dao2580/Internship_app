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

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizSessionEntity>> getRecentLive(String email);

    @Query("DELETE FROM quiz_sessions WHERE LOWER(userEmail) = LOWER(:email)")
    void deleteAllByEmail(String email);

    @Query("UPDATE quiz_sessions SET userEmail = :newEmail WHERE LOWER(userEmail) = LOWER(:oldEmail)")
    void migrateUserEmail(String oldEmail, String newEmail);
}