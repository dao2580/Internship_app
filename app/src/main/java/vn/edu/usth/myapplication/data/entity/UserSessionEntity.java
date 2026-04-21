package vn.edu.usth.myapplication.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_session")
public class UserSessionEntity {

    @PrimaryKey
    public int sessionId = 1;

    public String email;
    public boolean isLoggedIn;
}