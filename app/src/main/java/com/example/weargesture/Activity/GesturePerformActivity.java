package com.example.weargesture.Activity;

import static com.example.weargesture.Activity.MainActivity.apiCompatibleMode;
import static com.example.weargesture.Activity.MainActivity.wearConnect;
import static com.example.weargesture.Services.WearConnectService.accuracy;
import static com.example.weargesture.Services.WearConnectService.lib;
import static com.example.weargesture.Services.WearConnectService.sendMobileAction;
import static com.example.weargesture.Services.WearConnectService.showQuickLauncher;
import static com.example.weargesture.Services.WearConnectService.vibratorOn;
import static com.example.weargesture.Services.WearConnectService.wait;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weargesture.R;
import com.example.weargesture.Utills.AnalyticsApplication;
import com.example.weargesture.Utills.NameFilter;
import com.example.weargesture.databinding.ActivityGesturePerformBinding;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class GesturePerformActivity extends AppCompatActivity implements View.OnClickListener {

    // Google Analytics tracker
    public Tracker mTracker;

    // Timer for delayed actions
    final Timer timer = new Timer();

    // Progress bar to indicate ongoing action
    ProgressBar mProgress;

    // Gesture overlay view for recognizing gestures
    GestureOverlayView mGesture;

    // Flag to check if the activity is active
    static boolean active = false;

    // Tag for logging
    String tag = "fzg";

    // Binding for the activity layout
    private ActivityGesturePerformBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGesturePerformBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get Google Analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // Set screen name for analytics
        mTracker.setScreenName("Gesture Perform Activity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Log an event for opening Gesture Perform Activity
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("PerformActivity")
                .setAction("openPerformActivity")
                .setLabel("perform")
                .build());

        // Check if the activity is brought to front and finish if so
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        // Initialize UI components
        mProgress = findViewById(R.id.progressBar);
        final Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);

        // Arrays for vibration patterns
        final long[] wrong = {0, 30, 100, 30, 50, 30};
        final long[] right = {0, 30,};

        // Vibrate right pattern if enabled
        if (vibratorOn) {
            v.vibrate(right, -1);
        }

        mGesture = findViewById(R.id.gesture);

        // Hide unnecessary UI components
        mProgress.setVisibility(View.GONE);

        // Gesture listener for handling different gesture states
        mGesture.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
            @Override
            public void onGestureStarted(GestureOverlayView gestureOverlayView, MotionEvent motionEvent) {
                binding.text.setVisibility(View.GONE);
                binding.buttonClose.setVisibility(View.INVISIBLE);
                binding.buttonwhat.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
            }

            @Override
            public void onGesture(GestureOverlayView gestureOverlayView, MotionEvent motionEvent) {
            }

            @Override
            public void onGestureEnded(GestureOverlayView gestureOverlayView, MotionEvent motionEvent) {
                binding.text.setVisibility(View.VISIBLE);
                binding.text.setText(R.string.gesture_matching);
                binding.buttonClose.setVisibility(View.VISIBLE);
            }

            @Override
            public void onGestureCancelled(GestureOverlayView gestureOverlayView, MotionEvent motionEvent) {
                binding.text.setVisibility(View.VISIBLE);
                binding.text.setText(R.string.draw_your_pattern);
                binding.buttonClose.setVisibility(View.VISIBLE);
            }
        });

        // Gesture performed listener for recognizing gestures
        mGesture.addOnGesturePerformedListener((gestureOverlayView, gesture) -> {

            if (!lib.load()) {
                binding.text.setText(R.string.empty_lib);
                // Vibrate wrong pattern if enabled
                if (vibratorOn) {
                    v.vibrate(wrong, -1);
                }
                return;
            }

            ArrayList<Prediction> predictionArrayList = lib.recognize(gesture);  // Recognize the gesture and get predictions
            boolean matched = false;
            double maxfound = 0.0;
            String maxName = "";

            // Iterate through predictions to find the best match
            for (Prediction prediction : predictionArrayList) {
                Log.v(tag, prediction.toString() + "---" + Double.toString(prediction.score));

                if (prediction.score > maxfound) {
                    maxfound = prediction.score;
                    maxName = prediction.name;
                }
            }

            // Check if the best match exceeds the accuracy threshold
            if (maxfound > accuracy) {
                binding.text.setText(String.format(getString(R.string.gesture_sth_performed), maxName));
                matched = true;
                // Vibrate right pattern if enabled
                if (vibratorOn) {
                    v.vibrate(right, -1);
                }
                matchOpen(maxName); // Try to open the corresponding action
            } else {
                binding.text.setText(R.string.gesture_no_match);
            }

            // If no match is found, show the "What's this?" button
            if (!matched) {
                if (vibratorOn) {
                    v.vibrate(wrong, -1);
                }
                binding.buttonwhat.setVisibility(View.VISIBLE);
            }

            // Log analytics event for gesture matching
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("PerformActivity")
                    .setAction("gestureMatched")
                    .setLabel(maxName)
                    .setValue(Math.round(maxfound))
                    .build());
        });

        // Set click listeners for buttons
        binding.buttonClose.setOnClickListener(this);
        binding.buttonwhat.setOnClickListener(this);

        // Show "What's this?" button based on compatibility mode
        if (apiCompatibleMode == true || !showQuickLauncher) {
            binding.buttonwhat.setVisibility(View.VISIBLE);
        }
    }

    // Open the corresponding action based on the matched gesture
    @SuppressLint("SetTextI18n")
    public void matchOpen(String activity) {
        // Hide the gesture overlay view and show the progress bar
        mGesture.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
        binding.buttonClose.setTextColor(Color.rgb(118, 255, 3));

        // Create a NameFilter object for the matched activity
        final NameFilter name = new NameFilter(activity);
        int delay = 0;
        if (wait) {
            delay = 2600;
        }

        TimerTask task = null;

        try {
            switch (name.getMethod()) {
                case "wearapp":
                    if (name.getPackName().equals(getApplicationContext().getPackageName())) {
                        binding.text.setText(R.string.gesture_open_main);

                        // Delayed opening for the main activity
                        task = new TimerTask() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(GesturePerformActivity.this, MainActivity.class);
                                finish();
                                startActivity(intent);
                            }
                        };
                    } else {
                        binding.text.setText(String.format(getString(R.string.gesture_opening), name.getFilteredName()));

                        // Delayed opening for other apps
                        task = new TimerTask() {
                            @Override
                            public void run() {
                                Intent intent = getPackageManager().getLaunchIntentForPackage(name.getPackName());
                                startActivity(intent);
                            }
                        };
                    }
                    break;

                case "timer":
                    binding.text.setText(name.getFilteredName());

                    // Delayed opening for timer-related actions
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            timerOpen(name.getPackName());
                        }
                    };
                    break;

                case "call":
                    binding.text.setText(name.getFilteredName());

                    // Delayed opening for call actions
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            callOpen(name.getPackName());
                        }
                    };
                    break;

                case "mapp":
                    binding.text.setText(String.format(getString(R.string.gesture_open_phone), name.getFilteredName()));

                    // Delayed opening for map-related actions
                    if (delay == 0) {
                        delay = 1000;
                    }
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            mobileOpen(name.getOriginalName());
                            moveTaskToBack(true);
                        }
                    };
                    break;

                case "tasker":
                    if (delay == 0) {
                        delay = 1000;
                    }
                    binding.text.setText(String.format(getString(R.string.gesture_open_phone), name.getFilteredName()));

                    // Delayed opening for Tasker-related actions
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            mobileOpen(name.getOriginalName());
                            moveTaskToBack(true);
                        }
                    };
                    break;
            }
        } catch (Exception e) {
            // Display a message if opening the action fails
            msg(getString(R.string.gesture_failed) + name.getFilteredName());

            // Log an event for analytics
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("PerformActivity")
                    .setAction("failToOpenMatch")
                    .setLabel(activity)
                    .build());

            return;
        }

        // If a task is scheduled, execute it after the specified delay
        if (task != null) {
            timer.schedule(task, delay);
        }
    }

    // Open timer-related actions
    public void timerOpen(String method) {
        Intent intent = null;
        switch (method) {
            case "Alarm":
                intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case "Timer":
                intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case "Stopwatch":
                intent = new Intent("com.google.android.wearable.action.STOPWATCH");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case "Alarm List":
                intent = new Intent("android.intent.action.SHOW_ALARMS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                break;
        }

        // Start the intent if it's not null
        if (intent != null) {
            startActivity(intent);
        }
    }

    // Open call actions
    public void callOpen(String phonenumber) {
        Uri number = Uri.parse("tel:" + phonenumber);
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
        startActivity(callIntent);
    }

    // Open mobile-related actions
    public void mobileOpen(String action) {
        sendMobileAction(wearConnect, action);
    }

    // Called when the activity is started
    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    // Called when the activity is stopped
    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    // Called when the activity is paused
    @Override
    protected void onPause() {
        // Finish and remove the task from the recent tasks list
        finishAndRemoveTask();
        super.onPause();
    }

    // Display a message using a Toast and log it
    public void msg(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.v(tag, message);
    }

    // Called when a button is clicked
    @Override
    public void onClick(View view) {
        if (view.getId() == binding.buttonClose.getId()) {
            // Cancel the timer and purge its tasks
            timer.cancel();
            timer.purge();

            if (mProgress.getVisibility() == View.VISIBLE) {
                // Analytics: User canceled an ongoing action
                mTracker.send(new HitBuilders.EventBuilder().setCategory("PerformActivity").setAction("userCanceledAction").build());
                Intent intent = new Intent(GesturePerformActivity.this, GesturePerformActivity.class);
                finish();
                startActivity(intent);
            } else {
                // Analytics: User canceled the gesture performance
                mTracker.send(new HitBuilders.EventBuilder().setCategory("PerformActivity").setAction("userCanceledPerform").build());
                finish();
            }
        }

        if (view.getId() == binding.buttonwhat.getId()) {
            // Open the MainActivity when the "What's this?" button is clicked
            Intent intent = new Intent(GesturePerformActivity.this, MainActivity.class);
            intent.putExtra("extra", "notini");
            finish();
            startActivity(intent);
        }
    }
}
