package vn.edu.usth.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class SettingsFragment extends Fragment {

    private UserDatabase userDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        userDatabase = new UserDatabase(requireContext());

        LinearLayout layoutUpdateProfile = view.findViewById(R.id.layout_update_profile);
        LinearLayout layoutManagePrivacy = view.findViewById(R.id.layout_manage_privacy);
        LinearLayout layoutUserPreferences = view.findViewById(R.id.layout_user_preferences);
        LinearLayout btnFeedback = view.findViewById(R.id.btnFeedback);
        LinearLayout layoutLogout = view.findViewById(R.id.layout_logout);

        layoutUpdateProfile.setOnClickListener(v -> navigateTo(R.id.nav_settings_profile));
        layoutManagePrivacy.setOnClickListener(v -> navigateTo(R.id.nav_settings_privacy));
        layoutUserPreferences.setOnClickListener(v -> navigateTo(R.id.nav_settings_user_preferences));

        btnFeedback.setOnClickListener(v -> sendFeedback());

        layoutLogout.setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void navigateTo(int destinationId) {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(destinationId);
    }

    private void sendFeedback() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kingnopro0002@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for CamStudy App");
            intent.putExtra(Intent.EXTRA_TEXT, "Your feedback here...");
            startActivity(Intent.createChooser(intent, "Send feedback via..."));
        } catch (Exception e) {
            Toast.makeText(getContext(), "No email app found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        userDatabase.logout();

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        NavController navController =
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        navController.navigate(R.id.nav_login, null, navOptions);
    }
}