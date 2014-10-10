package com.linkbubble;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.linkbubble.util.CrashTracking;
import com.squareup.otto.Subscribe;

public class LicenseService extends Service {

    public static class CheckStateEvent {}

    private static final String COMMAND_KEY = "cmd";
    private static final String COMMAND_REGISTER = "register";
    private static final String COMMAND_UNREGISTER = "unregister";

    public static void register(Context context) {
        Intent serviceIntent = new Intent(context.getApplicationContext(), LicenseService.class);
        serviceIntent.putExtra(COMMAND_KEY, COMMAND_REGISTER);
        context.getApplicationContext().startService(serviceIntent);
    }

    public static void unregister(Context context) {
        Intent serviceIntent = new Intent(context.getApplicationContext(), LicenseService.class);
        serviceIntent.putExtra(COMMAND_KEY, COMMAND_UNREGISTER);
        context.getApplicationContext().startService(serviceIntent);
    }

    public DRM mDrm;
    private int sReferenceCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        MainApplication.registerForBus(this, this);

        mDrm = new DRM(this, MainApplication.sDrmSharedPreferences);
        mDrm.requestLicenseStatus();

        CrashTracking.log("LicenseService.onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String cmd = intent != null ? intent.getStringExtra(COMMAND_KEY) : null;
        CrashTracking.log("LicenseService.onStartCommand() cmd:" + cmd);

        if (cmd != null) {
            if (cmd.equals(COMMAND_REGISTER)) {
                sReferenceCount++;
                return START_STICKY;
            } else if (cmd.equals(COMMAND_UNREGISTER)) {
                sReferenceCount--;
                if (sReferenceCount > 0) {
                    return START_STICKY;
                }
                // go through to below and quit
            }
        }

        stopSelf();
        return START_NOT_STICKY;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mDrm.onDestroy();
        mDrm = null;

        MainApplication.unregisterForBus(this, this);

        CrashTracking.log("LicenseService.onDestroy()");

        super.onDestroy();
    }

    public void checkForProVersion() {
        if (DRM.isLicensed() == false) {
            if (mDrm != null && mDrm.mProServiceBound == false) {
                if (mDrm.bindProService()) {
                    mDrm.requestLicenseStatus();
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCheckStateEvent(CheckStateEvent event) {
        checkForProVersion();
    }

}
