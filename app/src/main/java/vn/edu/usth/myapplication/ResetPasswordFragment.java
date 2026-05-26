package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;
import vn.edu.usth.myapplication.data.remote.SupabaseSession;

public class ResetPasswordFragment extends Fragment {

    private TextView txtEmailSent;
    private EditText edtOtp;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;

    private Button btnResendOtp;

    private SupabaseAuthService authService;
    private String resetEmail;

    private final Handler resendHandler = new Handler(Looper.getMainLooper());
    private Runnable resendRunnable;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_reset_password, container, false);

        authService = new SupabaseAuthService();

        txtEmailSent = view.findViewById(R.id.txtEmailSent);
        edtOtp = view.findViewById(R.id.edtOtp);
        edtNewPassword = view.findViewById(R.id.edtNewPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);

        Button btnResetPassword = view.findViewById(R.id.btnResetPassword);
        Button btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        btnResendOtp = view.findViewById(R.id.btnResendOtp);

        Bundle args = getArguments();
        if (args != null) {
            resetEmail = args.getString("email");
        }

        if (resetEmail == null || resetEmail.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.reset_password_invalid_email_retry,
                    Toast.LENGTH_LONG
            ).show();

            view.post(() -> Navigation.findNavController(view).popBackStack());
            return view;
        }

        resetEmail = resetEmail.trim();

        txtEmailSent.setText(
                getString(R.string.reset_password_email_sent_to, resetEmail)
        );

        btnResetPassword.setOnClickListener(v -> handleResetPassword(btnResetPassword));

        btnResendOtp.setOnClickListener(v -> resendOtpCode());

        btnBackToLogin.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack(R.id.nav_login, false)
        );

        return view;
    }

    private void handleResetPassword(Button btnResetPassword) {
        String email = resetEmail;
        String otp = edtOtp.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            edtOtp.setError(getString(R.string.otp_required));
            edtOtp.requestFocus();
            return;
        }
        if (otp.length() != 8) {
            edtOtp.setError(getString(R.string.otp_must_be_8_digits));
            edtOtp.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError(getString(R.string.password_required));
            edtNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError(getString(R.string.password_min_length));
            edtNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.confirm_password_required));
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.password_not_match));
            edtConfirmPassword.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);

        authService.verifyRecoveryOtp(email, otp, new SupabaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseSession session) {
                authService.updatePassword(
                        session.accessToken,
                        newPassword,
                        new SupabaseAuthService.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                btnResetPassword.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        R.string.password_reset_success,
                                        Toast.LENGTH_LONG
                                ).show();

                                Navigation.findNavController(requireView())
                                        .popBackStack(R.id.nav_login, false);
                            }

                            @Override
                            public void onError(String message) {
                                btnResetPassword.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        R.string.password_reset_failed,
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                        }
                );
            }

            @Override
            public void onError(String message) {
                btnResetPassword.setEnabled(true);

                Toast.makeText(
                        requireContext(),
                        R.string.invalid_or_expired_code,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void resendOtpCode() {
        if (resetEmail == null || resetEmail.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.reset_password_invalid_email_retry,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnResendOtp.setEnabled(false);

        authService.sendPasswordResetEmail(
                resetEmail,
                new SupabaseAuthService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                requireContext(),
                                R.string.resend_code_success,
                                Toast.LENGTH_LONG
                        ).show();

                        startResendCooldown(60);
                    }

                    @Override
                    public void onError(String message) {
                        btnResendOtp.setEnabled(true);

                        Toast.makeText(
                                requireContext(),
                                R.string.resend_code_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void startResendCooldown(int seconds) {
        final int[] remainingSeconds = {seconds};

        resendRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingSeconds[0] <= 0) {
                    btnResendOtp.setEnabled(true);
                    btnResendOtp.setText(R.string.resend_verification_code);
                    return;
                }

                btnResendOtp.setEnabled(false);
                btnResendOtp.setText(
                        getString(R.string.resend_code_wait, remainingSeconds[0])
                );

                remainingSeconds[0]--;
                resendHandler.postDelayed(this, 1000);
            }
        };

        resendHandler.post(resendRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (resendRunnable != null) {
            resendHandler.removeCallbacks(resendRunnable);
        }
    }
}