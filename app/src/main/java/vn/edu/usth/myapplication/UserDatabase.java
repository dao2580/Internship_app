package vn.edu.usth.myapplication;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vn.edu.usth.myapplication.data.AppDatabase;
import vn.edu.usth.myapplication.data.entity.UserEntity;
import vn.edu.usth.myapplication.data.entity.UserSessionEntity;

public class UserDatabase {

    private static final String TAG = "UserDatabase";

    private final AppDatabase db;
    private final ExecutorService executor;

    public UserDatabase(Context context) {
        db = AppDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }

    private <T> T runBlocking(Callable<T> task, T fallbackValue) {
        Future<T> future = executor.submit(task);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Database operation failed", e);
            return fallbackValue;
        }
    }

    private void runBlockingVoid(Runnable task) {
        Future<?> future = executor.submit(task);
        try {
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Database operation failed", e);
        }
    }

    public boolean checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String safeEmail = email.trim();

        return runBlocking(
                () -> db.userDao().countByEmail(safeEmail) > 0,
                false
        );
    }

    public boolean registerUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            Log.e(TAG, "Invalid email or password");
            return false;
        }

        String safeEmail = email.trim();

        return runBlocking(() -> {
            if (db.userDao().countByEmail(safeEmail) > 0) {
                return false;
            }

            UserEntity user = new UserEntity();
            user.email = safeEmail;
            user.password = password;

            long result = db.userDao().insert(user);
            return result != -1;
        }, false);
    }

    public boolean validateLogin(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null) {
            return false;
        }

        String safeEmail = email.trim();

        return runBlocking(() -> {
            UserEntity user = db.userDao().findByEmail(safeEmail);
            return user != null && password.equals(user.password);
        }, false);
    }

    public boolean isEmailRegistered(String email) {
        return checkEmailExists(email);
    }

    public void saveLoginSession(String email, boolean isLoggedIn) {
        if (!isLoggedIn) {
            logout();
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            Log.e(TAG, "Cannot save session with empty email");
            return;
        }

        String safeEmail = email.trim();

        runBlockingVoid(() -> {
            db.userSessionDao().clearAll();

            UserSessionEntity session = new UserSessionEntity();
            session.sessionId = 1;
            session.email = safeEmail;
            session.isLoggedIn = true;

            db.userSessionDao().insert(session);
        });
    }

    public String getLoggedInEmail() {
        return runBlocking(() -> {
            UserSessionEntity session = db.userSessionDao().getCurrentSession();
            if (session != null && session.isLoggedIn && session.email != null && !session.email.trim().isEmpty()) {
                return session.email;
            }
            return null;
        }, null);
    }

    public boolean isLoggedIn() {
        return getLoggedInEmail() != null;
    }

    public void logout() {
        runBlockingVoid(() -> db.userSessionDao().clearAll());
    }

    public void clearAllData() {
        runBlockingVoid(() -> {
            db.userSessionDao().clearAll();
            db.userDao().deleteAll();
        });
    }

    public String getPasswordByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String safeEmail = email.trim();

        return runBlocking(() -> {
            UserEntity user = db.userDao().findByEmail(safeEmail);
            return user != null ? user.password : null;
        }, null);
    }

    public String getCurrentPassword() {
        String email = getLoggedInEmail();
        if (email == null) return null;
        return getPasswordByEmail(email);
    }

    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        if (email == null || email.trim().isEmpty()) return false;
        if (currentPassword == null || currentPassword.isEmpty()) return false;
        if (newPassword == null || newPassword.isEmpty()) return false;

        String safeEmail = email.trim();

        return runBlocking(() -> {
            UserEntity user = db.userDao().findByEmail(safeEmail);
            if (user == null) return false;
            if (!currentPassword.equals(user.password)) return false;

            int updated = db.userDao().updatePassword(safeEmail, newPassword);
            return updated > 0;
        }, false);
    }

    public boolean updateEmail(String currentEmail, String password, String newEmail) {
        if (currentEmail == null || currentEmail.trim().isEmpty()) return false;
        if (password == null || password.isEmpty()) return false;
        if (newEmail == null || newEmail.trim().isEmpty()) return false;

        String safeCurrentEmail = currentEmail.trim();
        String safeNewEmail = newEmail.trim();

        return runBlocking(() -> {
            UserEntity currentUser = db.userDao().findByEmail(safeCurrentEmail);
            if (currentUser == null) return false;
            if (!password.equals(currentUser.password)) return false;

            UserEntity newEmailUser = db.userDao().findByEmail(safeNewEmail);
            if (newEmailUser != null && !safeCurrentEmail.equalsIgnoreCase(safeNewEmail)) {
                return false;
            }

            final boolean[] success = {false};

            db.runInTransaction(() -> {
                int updatedUsers = db.userDao().updateEmail(safeCurrentEmail, safeNewEmail);
                if (updatedUsers > 0) {
                    db.userSessionDao().updateSessionEmail(safeCurrentEmail, safeNewEmail);
                    success[0] = true;
                }
            });

            return success[0];
        }, false);
    }
}