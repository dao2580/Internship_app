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
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;
import vn.edu.usth.myapplication.data.remote.SupabaseSession;
import vn.edu.usth.myapplication.data.sync.SyncManager;

public class RegisterFragment extends Fragment {

    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;

    private UserDatabase userDatabase;
    private SupabaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        userDatabase = new UserDatabase(requireContext());
        authService = new SupabaseAuthService();

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPass);

        Button btnRegister = view.findViewById(R.id.btnRegister);
        Button btnBack = view.findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(v -> handleRegister(btnRegister));

        btnBack.setOnClickListener(v -> {
            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.popBackStack();
        });
    }

    private void handleRegister(Button btnRegister) {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

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

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            edtPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Please confirm your password");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            edtConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        authService.signUp(email, password, new SupabaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseSession session) {
                btnRegister.setEnabled(true);

                // 1. Lưu session Supabase
                userDatabase.saveSupabaseSession(session);

                // 2. Sau khi register/login thành công, restore/sync dữ liệu
                new SyncManager(requireContext()).syncAfterLogin();

                Toast.makeText(
                        getContext(),
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                ).show();

                // 3. Vào Home luôn
                NavController navController =
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_welcome, true)
                        .build();

                navController.navigate(R.id.nav_home, null, navOptions);
            }

            @Override
            public void onError(String message) {
                btnRegister.setEnabled(true);

                Toast.makeText(
                        getContext(),
                        message != null ? message : "Registration failed",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}