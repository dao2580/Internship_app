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

        /*
         * Khi chuyển sang Supabase Auth, không đổi password bằng Room nữa.
         * Cách an toàn hơn là gửi email reset password qua Supabase.
         */
        layoutChangePassword.setOnClickListener(v -> sendPasswordResetEmail());

        /*
         * Đổi email trên Supabase cần flow riêng:
         * - update email trên Supabase Auth
         * - xác nhận email nếu Supabase bật confirm
         * - đảm bảo user_id không đổi
         *
         * Vì vậy giai đoạn này tạm khóa để tránh làm hỏng account/sync.
         */
        layoutChangeEmail.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Change email will be added after Supabase sync is completed.",
                    Toast.LENGTH_LONG
            ).show();
        });

        return view;
    }

    private void showCurrentProfileDialog() {
        String currentEmail = userDatabase.getLoggedInEmail();
        String currentUserId = userDatabase.getCurrentUserId();

        if (currentEmail == null || currentEmail.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No account logged in",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String message =
                "Email: " + currentEmail +
                        "\n\nUser ID: " + (currentUserId != null ? currentUserId : "Not available");

        new AlertDialog.Builder(requireContext())
                .setTitle("Current Profile")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void sendPasswordResetEmail() {
        String email = userDatabase.getLoggedInEmail();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No account logged in",
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
                                "Password reset email sent. Please check your inbox.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                requireContext(),
                                message != null ? message : "Cannot send reset email",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}