package com.linkbubble;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.linkbubble.util.Encrypt;

import java.util.List;


public class DRM {

    static public String TAG = "LinkBubbleDRM";
    static private boolean DEBUG = true;

    static final int MSG_LICENSE_RESULT = 12346;
    static final int MSG_CHECK_LICENSE = 12345;

    static final int LICENSE_INVALID = -1;
    static final int LICENSE_UNKNOWN = 0;
    public static final int LICENSE_VALID = 1;
    static final int LICENSE_IGNORE_ERROR = 99;

    static private int sLicenseState = DRM.LICENSE_UNKNOWN;

    public static boolean isLicensed() {
        return sLicenseState == DRM.LICENSE_VALID;
    }

    public static boolean allowProFeatures() {
        return isLicensed() || MainApplication.isInTrialPeriod();
    }

    private static String LICENSE_KEY = "lb_licenseKey";
    private static String FIRST_INSTALL_TIME_KEY = "lb_firstInstallTime";
    private static String USAGE_TIME_LEFT_KEY = "lb_usageTimeLeft";

    private long mUsageTimeLeft = 0;
    private long mFirstInstallTime = 0;
    private SharedPreferences mSharedPreferences;

    private Context mContext;
    private MainApplication.StateChangedEvent mStateChangedEvent = new MainApplication.StateChangedEvent();

    DRM(Context context) {
        Log.d(TAG, "DRM() init");
        mContext = context;
        Constant.DEVICE_ID = Constant.getSecureAndroidId(context);
        mSharedPreferences = context.getSharedPreferences(Constant.DRM_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        if (mSharedPreferences != null) {
            ServiceInfo serviceInfo = getProServiceInfo(context);
            sLicenseState = serviceInfo != null ? decryptSharedPreferencesInt(LICENSE_KEY, LICENSE_UNKNOWN) : LICENSE_INVALID;
        }
        MainApplication.registerForBus(context, this);

        boolean serviceBound = bindProService(context);
        sLicenseState = serviceBound ? decryptSharedPreferencesInt(LICENSE_KEY, LICENSE_UNKNOWN) : LICENSE_INVALID;
        mFirstInstallTime = decryptSharedPreferencesLong(FIRST_INSTALL_TIME_KEY, 0);
        mUsageTimeLeft = decryptSharedPreferencesLong(USAGE_TIME_LEFT_KEY, 0);

        // DRM: Save the time the app was first installed
        if (mFirstInstallTime == 0) {
            mFirstInstallTime = System.currentTimeMillis();
            String encryptedFirstInstallTime = encryptLong(mFirstInstallTime);
            saveToPreferences(FIRST_INSTALL_TIME_KEY, encryptedFirstInstallTime);
        }
    }

    private ServiceInfo getProServiceInfo(Context context) {
        final Intent mainIntent = new Intent(Constant.PRO_DRM_SERVICE_ACTION, null);
        mainIntent.setPackage(BuildConfig.PRO_PACKAGE_NAME);
        List<ResolveInfo> services = context.getPackageManager().queryIntentServices(mainIntent, 0);
        if (services != null && services.size() > 0) {
            Log.d(TAG, "getProServiceInfo() - " + services.get(0).serviceInfo.name);
            return services.get(0).serviceInfo;
        }
        Log.d(TAG, "getProServiceInfo() - return null");
        return null;
    }

    private String getValuePrefix() {
        return Constant.DEVICE_ID + "->:";
    }

    int decryptSharedPreferencesInt(String key, int defaultValue) {
        String encryptedValue = mSharedPreferences.getString(key, null);
        if (encryptedValue != null) {
            String decryptedValue = Encrypt.decryptIt(encryptedValue);
            if (decryptedValue.equals(encryptedValue) == false) {
                String valuePrefix = getValuePrefix();
                if (decryptedValue.contains(valuePrefix)) {
                    String suffix = decryptedValue.replace(valuePrefix, "");
                    try {
                        return Integer.parseInt(suffix);
                    } catch (NumberFormatException ex) {
                    }
                }
            }
        }

        return defaultValue;
    }

    String encryptInt(int value) {
        return Encrypt.encryptIt(getValuePrefix() + value);
    }

    long decryptSharedPreferencesLong(String key, long defaultValue) {
        String encryptedValue = mSharedPreferences.getString(key, null);
        if (encryptedValue != null) {
            String decryptedValue = Encrypt.decryptIt(encryptedValue);
            if (decryptedValue.equals(encryptedValue) == false) {
                String valuePrefix = getValuePrefix();
                if (decryptedValue.contains(valuePrefix)) {
                    String suffix = decryptedValue.replace(valuePrefix, "");
                    try {
                        return Long.parseLong(suffix);
                    } catch (NumberFormatException ex) {
                    }
                }
            }
        }

        return defaultValue;
    }

    String encryptLong(long value) {
        return Encrypt.encryptIt(getValuePrefix() + value);
    }

    void saveToPreferences(final String key, final String value) {
        new Thread("saveToPreferences") {
            public void run() {
                mSharedPreferences.edit()
                        .putString(key, value)
                        .commit();
            }
        }.start();
    }

    void onDestroy() {
        Log.d(TAG, "onDestroy()");
        MainApplication.unregisterForBus(mContext, this);
    }

    public boolean bindProService(Context context) {
        boolean serviceBound = false;
        ServiceInfo serviceInfo = getProServiceInfo(context);
        if (serviceInfo != null) {
            Intent intent = new Intent();
            intent.setClassName(serviceInfo.packageName, serviceInfo.name);
            serviceBound = context.bindService(intent, mProConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "bindProService() - " + "try bind: serviceBound=" + serviceBound);
        }
        Log.d(TAG, "bindProService() - serviceBound:" + serviceBound);
        return serviceBound;
    }

    public void requestLicenseStatus() {
        if (!mProServiceBound)
            return;

        Log.d(TAG, "requestLicenseStatus()");
        Message msg = Message.obtain(null, MSG_CHECK_LICENSE);
        msg.replyTo = mProMessenger;
        try {
            mProService.send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "requestLicenseStatus()", e);
        }
    }

    private void setLicenseState(int licenseState) {

        //long lastValidInstallTime = mLastValidInstallTime;
        long usageTimeLeft = mUsageTimeLeft;

        if (mFirstInstallTime != 0) {
            long currentTime = System.currentTimeMillis();
            long firstTimeInstallTimeDelta = currentTime - mFirstInstallTime;

            //if (firstTimeInstallTimeDelta > 30 * 1000) {			// TEMP
            if (firstTimeInstallTimeDelta > 20 * 60 * 1000) {
                if (licenseState == LICENSE_VALID) {
                    usageTimeLeft = 24 * 60 * 60 * 1000;
                } else if (licenseState == LICENSE_IGNORE_ERROR) {      // If there was an error reaching the server, give another hour
                    usageTimeLeft = 1 * 60 * 60 * 1000;
                }
            }

            if (licenseState != LICENSE_VALID) {
                if (usageTimeLeft > 0) {
                    licenseState = LICENSE_VALID;
                    Log.d(TAG, "Invalid, in safe period");
                }
            }
        }

        if (sLicenseState != licenseState || usageTimeLeft != mUsageTimeLeft) {

            mStateChangedEvent.mState = licenseState;
            mStateChangedEvent.mOldState = sLicenseState;

            sLicenseState = licenseState;
            mUsageTimeLeft = usageTimeLeft;

            if (mSharedPreferences != null) {
                new SetStateThread(mSharedPreferences, encryptInt(licenseState), encryptLong(usageTimeLeft)).start();
                Log.d(TAG, "call SetStateThread() - licenseState:" + licenseState);
            }

            MainApplication.postEvent(mContext, mStateChangedEvent);
        }
    }

    static final class SetStateThread extends Thread {

        SharedPreferences mSharedPreferences;
        String mEncryptedState;
        String mEncryptedLeft;

        SetStateThread(final SharedPreferences sharedPreferences, String encryptedState, String encryptedLeft) {
            super("setStateThread");
            mSharedPreferences = sharedPreferences;
            mEncryptedState = encryptedState;
            mEncryptedLeft = encryptedLeft;
        }

        @Override
        public void run() {

            mSharedPreferences.edit()
                    .putString(LICENSE_KEY, mEncryptedState)
                    .putString(USAGE_TIME_LEFT_KEY, mEncryptedLeft)
                    .commit();
            Log.d(TAG, "Save license state: " + mEncryptedState);
        }
    }

    /**
     * Handler of incoming messages from service.
     */
    class ProHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LICENSE_RESULT:
                    switch (msg.arg1) {
                        case LICENSE_INVALID:
                        case LICENSE_UNKNOWN:
                        case LICENSE_VALID:
                            setLicenseState(msg.arg1);
                            break;
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mProMessenger = new Messenger(new ProHandler());

    /** Messenger for communicating with the service. */
    Messenger mProService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mProServiceBound;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mProConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mProService = new Messenger(service);
            mProServiceBound = true;
            requestLicenseStatus();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected()");
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mProService = null;
            mProServiceBound = false;
        }
    };


}
