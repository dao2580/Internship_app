package vn.edu.usth.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class SettingsUserPreferencesFragment extends Fragment {

    private TextView txtDefaultLanguageValue;
    private TextView txtThemeValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings_user_preferences, container, false);

        TextView btnBack = view.findViewById(R.id.btn_back_preferences);
        txtDefaultLanguageValue = view.findViewById(R.id.txt_default_language_value);
        txtThemeValue = view.findViewById(R.id.txt_theme_value);

        LinearLayout layoutDefaultLanguage = view.findViewById(R.id.layout_default_language);
        LinearLayout layoutSelectTheme = view.findViewById(R.id.layout_select_theme);

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        bindValues();

        layoutDefaultLanguage.setOnClickListener(v -> showLanguageDialog());
        layoutSelectTheme.setOnClickListener(v -> showThemeDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindValues();
    }

    private void bindValues() {
        txtDefaultLanguageValue.setText(SettingsPreferences.getDefaultLanguageName(requireContext()));
        txtThemeValue.setText(SettingsPreferences.isDarkMode(requireContext()) ? "Dark" : "Light");
    }

    private void showLanguageDialog() {
        int checkedItem = SettingsPreferences.getLanguageIndexFromCode(
                SettingsPreferences.getDefaultLanguageCode(requireContext())
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Set Default Language")
                .setSingleChoiceItems(SettingsPreferences.LANGUAGE_NAMES, checkedItem,
                        (dialog, which) -> {
                            String selectedCode = SettingsPreferences.getLanguageCodeAt(which);
                            SettingsPreferences.setDefaultLanguageCode(requireContext(), selectedCode);
                            bindValues();
                            Toast.makeText(requireContext(),
                                    "Default language updated",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark"};
        int checkedItem = SettingsPreferences.isDarkMode(requireContext()) ? 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    boolean newDarkModeValue = which == 1;
                    dialog.dismiss();

                    if (SettingsPreferences.isDarkMode(requireContext()) == newDarkModeValue) {
                        return;
                    }

                    showRestartDialog(newDarkModeValue);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestartDialog(boolean newDarkModeValue) {
        String themeMode = newDarkModeValue ? "Dark Mode" : "Light Mode";

        new AlertDialog.Builder(requireContext())
                .setTitle("Restart Required")
                .setMessage("The app needs to restart to apply " + themeMode + ". Do you want to restart now?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SettingsPreferences.setDarkMode(requireContext(), newDarkModeValue);
                    bindValues();

                    new Handler(Looper.getMainLooper()).postDelayed(this::restartApp, 100);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void restartApp() {
        try {
            Intent intent = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage(requireActivity().getPackageName());

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                requireActivity().finish();
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Please restart the app manually to apply theme changes",
                    Toast.LENGTH_LONG).show();
        }
    }
}