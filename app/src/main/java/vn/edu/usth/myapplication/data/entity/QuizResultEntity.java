package vn.edu.usth.myapplication.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_results")
public class QuizResultEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userEmail;

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String questionType = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String targetLang = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String wordLabelEn = "";

    public String question;
    public String correctAnswer;
    public String userAnswer;

    public boolean isCorrect;

    @ColumnInfo(defaultValue = "0")
    public int pointsEarned;

    @ColumnInfo(defaultValue = "0")
    public int maxPoints;

    public long createdAt;

    public QuizResultEntity() {
        this.createdAt = System.currentTimeMillis();
    }
}