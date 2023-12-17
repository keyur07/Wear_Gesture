package com.example.weargesture.Services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weargesture.Activity.MainActivity;
import com.example.weargesture.BuildConfig;
import com.example.weargesture.R;
import com.example.weargesture.Utills.AnalyticsApplication;
import com.example.weargesture.Utills.NameFilter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WearConnectService extends Service implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    Node mNode;
    private GoogleApiClient mGoogleApiClient;
    String path;
    String mobileReceived = "/receive";
    public static GestureLibrary lib;
    public static boolean alreadyCreated;
    public static final String TAG = "fzg";
    byte[] fileInBytes;
    public static String[] packNameList;
    public static String[] appNameList;
    public static int WEAR_VERSION;
    public static int MOBILE_VERSION;
    public static String locationAction = "/action";
    public static boolean showQuickLauncher;
    public static boolean vibratorOn;
    public static String location;
    public static int accuracy;
    public static boolean wait;
    public Tracker mTracker;

    public WearConnectService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        alreadyCreated = true;
        MainActivity.wearConnect = this;

        WEAR_VERSION = BuildConfig.VERSION_CODE;
        MOBILE_VERSION = 0;
        loadLibrary();

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    //---------------------------------------------------load lib
    public void loadLibrary() {
        final File mStoreFile = new File(getFilesDir(), "gestureNew");
        lib = GestureLibraries.fromFile(mStoreFile);

        getApplicationPacklist();

        if (!lib.load()) {
            firstInitiate();
        }

        loadPref();
    }

    //---------------------------------------------------load preference
    public void loadPref() {
        SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
        showQuickLauncher = sharedPref.getBoolean("show", true);
        vibratorOn = sharedPref.getBoolean("vibrate", true);
        location = sharedPref.getString("location", "r");
        accuracy = sharedPref.getInt("accuracy", 2);
        wait = sharedPref.getBoolean("wait", false);
    }

    //----------------------------------------------------------------------------------------------------------First time run
    public static void reload(WearConnectService connect) {
        connect.firstInitiate();
        connect.sendDataMapToDataLayerForMobile("/receive");
    }

    public void firstInitiate() {
        final File mStoreFile = new File(getFilesDir(), "gestureNew");

        if (!mStoreFile.exists()) {
            try {
                FileOutputStream stream = new FileOutputStream(mStoreFile);
                stream.write("".getBytes());
                stream.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }

        lib = GestureLibraries.fromFile(mStoreFile);
        GestureLibrary libInitial = GestureLibraries.fromRawResource(this, R.raw.gestureini);

        if (!libInitial.load()) {
            Toast.makeText(this, "Fatal error (Initial gesture not found). Please contact the developer.", Toast.LENGTH_LONG).show();
        }

        Set<String> gestureNameSet = libInitial.getGestureEntries();
        for (String gestureName : gestureNameSet) {
            ArrayList<Gesture> gesturesList = libInitial.getGestures(gestureName);
            NameFilter filter = new NameFilter(gestureName);

            for (Gesture gesture : gesturesList) {
                if (packExists(filter.getPackName())) {
                    String finalName;
                    PackageManager packageManager = getApplicationContext().getPackageManager();
                    try {
                        String appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(filter.getPackName(), PackageManager.GET_META_DATA));
                        finalName = filter.changeFilteredName(appName);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        finalName = gestureName;
                    }
                    lib.addGesture(finalName, gesture);
                }
            }
        }

        if (lib.save()) {
            msg("Gesture Library initiated!");
        } else {
            msg("Error: fail to save gesture library");
        }

        if (!lib.load()) {
            msg("No gestures found, failsafe gesture is added.");
            lib.addGesture("Test", libInitial.getGestures("WearTest##wearapp##com.format.weartest").get(0));
            lib.save();
        }
    }

    public boolean packExists(String packageName) {
        for (String packNameWear : packNameList) {
            if (packNameWear.equals(packageName)) {
                for (String packNameFile : lib.getGestureEntries()) {
                    if (packNameFile.equals(packageName)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        resolveNode();
    }

    public void byteToFile() {
        String strFilePath = getFilesDir() + "/gestureNew";
        try {
            FileOutputStream fos = new FileOutputStream(strFilePath);
            //String strContent = "Write File using Java ";

            fos.write(fileInBytes);
            fos.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException : " + ex);
        } catch (IOException ioe) {
            System.out.println("IOException : " + ioe);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    private void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for (Node node : nodes.getNodes()) {
                    Log.v(TAG, node.toString());
                    mNode = node;
                }
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.createFromDataMapItem(DataMapItem.fromDataItem(event.getDataItem()));

            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                path = item.getUri().getPath();

                if (path.equals("/gestures")) {
                    DataMap map = putDataMapRequest.getDataMap();

                    MOBILE_VERSION = map.getInt("version");

                    if (MOBILE_VERSION != WEAR_VERSION - 1000) {
                        if (map.getInt("version") < 1012) {
                            Toast t = Toast.makeText(this, "\n\n\nWarning: Gestures overwrote from phone, Mobile app version too old, please update the app on your phone.", Toast.LENGTH_LONG);
                            t.setGravity(Gravity.FILL_HORIZONTAL | Gravity.FILL_VERTICAL, 0, 0);
                            t.show();
                        } else {
                            Toast t = Toast.makeText(this, String.format(getString(R.string.version_warn), MOBILE_VERSION, WEAR_VERSION), Toast.LENGTH_LONG);
                            t.setGravity(Gravity.FILL_HORIZONTAL | Gravity.FILL_VERTICAL, 0, 0);
                            t.show();
                        }
                    }

                    if (map.getBoolean("overwrite") || map.getInt("version") < 1012) {
                        fileInBytes = map.getByteArray("File");
                        byteToFile();
                        loadLibrary();
                        msg(getString(R.string.lib_synced));
                    }

                    boolean safe = false;
                    try {
                        if (map.getString("location").length() > 0) {
                            safe = true;
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        msg("No pref received");
                    }

                    if (safe) {
                        SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("show", map.getBoolean("show"));
                        editor.putBoolean("vibrate", map.getBoolean("vibrate"));
                        editor.putString("location", map.getString("location"));
                        editor.putInt("accuracy", map.getInt("accuracy"));
                        editor.apply();

                        if (FloaterService.frameLayoutfloater != null) {
                            FloaterService.frameLayoutfloater.removeAllViews();
                            FloaterService.frameLayoutfloater = null;
                            stopService(new Intent(WearConnectService.this, FloaterService.class));

                            if (sharedPref.getBoolean("show", true)) {
                                startService(new Intent(WearConnectService.this, FloaterService.class));
                            }
                        } else {
                            if (sharedPref.getBoolean("show", true)) {
                                startService(new Intent(WearConnectService.this, FloaterService.class));
                            }
                        }
                    }

                    loadPref();
                    sendDataMapToDataLayerForMobile("/receive");
                } else if (path.equals("/initiate")) {
                    sendDataMapToDataLayerForMobile("/receive");
                    msg(getString(R.string.connection_ini));
                } else if (path.equals("/needupdate")) {
                    sendDataMapToDataLayerForMobile("/update");
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public static void sendMobileAction(WearConnectService connect, String action) {
        connect.resolveNode();
        connect.sendMobileMessage(action);
    }

    private void sendMobileMessage(final String action) {
        byte[] bytes = action.getBytes(Charset.forName("UTF-8"));

        if (mNode != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mNode.getId(), "/sync", bytes).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("TAG", "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                                msg(getString(R.string.connect_fail_code) + sendMessageResult.getStatus().getStatusMessage());
                                sendMobileActionDatamap(action);
                                mTracker.send(new HitBuilders.EventBuilder().setCategory("MobileConnection").setAction("failToSendAction").setLabel(sendMessageResult.getStatus().getStatusMessage()).build());
                            } else {
                                msg(getString(R.string.connect_action_sent));
                                mTracker.send(new HitBuilders.EventBuilder().setCategory("MobileConnection").setAction("actionSent").setLabel(action).build());
                            }
                        }
                    }
            );
        } else {
            msg(getString(R.string.connect_connection_failed));
            sendMobileActionDatamap(action);
            mTracker.send(new HitBuilders.EventBuilder().setCategory("MobileConnection").setAction("failToSendAction").setLabel("Connection failed").build());
        }
    }

    public static void sendMobile(WearConnectService connect) {
        connect.sendDataMapToDataLayerForMobile("/update");
    }

    private void getApplicationPacklist() {
        ArrayList<String> packageName = new ArrayList<>();
        ArrayList<String> appName = new ArrayList<>();
        final PackageManager pm = getPackageManager();
        final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));

        for (ApplicationInfo packageInfo : packages) {
            if (checkForLaunchIntent(packageInfo) && !checkForAlreadyExist(packageInfo)) {
                packageName.add(packageInfo.packageName);
                appName.add(pm.getApplicationLabel(packageInfo).toString());
            }
        }

        packNameList = packageName.toArray(new String[packageName.size()]);
        appNameList = appName.toArray(new String[appName.size()]);
    }

    private boolean checkForLaunchIntent(ApplicationInfo info) {
        try {
            if (null != getPackageManager().getLaunchIntentForPackage(info.packageName)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean checkForAlreadyExist(ApplicationInfo info) {
        if (!lib.load()) {
            return false;
        }

        for (String gestureName : lib.getGestureEntries()) {
            if (new NameFilter(gestureName).getPackName().equals(info.packageName)) {
                return true;
            }
        }
        return false;
    }

    public byte[] file2byte() {
        File file = new File(getFilesDir(), "gestureNew");

        byte[] b = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(b);
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found.");
            e.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Error Reading The File.");
            e1.printStackTrace();
        }
        return b;
    }

    public void sendDataMapToDataLayerForMobile(String location) {
        mobileReceived = location;
        if (mGoogleApiClient.isConnected()) {
            DataMap dataMap = createSyncDatamap();
            new SendDataMapToDataLayer(mobileReceived, dataMap).start();
        }
    }

    private DataMap createSyncDatamap() {
        DataMap dataMap = new DataMap();
        dataMap.putString("Received!", "From wear");
        dataMap.putString("Time", Long.toString(System.currentTimeMillis()));

        getApplicationPacklist();
        dataMap.putStringArray("packList", packNameList);
        dataMap.putStringArray("appList", appNameList);
        dataMap.putByteArray("updatedlib", file2byte());
        dataMap.putInt("version", BuildConfig.VERSION_CODE);


        //---------------------------------------------------------------Sync SharedPreference
        SharedPreferences sharedPref = getSharedPreferences("main", MODE_PRIVATE);
        boolean showq = sharedPref.getBoolean("show", true);
        boolean vib = sharedPref.getBoolean("vibrate", true);
        String loca = sharedPref.getString("location", "r");
        int accuracy = sharedPref.getInt("accuracy", 2);

        dataMap.putBoolean("show", showq);
        dataMap.putBoolean("vibrate", vib);
        dataMap.putString("location", loca);
        dataMap.putInt("accuracy", accuracy);
        //--------------------------------------------------


        return dataMap;
    }

    private void sendMobileActionDatamap(String action) {
        DataMap dataMap = new DataMap();
        dataMap.putString("action", action);
        dataMap.putString("Time", Long.toString(System.currentTimeMillis()));


        mobileReceived = locationAction;
        if (mGoogleApiClient.isConnected()) {
            new SendDataMapToDataLayer(mobileReceived, dataMap).start();
        }
    }

    private class SendDataMapToDataLayer extends Thread {
        String path;
        DataMap dataMap;

        public SendDataMapToDataLayer(String path, DataMap dataMap) {
            this.path = path;
            this.dataMap = dataMap;
        }

        public void run() {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(mobileReceived);
            putDataMapReq.getDataMap().putAll(dataMap);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        }


    }

    public void msg(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        Log.v(TAG, message);
    }
}
