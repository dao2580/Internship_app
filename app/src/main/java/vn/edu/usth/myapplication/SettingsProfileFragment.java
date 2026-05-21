package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import vn.edu.usth.myapplication.data.remote.SupabaseAuthService;

public class SettingsProfileFragment extends Fragment {

    private UserDatabase userDatabase;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_settings_profile, container, false);

        userDatabase = new UserDatabase(requireContext());

        TextView btnBack = view.findViewById(R.id.btn_back_profile);
        LinearLayout layoutViewCurrentProfile = view.findViewById(R.id.layout_view_current_profile);
        LinearLayout layoutChangePassword = view.findViewById(R.id.layout_change_password);
        LinearLayout layoutChangeEmail = view.findViewById(R.id.layout_change_email);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        layoutViewCurrentProfile.setOnClickListener(v -> showCurrentProfileDialog());
        layoutChangePassword.setOnClickListener(v -> sendPasswordResetEmail());

        layoutChangeEmail.setOnClickListener(v -> Toast.makeText(
                requireContext(),
                R.string.change_email_after_sync,
                Toast.LENGTH_LONG
        ).show());

        return view;
    }

    private void showCurrentProfileDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();
        String currentUserId = userDatabase.getCurrentUserId();

        if (currentEmail == null || currentEmail.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.no_account_logged_in,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String message = getString(R.string.email) + ": " + currentEmail
                + "\n\n" + getString(R.string.user_id) + ": "
                + (currentUserId != null ? currentUserId : getString(R.string.not_available));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.current_profile)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void sendPasswordResetEmail() {
        String email = userDatabase.getLoggedInEmail();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.no_account_logged_in,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new SupabaseAuthService().sendPasswordResetEmail(
                email,
                new SupabaseAuthService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                requireContext(),
                                R.string.password_reset_email_sent,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                requireContext(),
                                R.string.password_reset_email_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}