package vn.edu.usth.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

@Dao
public interface QuizSessionDao {

    @Insert
    void insert(QuizSessionEntity session);

    @Query("SELECT * FROM quiz_sessions " +
            "WHERE LOWER(userEmail) = LOWER(:email) " +
            "ORDER BY createdAt DESC LIMIT 5")
    LiveData<List<QuizSessionEntity>> getRecentLive(String email);
}