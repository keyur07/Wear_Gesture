package com.example.weargesture.Services;

import static com.example.weargesture.Activity.MainActivity.apiCompatibleMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.weargesture.Activity.GesturePerformActivity;
import com.example.weargesture.R;
import com.example.weargesture.Utills.AnalyticsApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class FloaterService extends Service {

    // Frame layout for the floater
    public static FrameLayout frameLayoutfloater;
    // Google Analytics tracker
    public Tracker mTracker;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // Get the default tracker for Google Analytics
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        super.onCreate();

        // Start the floater service if not in API-compatible mode
        if (!apiCompatibleMode) {
            startFloater();
        }
    }

    // Start the floater service
    private void startFloater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            msg("To display the quick launcher, please enable the draw over permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            msg("Fail to open quick launcher: not allowed");
            createFloater();
        } else {
            createFloater();
        }
    }

    // Create the floater
    private void createFloater() {
        if (frameLayoutfloater == null) {
            newFloater();
        } else {
            msg("Floater already exits");
        }
    }

    // Get the window type based on the Android version
    private int getAlertType() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

    // Create a new floater
    private void newFloater() {
        SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
        String location = sharedPref.getString("location", "r");
        boolean small = sharedPref.getBoolean("small", false);
        boolean wider = sharedPref.getBoolean("wider", false);

        // Set window manager parameters
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getAlertType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        int default_width = 35;

        int LayoutID = R.layout.layout_quick_vertical;
        switch (location) {
            case "r":
                params.gravity = Gravity.CENTER | Gravity.RIGHT;
                LayoutID = R.layout.layout_quick_vertical;
                params.width = default_width;
                break;
            case "l":
                params.gravity = Gravity.CENTER | Gravity.LEFT;
                LayoutID = R.layout.layout_quick_vertical;
                params.width = default_width;
                break;
            case "t":
                params.gravity = Gravity.TOP | Gravity.CENTER;
                LayoutID = R.layout.layout_quick_horizon;
                params.height = default_width;
                break;
            case "b":
                params.gravity = Gravity.BOTTOM | Gravity.CENTER;
                LayoutID = R.layout.layout_quick_horizon;
                params.height = default_width;
                break;
            default:
                params.gravity = Gravity.CENTER | Gravity.RIGHT;
                break;
        }

        // Adjust width and height based on settings
        if (small) {
            if (location.equals("r") || location.equals("l")) {
                params.height = 180;
                params.gravity = params.gravity | Gravity.BOTTOM;
            } else {
                params.width = 130;
            }
        }

        if (wider) {
            if (location.equals("r") || location.equals("l")) {
                params.width = 50;
            } else {
                params.height = 50;
            }
        }

        final Context context = getApplicationContext();

        // Create a new frame layout
        frameLayoutfloater = new FrameLayout(context);

        // Get the window manager and add the frame layout
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(frameLayoutfloater, params);

        // Inflate the layout for the floater
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(LayoutID, frameLayoutfloater);

        try {
            // Set click and long-click listeners for the floater
            final ImageView sideOpen = (ImageView) frameLayoutfloater.findViewById(R.id.sideOpenLayout);
            sideOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Send an analytics event when the floater is clicked
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Floater")
                            .setAction("floaterClicked")
                            .setLabel("floater action")
                            .build());

                    // Open GesturePerformActivity when the floater is clicked
                    Intent intent = new Intent(context, GesturePerformActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        msg("Canceled");
                        e.printStackTrace();
                    }
                }
            });

            sideOpen.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // Stop the floater if long-press is enabled
                    SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
                    boolean longpress = sharedPref.getBoolean("longpress", true);

                    if (longpress) {
                        stopMe();
                        // Send an analytics event when the floater is long-pressed and stopped
                        mTracker.send(new HitBuilders.EventBuilder()
                                .setCategory("Floater")
                                .setAction("floaterLongpressedStopped")
                                .setLabel("floater action")
                                .build());
                    } else {
                        // Send an analytics event when the floater is long-pressed
                        mTracker.send(new HitBuilders.EventBuilder()
                                .setCategory("Floater")
                                .setAction("floaterLongpressed")
                                .setLabel("floater action")
                                .build());
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            // Show a message and retry if floater creation fails
            msg("Floater create failed, retrying...");
            stopMe();
        }

        // Fade out the floater after a certain duration
        new CountDownTimer(5000, 20) {
            public void onTick(long millisUntilFinished) {
                if (frameLayoutfloater != null) {
                    if (millisUntilFinished <= 2000) {
                        frameLayoutfloater.setAlpha((float) (millisUntilFinished / 20) / 100);
                    } else {
                        frameLayoutfloater.setAlpha((float) 1);
                    }
                }
            }

            public void onFinish() {
                if (frameLayoutfloater != null) {
                    frameLayoutfloater.setAlpha((float) 0);
                }
            }
        }.start();
    }

    // Stop the floater
    public void stopMe() {
        msg(getString(R.string.floater_disabled));
        // Remove all views from the frame layout and set it to null
        frameLayoutfloater.removeAllViews();
        frameLayoutfloater = null;
        // Stop the service
        stopSelf();
    }

    // Display a message as a toast and log it
    public void msg(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.v("fzg", message);
    }
}
