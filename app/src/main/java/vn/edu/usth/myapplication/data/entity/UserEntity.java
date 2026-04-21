package vn.edu.usth.myapplication.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {
                @Index(value = {"email"}, unique = true)
        }
)
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String email;
    public String password;
    public long createdAt;

    public UserEntity() {
        this.createdAt = System.currentTimeMillis();
    }
}