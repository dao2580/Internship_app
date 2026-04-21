package vn.edu.usth.myapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import vn.edu.usth.myapplication.data.entity.UserEntity;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(UserEntity user);

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    UserEntity findByEmail(String email);

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)")
    int countByEmail(String email);

    @Query("UPDATE users SET password = :newPassword WHERE LOWER(email) = LOWER(:email)")
    int updatePassword(String email, String newPassword);

    @Query("UPDATE users SET email = :newEmail WHERE LOWER(email) = LOWER(:currentEmail)")
    int updateEmail(String currentEmail, String newEmail);

    @Query("DELETE FROM users")
    void deleteAll();
}