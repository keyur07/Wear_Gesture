package com.example.weargesture.Activity;

import static com.example.weargesture.Activity.MainActivity.wearConnect;
import static com.example.weargesture.Services.WearConnectService.lib;
import static com.example.weargesture.Services.WearConnectService.sendMobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;
import androidx.wear.widget.ConfirmationOverlay;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.Prediction;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.weargesture.R;
import com.example.weargesture.Utills.AnalyticsApplication;
import com.example.weargesture.Utills.NameFilter;
import com.example.weargesture.databinding.ActivityAddConfirmGestureBinding;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

public class AddConfirmGesture extends WearableActivity implements View.OnClickListener {

    // Variables to store method name, filtered name, and gesture
    String methodNameForReturn;
    String filteredName;
    Gesture gesture;

    private ActivityAddConfirmGestureBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddConfirmGestureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setAmbientEnabled();

        // Retrieve method name, filtered name, and gesture from the intent
        methodNameForReturn = getIntent().getStringExtra("method");
        filteredName = getIntent().getStringExtra("name");
        gesture = getIntent().getParcelableExtra("gesture");

        // Set text and image based on the received gesture
        binding.textViewAction.setText(filteredName);
        binding.imageViewConfirm.setImageBitmap(gesture.toBitmap(150, 150, 10, Color.YELLOW));

        // Perform collision check for the gesture
        collisionCheck(gesture);

        // Set click listeners for buttons
        binding.buttonConfirm.setOnClickListener(this);
        binding.buttonConfirmCancel.setOnClickListener(this);
    }

    // Method to perform collision check for the gesture
    private void collisionCheck(Gesture gesture) {
        TextView text = findViewById(R.id.textCollision);
        ArrayList<Prediction> predictionArrayList = lib.recognize(gesture);

        double maxFound = 0.0;
        String maxName = "";

        for (Prediction prediction : predictionArrayList) {
            Log.v("fzg", prediction.toString() + "---" + Double.toString(prediction.score));

            if (prediction.score > maxFound) {
                maxFound = prediction.score;
                maxName = prediction.name;
            }
        }

        // Display collision information
        if (maxFound > 2.0) {
            String gestureName = new NameFilter(maxName).getFilteredName();
            int similarity = (int) ((maxFound / 5) * 100);
            text.setText(String.format(getString(R.string.confirm_collision_similar), gestureName, similarity));
        } else {
            text.setText(getString(R.string.confirm_collision_clear));
        }
    }

    // Ambient mode lifecycle methods
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    // Method to update the display based on ambient mode
    private void updateDisplay() {
        if (isAmbient()) {
            binding.container.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            binding.container.setBackgroundColor(getResources().getColor(R.color.dark_grey));
        }
    }

    // Click listener for buttons
    @Override
    public void onClick(View view) {
        if (view.getId() == binding.buttonConfirm.getId()) {
            // Save the gesture, send analytics event, and navigate to MainActivity
            lib.addGesture(methodNameForReturn, gesture);
            lib.save();

            Tracker mTracker;
            AnalyticsApplication application = (AnalyticsApplication) getApplication();
            mTracker = application.getDefaultTracker();
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Wearable Action")
                    .setAction("newGestureAdded")
                    .setLabel(methodNameForReturn)
                    .build());

            final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("extra", "notini");

            // Display confirmation message and navigate to MainActivity
            new ConfirmationOverlay().setFinishedAnimationListener(() -> {
                        startActivity(intent);
                        finish();
                        sendMobile(wearConnect);
                    })
                    .setMessage(getString(R.string.gesture_saved))
                    .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                    .showOn(AddConfirmGesture.this);
        }

        if (view.getId() == binding.buttonConfirmCancel.getId()) {
            // Finish the activity when cancel button is clicked
            finish();
        }
    }
}
