package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.AppRepository;

public class SettingsProfileFragment extends Fragment {

    private UserDatabase userDatabase;
    private AppRepository appRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings_profile, container, false);

        userDatabase = new UserDatabase(requireContext());
        appRepository = new AppRepository(requireContext());

        TextView btnBack = view.findViewById(R.id.btn_back_profile);
        LinearLayout layoutViewCurrentProfile = view.findViewById(R.id.layout_view_current_profile);
        LinearLayout layoutChangePassword = view.findViewById(R.id.layout_change_password);
        LinearLayout layoutChangeEmail = view.findViewById(R.id.layout_change_email);

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        layoutViewCurrentProfile.setOnClickListener(v -> showCurrentProfileDialog());
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        layoutChangeEmail.setOnClickListener(v -> showChangeEmailDialog());

        return view;
    }

    private void showCurrentProfileDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();
        String currentPassword = userDatabase.getCurrentPassword();

        if (currentEmail == null) {
            Toast.makeText(requireContext(), "No account logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Email: " + currentEmail + "\n\nPassword: " +
                (currentPassword != null ? currentPassword : "Not available");

        new AlertDialog.Builder(requireContext())
                .setTitle("Current Profile")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showChangePasswordDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();

        if (currentEmail == null) {
            Toast.makeText(requireContext(), "No account logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null, false);

        EditText edtCurrentPassword = dialogView.findViewById(R.id.edt_current_password);
        EditText edtNewPassword = dialogView.findViewById(R.id.edt_new_password);
        EditText edtConfirmNewPassword = dialogView.findViewById(R.id.edt_confirm_new_password);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String currentPassword = edtCurrentPassword.getText().toString().trim();
                    String newPassword = edtNewPassword.getText().toString().trim();
                    String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(currentPassword)) {
                        edtCurrentPassword.setError("Current password is required");
                        edtCurrentPassword.requestFocus();
                        return;
                    }

                    if (TextUtils.isEmpty(newPassword)) {
                        edtNewPassword.setError("New password is required");
                        edtNewPassword.requestFocus();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        edtNewPassword.setError("Password must be at least 6 characters");
                        edtNewPassword.requestFocus();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        edtConfirmNewPassword.setError("Passwords do not match");
                        edtConfirmNewPassword.requestFocus();
                        return;
                    }

                    boolean success = userDatabase.updatePassword(
                            currentEmail,
                            currentPassword,
                            newPassword
                    );

                    if (success) {
                        Toast.makeText(requireContext(),
                                "Password changed successfully",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(),
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.show();
    }

    private void showChangeEmailDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();

        if (currentEmail == null) {
            Toast.makeText(requireContext(), "No account logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_email, null, false);

        EditText edtPassword = dialogView.findViewById(R.id.edt_password_for_email_change);
        EditText edtNewEmail = dialogView.findViewById(R.id.edt_new_email);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = edtPassword.getText().toString().trim();
                    String newEmail = edtNewEmail.getText().toString().trim();

                    if (TextUtils.isEmpty(password)) {
                        edtPassword.setError("Current password is required");
                        edtPassword.requestFocus();
                        return;
                    }

                    if (TextUtils.isEmpty(newEmail)) {
                        edtNewEmail.setError("New email is required");
                        edtNewEmail.requestFocus();
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                        edtNewEmail.setError("Please enter a valid email");
                        edtNewEmail.requestFocus();
                        return;
                    }

                    if (currentEmail.equalsIgnoreCase(newEmail)) {
                        edtNewEmail.setError("New email must be different");
                        edtNewEmail.requestFocus();
                        return;
                    }

                    boolean success = userDatabase.updateEmail(currentEmail, password, newEmail);

                    if (success) {
                        appRepository.migrateUserEmail(currentEmail, newEmail);

                        Toast.makeText(requireContext(),
                                "Email changed successfully",
                                Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(),
                                "Cannot change email. Check password or email already exists.",
                                Toast.LENGTH_LONG).show();
                    }
                }));

        dialog.show();
    }
}