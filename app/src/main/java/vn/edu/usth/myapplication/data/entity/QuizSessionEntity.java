package vn.edu.usth.myapplication.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_sessions")
public class QuizSessionEntity {

    @PrimaryKey
    @NonNull
    public String sessionId = "";

    public String userEmail;
    public String targetLang;
    public String sourceMode;

    public int totalQuestions;
    public int correctAnswers;
    public int earnedPoints;
    public int maxPoints;

    public long createdAt;

    public QuizSessionEntity() {
        this.createdAt = System.currentTimeMillis();
    }
}