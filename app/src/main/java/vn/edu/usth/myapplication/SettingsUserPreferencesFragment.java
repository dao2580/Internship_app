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

    private TextView txtAppLanguageValue;
    private TextView txtDefaultLanguageValue;
    private TextView txtThemeValue;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_settings_user_preferences, container, false);

        TextView btnBack = view.findViewById(R.id.btn_back_preferences);

        txtAppLanguageValue = view.findViewById(R.id.txt_app_language_value);
        txtDefaultLanguageValue = view.findViewById(R.id.txt_default_language_value);
        txtThemeValue = view.findViewById(R.id.txt_theme_value);

        LinearLayout layoutAppLanguage = view.findViewById(R.id.layout_app_language);
        LinearLayout layoutDefaultLanguage = view.findViewById(R.id.layout_default_language);
        LinearLayout layoutSelectTheme = view.findViewById(R.id.layout_select_theme);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        bindValues();

        layoutAppLanguage.setOnClickListener(v -> showAppLanguageDialog());
        layoutDefaultLanguage.setOnClickListener(v -> showDefaultTranslationLanguageDialog());
        layoutSelectTheme.setOnClickListener(v -> showThemeDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindValues();
    }

    private void bindValues() {
        txtAppLanguageValue.setText(SettingsPreferences.getAppLanguageName(requireContext()));
        txtDefaultLanguageValue.setText(SettingsPreferences.getDefaultLanguageName(requireContext()));

        txtThemeValue.setText(
                SettingsPreferences.isDarkMode(requireContext())
                        ? getString(R.string.dark_mode)
                        : getString(R.string.light_mode)
        );
    }

    private void showAppLanguageDialog() {
        int checkedItem = SettingsPreferences.getLanguageIndexFromCode(
                SettingsPreferences.getAppLanguageCode(requireContext())
        );

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_language)
                .setSingleChoiceItems(SettingsPreferences.LANGUAGE_NAMES_VI, checkedItem, (dialog, which) -> {
                    String selectedCode = SettingsPreferences.getLanguageCodeAt(which);

                    if (selectedCode.equals(SettingsPreferences.getAppLanguageCode(requireContext()))) {
                        dialog.dismiss();
                        return;
                    }

                    SettingsPreferences.setAppLanguageCode(requireContext(), selectedCode);
                    bindValues();
                    dialog.dismiss();

                    showRestartDialog(getString(R.string.app_language));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDefaultTranslationLanguageDialog() {
        int checkedItem = SettingsPreferences.getLanguageIndexFromCode(
                SettingsPreferences.getDefaultLanguageCode(requireContext())
        );

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.default_translation_language)
                .setSingleChoiceItems(SettingsPreferences.LANGUAGE_NAMES_VI, checkedItem, (dialog, which) -> {
                    String selectedCode = SettingsPreferences.getLanguageCodeAt(which);

                    SettingsPreferences.setDefaultLanguageCode(requireContext(), selectedCode);
                    bindValues();

                    Toast.makeText(
                            requireContext(),
                            R.string.default_language_updated,
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showThemeDialog() {
        String[] themes = {
                getString(R.string.light_mode),
                getString(R.string.dark_mode)
        };

        int checkedItem = SettingsPreferences.isDarkMode(requireContext()) ? 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_theme)
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    boolean newDarkModeValue = which == 1;
                    dialog.dismiss();

                    if (SettingsPreferences.isDarkMode(requireContext()) == newDarkModeValue) {
                        return;
                    }

                    SettingsPreferences.setDarkMode(requireContext(), newDarkModeValue);
                    bindValues();

                    showRestartDialog(
                            newDarkModeValue
                                    ? getString(R.string.dark_mode)
                                    : getString(R.string.light_mode)
                    );
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRestartDialog(String changedPart) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restart_required)
                .setMessage(getString(R.string.restart_required_message, changedPart))
                .setPositiveButton(R.string.restart_now, (dialog, which) ->
                        new Handler(Looper.getMainLooper()).postDelayed(this::restartApp, 100)
                )
                .setNegativeButton(R.string.later, null)
                .show();
    }

    private void restartApp() {
        try {
            Intent intent = requireActivity()
                    .getPackageManager()
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
            Toast.makeText(
                    requireContext(),
                    R.string.restart_manually,
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}