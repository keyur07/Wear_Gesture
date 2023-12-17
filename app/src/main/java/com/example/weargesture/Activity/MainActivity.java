package com.example.weargesture.Activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.ConfirmationOverlay;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.example.weargesture.R;
import com.example.weargesture.Services.FloaterService;
import com.example.weargesture.Services.WearConnectService;
import com.example.weargesture.Utills.AnalyticsApplication;
import com.example.weargesture.databinding.ActivityMainBinding;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.wearable.intent.RemoteIntent;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Instance of WearConnectService
    public static WearConnectService wearConnect;

    // Flag to check if the device is in API compatible mode
    public static boolean apiCompatibleMode = false;

    // Flag to determine if the initiation should be performed
    boolean doInitiate = true;

    // Flag to control the visibility of the floater
    public boolean show;

    // Google Analytics tracker
    public Tracker mTracker;

    // Binding for the activity layout
    public ActivityMainBinding binding;

    // Shared preferences to store settings
    public SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize shared preferences
        sharedPref = getSharedPreferences("main", MODE_PRIVATE);

        // Get the show flag from preferences
        show = sharedPref.getBoolean("show", true);

        // Get Google Analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // Set screen name for analytics
        mTracker.setScreenName("Wearable Main");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        try {
            // Check if the activity is started with specific extras
            if (getIntent().getStringExtra("extra").equals("first")) {
                initiateConnection();
                if (show) {
                    initiateFloater();
                }
                finish();
            }

            // Check if the activity is started with specific extras
            if (getIntent().getStringExtra("extra").equals("notini")) {
                doInitiate = false;
            }

            // Check if the activity has a message extra
            if (getIntent().hasExtra("message")) {
                // Display a confirmation overlay with a message
                new ConfirmationOverlay()
                        .setMessage(getIntent().getStringExtra("message"))
                        .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                        .showOn(MainActivity.this);
            }
        } catch (Exception e) {
            // Handle exceptions
        }

        // Check if the floater is not set to show and initiation is required
        if (!show && doInitiate) {
            // Start gesture perform activity
            Intent intent = new Intent(getApplicationContext(), GesturePerformActivity.class);
            startActivity(intent);
            initiateConnection();
            finish();
        }

        // Check if the device is running Oreo or later and the floater is set to show
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && show) {
            if (!sharedPref.getBoolean("oreoWarned", false)) {
                binding.oreoWarn.setVisibility(View.VISIBLE);
            }
        }

        // Set click listeners for buttons
        binding.buttonOreo.setOnClickListener(this);
        binding.buttonall.setOnClickListener(this);
        binding.buttonadd.setOnClickListener(this);
    }

    // Result receiver for handling the result of RemoteIntent
    private final ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == RemoteIntent.RESULT_OK) {
                // Display a confirmation overlay for successful connection
                new ConfirmationOverlay()
                        .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                        .showOn(MainActivity.this);
            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                // Display a confirmation overlay for failed connection
                new ConfirmationOverlay()
                        .setMessage(getString(R.string.connection_phone_disconnect))
                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                        .showOn(MainActivity.this);
            } else {
                throw new IllegalStateException("Unexpected result " + resultCode);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        // Check if initiation is required and perform the initiation
        if (doInitiate) {
            initiateConnection();
            if (show) {
                initiateFloater();
            }
        }

        // Check if the device is in API compatible mode
        if (apiCompatibleMode) {
            // Log an event for analytics
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Wearable Action")
                    .setAction("mainActivityCompatibleMode")
                    .build());

            // Set instructions for the user
            TextView text = findViewById(R.id.textViewIns);
            text.setText(R.string.main_click_perfrom_gesture);

            // Set click listener to start GesturePerformActivity
            text.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), GesturePerformActivity.class);
                startActivity(intent);
            });
        }
    }

    // Method to initiate the floater
    public void initiateFloater() {
        // Check if the device is running Marshmallow or earlier
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            // Start gesture perform activity
            Intent intent = new Intent(getApplicationContext(), GesturePerformActivity.class);
            startActivity(intent);
            return;
        }

        // Check if the device is running Nougat or later and has the overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
            // Start dialog to request overlay permission
            Intent intent = new Intent(MainActivity.this, DialogFirst.class);
            startActivity(intent);
            finish();
            return;
        }

        // Check if the floater view is not already created
        if (FloaterService.frameLayoutfloater == null) {
            // Start the FloaterService
            startService(new Intent(MainActivity.this, FloaterService.class));
        }
    }

    // Method to initiate the WearConnectService
    public void initiateConnection() {
        // Check if WearConnectService is not already created
        if (!WearConnectService.alreadyCreated) {
            // Start the WearConnectService
            startService(new Intent(MainActivity.this, WearConnectService.class));
        }
    }

    // Method to display version alert
    public void versionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.Version_Warning)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    Intent intent = new Intent(getApplicationContext(), GesturePerformActivity.class);
                    startActivity(intent);
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onClick(View view) {
        // Handle button clicks
        if (view.getId() == binding.buttonOreo.getId()) {
            // Set the oreoWarned flag in preferences to true
            sharedPref.edit().putBoolean("oreoWarned", true).apply();
            // Hide the Oreo warning view
            findViewById(R.id.oreoWarn).setVisibility(View.GONE);
            // Log an event for analytics
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Wearable Action")
                    .setAction("Oreo Gone")
                    .build());
        }

        if (view.getId() == binding.buttonall.getId()) {
            // Start AllGestures activity with the "open" extra
            Intent intent = new Intent(getApplicationContext(), AllGestures.class);
            intent.putExtra("open", "y");
            startActivity(intent);
        }

        if (view.getId() == binding.buttonadd.getId()) {
            // Start AddAction activity
            startActivity(new Intent(getApplicationContext(), AddAction.class));
        }
    }
}
