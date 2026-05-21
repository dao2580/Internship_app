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
            edtEmail.setError(getString(R.string.email_required));
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.valid_email_required));
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError(getString(R.string.password_required));
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError(getString(R.string.password_min_length));
            edtPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.confirm_password_required));
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            edtConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        authService.signUp(email, password, new SupabaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseSession session) {
                btnRegister.setEnabled(true);

                userDatabase.saveSupabaseSession(session);

                new SyncManager(requireContext()).syncAfterLogin();

                Toast.makeText(
                        getContext(),
                        R.string.register_success,
                        Toast.LENGTH_SHORT
                ).show();

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
                        R.string.register_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}