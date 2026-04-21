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

public class ForgotPasswordFragment extends Fragment {

    private EditText edtEmail;
    private UserDatabase userDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        userDatabase = new UserDatabase(requireContext());

        edtEmail = view.findViewById(R.id.edtEmail);
        Button btnSendReset = view.findViewById(R.id.btnSendReset);
        Button btnBackToLogin = view.findViewById(R.id.btnBackToLogin);

        btnSendReset.setOnClickListener(v -> handleResetPassword());
        btnBackToLogin.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );

        return view;
    }

    private void handleResetPassword() {
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

        if (!userDatabase.checkEmailExists(email)) {
            Toast.makeText(requireContext(),
                    "This email is not registered",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(),
                "Forgot Password via email is not available yet in local Room mode",
                Toast.LENGTH_LONG).show();
    }
}