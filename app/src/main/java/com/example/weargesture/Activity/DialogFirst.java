package com.example.weargesture.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.example.weargesture.R;
import com.example.weargesture.Services.WearConnectService;
import com.example.weargesture.databinding.ActivityDialogFirstBinding;

public class DialogFirst extends Activity implements View.OnClickListener {

    // Flag to track if the user has skipped the setup
    private boolean skipped = false;

    // Binding for the activity layout
    private ActivityDialogFirstBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDialogFirstBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set click listeners for buttons
        binding.buttonsure.setOnClickListener(this);
        binding.buttonskip.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // Handle click events for different buttons
        if (view.getId() == binding.buttonsure.getId()) {
            // Check if the app has the overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getApplicationContext())) {
                // If not, request the overlay permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                    binding.textwelcome.setText(R.string.welcome_begin);
                } catch (Exception e) {
                    // Display a message if permission request fails
                    binding.textwelcome.setText(String.format("Sorry, fail to grant permission, your build version is %d, please contact developer at henryzhang9802@gmail.com and attach the build version. Thank you!", Build.VERSION.SDK_INT));
                }
                binding.buttonsure.setText("Go");
            } else {
                // If the app has the overlay permission, proceed to the main activity
                SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("show", true);
                editor.apply();
                Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                intent2.putExtra("extra", "notini");
                startActivity(intent2);
                finish();
            }
        }

        if (view.getId() == binding.buttonskip.getId()) {
            // Handle the "Skip" button click
            if (skipped) {
                // If already skipped, proceed to the main activity
                Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                intent2.putExtra("extra", "notini");
                startActivity(intent2);
                finish();
            }

            // Check if the app does not have overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getApplicationContext())) {
                // Display a message and update preferences if overlay permission is not granted
                binding.textwelcome.setText(R.string.welcome_not_grant);
                SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("show", false);
                editor.apply();
                WearConnectService.showQuickLauncher = false;
                binding.buttonsure.setVisibility(View.GONE);
                binding.buttonskip.setText("Got it");
                binding.buttonskip.setTextColor(Color.WHITE);
                skipped = true;
            } else {
                // If the app has the overlay permission, proceed to the main activity
                Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                intent2.putExtra("extra", "notini");
                startActivity(intent2);
                finish();
            }
        }
    }
}
