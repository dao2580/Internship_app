package vn.edu.usth.myapplication.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "learned_words",
        indices = {
                @Index(value = {"userEmail", "labelEn", "targetLang"}, unique = true)
        }
)
public class LearnedWordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userEmail;
    public String labelEn;
    public String labelVi;
    public String translated;
    public String targetLang;
    public String mode;

    public int timesSeen;
    public int timesCorrect;
    public int timesWrong;

    public boolean isFavorite;

    public long createdAt;
    public long lastSeenAt;

    public LearnedWordEntity() {
        this.timesSeen = 1;
        this.timesCorrect = 0;
        this.timesWrong = 0;
        this.isFavorite = false;
        this.createdAt = System.currentTimeMillis();
        this.lastSeenAt = System.currentTimeMillis();
    }
}