/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: MainActivity.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.edu.usth.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private UserDatabase userDatabase;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * Theme is now applied in MyApplication.onCreate().
         * Locale is applied in attachBaseContext().
         */
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userDatabase = new UserDatabase(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            checkLoginStatus(navController);

            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();

                if (id == R.id.nav_welcome
                        || id == R.id.nav_login
                        || id == R.id.nav_register
                        || id == R.id.nav_forgot_password
                        || id == R.id.nav_photo_preview
                        || id == R.id.nav_translation
                        || id == R.id.nav_streaming) {

                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            });

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    navController.popBackStack(R.id.nav_home, false);

                    if (navController.getCurrentDestination() == null
                            || navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.navigate(R.id.nav_home);
                    }

                    return true;
                } else if (itemId == R.id.nav_camera) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_camera);
                    return true;
                } else if (itemId == R.id.nav_history) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_history);
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_settings);
                    return true;
                }

                return false;
            });
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void checkLoginStatus(NavController navController) {
        if (userDatabase.isLoggedIn()) {
            navController.navigate(R.id.nav_home);
        }
    }
}