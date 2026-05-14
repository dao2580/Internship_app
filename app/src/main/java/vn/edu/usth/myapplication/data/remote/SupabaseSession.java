package vn.edu.usth.myapplication.data.remote;

public class SupabaseSession {

    public String accessToken;
    public String refreshToken;
    public String userId;
    public String email;

    public SupabaseSession(String accessToken, String refreshToken, String userId, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
    }
}