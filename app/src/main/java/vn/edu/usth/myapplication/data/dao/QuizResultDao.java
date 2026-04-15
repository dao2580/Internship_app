package vn.edu.usth.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import vn.edu.usth.myapplication.data.entity.QuizResultEntity;

@Dao
public interface QuizResultDao {

    @Insert
    void insert(QuizResultEntity result);

    @Query("SELECT * FROM quiz_results WHERE LOWER(userEmail) = LOWER(:email) ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<QuizResultEntity>> getRecentLive(String email);

    @Query("SELECT COUNT(*) FROM quiz_results WHERE LOWER(userEmail) = LOWER(:email) AND isCorrect = 1")
    int countCorrect(String email);

    @Query("SELECT COUNT(*) FROM quiz_results WHERE LOWER(userEmail) = LOWER(:email)")
    int countTotal(String email);
}