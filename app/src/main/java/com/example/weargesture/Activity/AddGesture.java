package com.example.weargesture.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.gesture.Gesture;
import android.support.wearable.activity.WearableActivity;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.weargesture.R;
import com.example.weargesture.databinding.ActivityAddGestureBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AddGesture extends WearableActivity implements View.OnClickListener {

    // Date format for ambient mode display
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);

    // Variables to store method name and filtered name
    String methodNameForReturn;
    String filteredName;

    private ActivityAddGestureBinding binding;

    @SuppressLint({"MissingInflatedId", "StringFormatInvalid"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddGestureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setAmbientEnabled();

        // Retrieve method name and filtered name from intent
        methodNameForReturn = getIntent().getStringExtra("method");
        filteredName = getIntent().getStringExtra("name");

        // Set text in the layout indicating the gesture drawing action
        binding.textDraw.setText(String.format(getString(R.string.add_indicator_drawtoset), filteredName));

        // GestureOverlayView setup
        GestureOverlayView gesturer = findViewById(R.id.gestureDraw);
        gesturer.cancelClearAnimation();

        // Add a listener for the performed gesture
        gesturer.addOnGesturePerformedListener(new GestureOverlayView.OnGesturePerformedListener() {
            @Override
            public void onGesturePerformed(GestureOverlayView gestureOverlayView, Gesture gesture) {
                // Launch the confirmation activity with the performed gesture
                sendConfirm(gesture);
            }
        });

        // Set click listener for the back button
        binding.buttonBack.setOnClickListener(this);
    }

    // Method to launch the confirmation activity with the performed gesture
    public void sendConfirm(Gesture gesture) {
        Intent confirm = new Intent(this, AddConfirmGesture.class);
        confirm.putExtra("gesture", gesture);
        confirm.putExtra("method", methodNameForReturn);
        confirm.putExtra("name", filteredName);
        startActivity(confirm);
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

    // Click listener for the back button
    @Override
    public void onClick(View view) {
        if (view.getId() == binding.buttonBack.getId()) {
            finish();
        }
    }
}
