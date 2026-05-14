package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;

public class ForgotPasswordFragment extends Fragment {
    private EditText edtEmail;
    private SupabaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        authService = new SupabaseAuthService();

        edtEmail = view.findViewById(R.id.edtEmail);
        Button btnSendReset = view.findViewById(R.id.btnSendReset);
        Button btnBackToLogin = view.findViewById(R.id.btnBackToLogin);

        btnSendReset.setOnClickListener(v -> handleResetPassword(btnSendReset));
        btnBackToLogin.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        return view;
    }

    private void handleResetPassword(Button btnSendReset) {
        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Please enter a valid email");
            edtEmail.requestFocus();
            return;
        }

        btnSendReset.setEnabled(false);

        authService.sendPasswordResetEmail(email, new SupabaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                btnSendReset.setEnabled(true);
                Toast.makeText(
                        requireContext(),
                        "Password reset email sent. Please check your inbox.",
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onError(String message) {
                btnSendReset.setEnabled(true);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}