package com.example.weargesture.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.weargesture.R;
import com.example.weargesture.databinding.ActivityAddActionBinding;
import com.google.android.gms.analytics.HitBuilders;

public class AddAction extends AppCompatActivity implements View.OnClickListener {

    // Binding for the layout
    private ActivityAddActionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using ViewBinding
        binding = ActivityAddActionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set click listeners for buttons
        binding.buttonApp.setOnClickListener(this);
        binding.buttonTimer.setOnClickListener(this);
        binding.buttonPhone.setOnClickListener(this);
    }

    // Destroy the activity and navigate to MainActivity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("extra", "notini");
        startActivity(intent);
    }

    // Click listener for buttons
    @Override
    public void onClick(View view) {
        // Handle button clicks based on the button ID
        if (view.getId() == binding.buttonApp.getId()) {
            // Start AppSelector activity for selecting a wearable app
            Intent intent = new Intent(getApplicationContext(), AppSelector.class);
            intent.putExtra("method", "wearapp");
            startActivity(intent);
        }

        if (view.getId() == binding.buttonTimer.getId()) {
            // Start AppSelector activity for selecting a timer
            Intent intent = new Intent(getApplicationContext(), AppSelector.class);
            intent.putExtra("method", "timer");
            startActivity(intent);
        }

        if (view.getId() == binding.buttonPhone.getId()) {
            // Display a toast indicating that phone selection is not available
            Toast.makeText(getApplicationContext(), "This is not available right now", Toast.LENGTH_SHORT).show();
        }
    }
}
