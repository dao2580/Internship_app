package vn.edu.usth.myapplication;

import android.os.Bundle;
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
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;
import vn.edu.usth.myapplication.data.remote.SupabaseSession;
import vn.edu.usth.myapplication.data.sync.SyncManager;

public class LoginFragment extends Fragment {

    private EditText edtEmail;
    private EditText edtPassword;
    private UserDatabase userDatabase;
    private SupabaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        userDatabase = new UserDatabase(requireContext());
        authService = new SupabaseAuthService();

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);

        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);
        TextView txtForgotPassword = view.findViewById(R.id.txtForgotPassword);

        btnLogin.setOnClickListener(v -> handleLogin(btnLogin));

        btnRegister.setOnClickListener(v -> {
            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_register);
        });

        txtForgotPassword.setOnClickListener(v -> {
            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_forgot_password);
        });

        return view;
    }

    private void handleLogin(Button btnLogin) {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(
                    getContext(),
                    R.string.error_enter_email_password,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnLogin.setEnabled(false);

        authService.signIn(email, password, new SupabaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseSession session) {
                btnLogin.setEnabled(true);

                userDatabase.saveSupabaseSession(session);

                new SyncManager(requireContext()).syncAfterLogin();

                Toast.makeText(
                        getContext(),
                        R.string.login_success,
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
                btnLogin.setEnabled(true);

                Toast.makeText(
                        getContext(),
                        R.string.wrong_password,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}