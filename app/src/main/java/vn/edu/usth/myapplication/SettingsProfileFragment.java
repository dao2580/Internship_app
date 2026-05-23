package vn.edu.usth.myapplication;

import android.os.Bundle;
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

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;
import vn.edu.usth.myapplication.data.remote.SupabaseSession;

public class SettingsProfileFragment extends Fragment {

    private UserDatabase userDatabase;
    private SupabaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_settings_profile, container, false);

        userDatabase = new UserDatabase(requireContext());
        authService = new SupabaseAuthService();

        TextView btnBack = view.findViewById(R.id.btn_back_profile);
        LinearLayout layoutViewCurrentProfile = view.findViewById(R.id.layout_view_current_profile);
        LinearLayout layoutChangePassword = view.findViewById(R.id.layout_change_password);
        LinearLayout layoutChangeEmail = view.findViewById(R.id.layout_change_email);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        layoutViewCurrentProfile.setOnClickListener(v -> showCurrentProfileDialog());

        // Hiện dialog đổi mật khẩu
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Hiện dialog đổi email
        layoutChangeEmail.setOnClickListener(v -> showChangeEmailDialog());

        return view;
    }

    private void showCurrentProfileDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();
        String currentUserId = userDatabase.getCurrentUserId();

        if (currentEmail == null || currentEmail.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Email: " + currentEmail
                + "\n\nUser ID: "
                + (currentUserId != null ? currentUserId : "Không có");

        new AlertDialog.Builder(requireContext())
                .setTitle("Hồ sơ hiện tại")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        EditText edtCurrentPassword = dialogView.findViewById(R.id.edt_current_password);
        EditText edtNewPassword = dialogView.findViewById(R.id.edt_new_password);
        EditText edtConfirmNewPassword = dialogView.findViewById(R.id.edt_confirm_new_password);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = userDatabase.getLoggedInEmail();
                String currentPassword = edtCurrentPassword.getText().toString().trim();
                String newPassword = edtNewPassword.getText().toString().trim();
                String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

                if (email == null || email.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Chưa có tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentPassword.isEmpty()) {
                    edtCurrentPassword.setError("Nhập mật khẩu hiện tại");
                    return;
                }

                if (newPassword.length() < 6) {
                    edtNewPassword.setError("Mật khẩu mới cần ít nhất 6 ký tự");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    edtConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
                    return;
                }

                // Đăng nhập lại để kiểm tra mật khẩu hiện tại
                authService.signIn(email, currentPassword, new SupabaseAuthService.AuthCallback() {
                    @Override
                    public void onSuccess(SupabaseSession session) {
                        userDatabase.saveSupabaseSession(session);

                        authService.updatePassword(session.accessToken, newPassword, new SupabaseAuthService.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(requireContext(), "Không thể đổi mật khẩu", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showChangeEmailDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_email, null);

        EditText edtPassword = dialogView.findViewById(R.id.edt_password_for_email_change);
        EditText edtNewEmail = dialogView.findViewById(R.id.edt_new_email);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String currentEmail = userDatabase.getLoggedInEmail();
                String password = edtPassword.getText().toString().trim();
                String newEmail = edtNewEmail.getText().toString().trim();

                if (currentEmail == null || currentEmail.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Chưa có tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.isEmpty()) {
                    edtPassword.setError("Nhập mật khẩu hiện tại");
                    return;
                }

                if (newEmail.isEmpty() || !newEmail.contains("@")) {
                    edtNewEmail.setError("Email không hợp lệ");
                    return;
                }

                // Đăng nhập lại để xác nhận mật khẩu trước khi đổi email
                authService.signIn(currentEmail, password, new SupabaseAuthService.AuthCallback() {
                    @Override
                    public void onSuccess(SupabaseSession session) {
                        userDatabase.saveSupabaseSession(session);

                        authService.updateEmail(session.accessToken, newEmail, new SupabaseAuthService.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(
                                        requireContext(),
                                        "Đã gửi yêu cầu đổi email. Hãy kiểm tra email để xác nhận.",
                                        Toast.LENGTH_LONG
                                ).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(requireContext(), "Không thể đổi email", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }
}