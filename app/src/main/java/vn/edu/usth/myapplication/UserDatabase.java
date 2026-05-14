package vn.edu.usth.myapplication;

import android.content.Context;

import vn.edu.usth.myapplication.data.remote.SupabaseSession;
import vn.edu.usth.myapplication.data.remote.SupabaseSessionManager;

/**
 * This class used to manage local Room users.
 *
 * After moving to Supabase:
 * - Supabase Auth is the real account system.
 * - This class only keeps the current Supabase session.
 * - Local Room users/user_session tables are no longer used for authentication.
 */
public class UserDatabase {

    private final SupabaseSessionManager sessionManager;

    public UserDatabase(Context context) {
        sessionManager = new SupabaseSessionManager(context.getApplicationContext());
    }

    public void saveSupabaseSession(SupabaseSession session) {
        sessionManager.saveSession(session);
    }

    public String getCurrentUserId() {
        return sessionManager.getUserId();
    }

    public String getLoggedInEmail() {
        return sessionManager.getEmail();
    }

    public String getAccessToken() {
        return sessionManager.getAccessToken();
    }

    public String getRefreshToken() {
        return sessionManager.getRefreshToken();
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void logout() {
        sessionManager.clear();
    }

    public void clearAllData() {
        logout();
    }

    /*
     * Deprecated local-account methods.
     * Keep temporarily to avoid compile errors if some old screens still call them.
     * Do not use these for real authentication anymore.
     */

    public boolean checkEmailExists(String email) {
        return false;
    }

    public boolean isEmailRegistered(String email) {
        return false;
    }

    public boolean registerUser(String email, String password) {
        return false;
    }

    public boolean validateLogin(String email, String password) {
        return false;
    }

    public void saveLoginSession(String email, boolean isLoggedIn) {
        if (!isLoggedIn) {
            logout();
        }
    }

    public String getPasswordByEmail(String email) {
        return null;
    }

    public String getCurrentPassword() {
        return null;
    }

    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        return false;
    }

    public boolean updateEmail(String currentEmail, String password, String newEmail) {
        return false;
    }
}