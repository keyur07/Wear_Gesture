package com.example.weargesture.Activity;

import static com.example.weargesture.Services.WearConnectService.lib;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.weargesture.R;
import com.example.weargesture.Services.WearConnectService;
import com.example.weargesture.Utills.NameFilter;
import com.example.weargesture.databinding.ActivityAppSelectorBinding;

import java.util.ArrayList;
import java.util.List;

public class AppSelector extends AppCompatActivity {

    // ArrayList to store package names
    ArrayList<String> packageName = new ArrayList<String>();

    // String to store the generated method name for return
    String methodNameForReturn;

    // Binding for the activity layout
    private ActivityAppSelectorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Load apps based on the specified method
            switch (getIntent().getStringExtra("method")) {
                case "wearapp":
                    loadWearApps();
                    break;
                case "timer":
                    loadTimers();
                    break;
                case "test":
                    loadTest();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Check if the list is empty and show a toast message if needed
            if (binding.listviewApp.getAdapter().getCount() <= 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.app_select_all_item), Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // Method to load Wear apps
    public void loadWearApps() {
        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);

        // Populate the list adapter with Wear apps
        for (int i = 0; i < WearConnectService.packNameList.length; i++) {
            listAdapter.add(WearConnectService.appNameList[i]);
            packageName.add(WearConnectService.packNameList[i]);
        }

        // Set the list adapter and item click listener
        binding.listviewApp.setAdapter(listAdapter);
        binding.listviewApp.setOnItemClickListener((parent, view, position, id) -> {
            String packName = packageName.get(position);
            generateMethod("wearapp", packName, binding.listviewApp.getItemAtPosition(position).toString());
        });
    }

    // Method to load timer-related apps
    public void loadTimers() {
        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);

        // Define timer methods and their indicators
        final String[] methods = {"Alarm", "Alarm List", "Timer", "Stopwatch"};
        final String[] methodsIndicator = {getString(R.string.timer_new_alarm), getString(R.string.timer_manage_alarms), getString(R.string.timer_open_timer), getString(R.string.timer_open_stopwatch)};

        final ArrayList<String> nonExistMethods = new ArrayList<>();

        // Add timer methods to the list adapter if they don't exist
        for (int i = 0; i < methods.length; i++) {
            if (!timerCheckExist(methods[i])) {
                listAdapter.add(methodsIndicator[i]);
                nonExistMethods.add(methods[i]);
            }
        }

        // Set the list adapter and item click listener
        binding.listviewApp.setAdapter(listAdapter);
        binding.listviewApp.setOnItemClickListener((parent, view, position, id) -> {
            String packName = nonExistMethods.get(position);
            generateMethod("timer", packName, binding.listviewApp.getItemAtPosition(position).toString());
        });
    }

    // Method to check if a timer method already exists
    public boolean timerCheckExist(String method) {
        for (String name : lib.getGestureEntries()) {
            NameFilter filter = new NameFilter(name);
            if (filter.getMethod().equals("timer") && filter.getPackName().equals(method)) {
                return true;
            }
        }
        return false;
    }

    // Method to load all installed apps for testing
    public void loadTest() {
        List<PackageInfo> shortcuts = getAllApps(getApplicationContext());
        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);

        // Populate the list adapter with all installed apps
        for (PackageInfo shortcut : shortcuts) {
            listAdapter.add(shortcut.packageName);
            packageName.add(shortcut.packageName);
        }

        // Set the list adapter and item click listener
        binding.listviewApp.setAdapter(listAdapter);
        binding.listviewApp.setOnItemClickListener((parent, view, position, id) -> {
            String packName = packageName.get(position);
            generateMethod("wearapp", packName, binding.listviewApp.getItemAtPosition(position).toString());
        });
    }

    // Method to get a list of all installed apps
    public static List<PackageInfo> getAllApps(Context context) {
        List<PackageInfo> apps = new ArrayList<>();
        PackageManager pManager = context.getPackageManager();
        List<PackageInfo> packlist = pManager.getInstalledPackages(0);

        for (PackageInfo pak : packlist) {
            apps.add(pak);
        }
        return apps;
    }

    // Method to generate the method and start the AddGesture activity
    public void generateMethod(String runType, String runMethod, String Label) {
        try {
            methodNameForReturn = Label + "##" + runType + "##" + runMethod;
            Intent addgesture = new Intent(this, AddGesture.class);
            addgesture.putExtra("method", methodNameForReturn);
            addgesture.putExtra("name", new NameFilter(methodNameForReturn).getFilteredName());
            startActivity(addgesture);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Fail to run " + runMethod + "\n Error message: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
