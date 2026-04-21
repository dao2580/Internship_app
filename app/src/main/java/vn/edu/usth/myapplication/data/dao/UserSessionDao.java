package vn.edu.usth.myapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import vn.edu.usth.myapplication.data.entity.UserSessionEntity;

@Dao
public interface UserSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserSessionEntity session);

    @Query("SELECT * FROM user_session WHERE sessionId = 1 LIMIT 1")
    UserSessionEntity getCurrentSession();

    @Query("UPDATE user_session SET email = :newEmail WHERE LOWER(email) = LOWER(:currentEmail)")
    int updateSessionEmail(String currentEmail, String newEmail);

    @Query("DELETE FROM user_session")
    void clearAll();
}