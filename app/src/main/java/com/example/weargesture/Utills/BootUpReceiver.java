package com.example.weargesture.Utills;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.weargesture.Activity.MainActivity;

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("extra","first");
        context.startActivity(i);
    }}
