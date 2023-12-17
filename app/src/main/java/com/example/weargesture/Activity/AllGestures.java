package com.example.weargesture.Activity;

import static com.example.weargesture.Activity.MainActivity.wearConnect;
import static com.example.weargesture.Services.WearConnectService.lib;
import static com.example.weargesture.Services.WearConnectService.sendMobile;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;
import androidx.wear.widget.ConfirmationOverlay;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weargesture.Adapter.ImageAdapter;
import com.example.weargesture.R;
import com.example.weargesture.Utills.NameFilter;
import com.example.weargesture.databinding.ActivityAllGesturesBinding;

import java.util.ArrayList;
import java.util.Set;

public class AllGestures extends WearableActivity {

    // ArrayLists to store gesture titles, shortened titles, and bitmaps
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> shortenTitles = new ArrayList<>();
    ArrayList<Bitmap> bitmaps = new ArrayList<>();
    Boolean openMain = false; // Flag to check if MainActivity should be opened

    private ActivityAllGesturesBinding binding;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllGesturesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setAmbientEnabled();

        binding.container.setBackground(null);

        try {
            // Check if "open" extra is set to "y" to open MainActivity
            if ("y".equals(getIntent().getStringExtra("open"))) {
                openMain = true;
            }
        } catch (Exception ignored) {
        }

        // Refresh the list of gestures
        refreshList();
    }

    // Method to refresh the list of gestures
    public void refreshList() {
        titles.clear();
        bitmaps.clear();
        shortenTitles.clear();

        Set<String> gestureNameSet = lib.getGestureEntries();

        // Show a confirmation overlay if the gesture library is empty
        if (gestureNameSet.size() <= 0) {
            new ConfirmationOverlay().setDuration(99000)
                    .setMessage("Gesture library is empty, add a gesture now!")
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(AllGestures.this);
        }

        // Iterate over gesture entries and populate lists
        for (String gestureName : gestureNameSet) {
            ArrayList<Gesture> gesturesList = lib.getGestures(gestureName);
            NameFilter filter = new NameFilter(gestureName);

            for (Gesture gesture : gesturesList) {
                titles.add(gestureName);
                bitmaps.add(gesture.toBitmap(125, 125, 30, Color.YELLOW));
                shortenTitles.add(filter.getFilteredName());
            }
        }

        // Set up the ListView and ImageAdapter
        final ListView listView = findViewById(R.id.listview);
        final ImageAdapter adapter = new ImageAdapter(getApplicationContext(), shortenTitles, bitmaps);
        listView.setAdapter(adapter);

        // Set item click and long click listeners
        listView.setOnItemClickListener((parent, v, position, id) -> delete(position));

        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            delete(i);
            return true;
        });
    }

    // Method to show a confirmation dialog and delete a gesture
    public void delete(final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.dialog_delete_msg) + shortenTitles.get(position) + "' ?");
        builder.setPositiveButton(getString(R.string.delete), (dialog, id) -> {
            deleteItem(position);
            dialog.cancel();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.cancel());
        builder.show();
    }

    // Method to delete a gesture item
    public void deleteItem(int position) {
        lib.removeEntry(titles.get(position));
        lib.save();
        refreshList();
        sendMobile(wearConnect);

        // Show a confirmation overlay for successful deletion
        new ConfirmationOverlay()
                .setMessage(getString(R.string.main_deleted))
                .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                .showOn(AllGestures.this);
    }

    // onPause method to finish the activity when it goes into the background
    @Override
    protected void onPause() {
        super.onPause();
        if (!this.isFinishing()) {
            finish();
        }
    }

    // onDestroy method to open MainActivity if necessary
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openMain) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("extra", "notini");
            startActivity(intent);
        }
    }

    // Toast message utility method
    public void msg(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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

    // Method to update display based on ambient mode
    private void updateDisplay() {
        if (isAmbient()) {
            binding.container.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            binding.container.setBackground(null);
        }
    }
}
