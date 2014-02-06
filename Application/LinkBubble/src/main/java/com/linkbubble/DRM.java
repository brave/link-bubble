package com.linkbubble;

import android.app.Application;
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
import com.squareup.otto.Subscribe;

import java.util.List;


public class DRM {

    static private String TAG = "LinkBubbleDRM";
    static private boolean DEBUG = false;

    static final int MSG_LICENSE_RESULT = 12346;
    static final int MSG_CHECK_LICENSE = 12345;

    static final int LICENSE_INVALID = -1;
    static final int LICENSE_UNKNOWN = 0;
    static final int LICENSE_VALID = 1;
    static final int LICENSE_IGNORE_ERROR = 99;

    static private int sLicenseState = DRM.LICENSE_UNKNOWN;

    public static boolean isLicensed() {
        return sLicenseState == DRM.LICENSE_VALID;
    }

    private static String LICENSE_KEY = "alp_licenseKey2";
    private static String FIRST_INSTALL_TIME_KEY = "alp_firstInstallTime2";
    private static String USAGE_TIME_LEFT_KEY = "alp_usageTimeLeft2";

    private long mUsageTimeLeft = 0;
    private long mFirstInstallTime = 0;
    private SharedPreferences mSharedPreferences;

    public interface Listener {
        void onDrmInvalid();
    };

    public static class SetLicenseStateEvent {
        public int mState;

        public SetLicenseStateEvent(int state) {
            mState = state;
        }
    }

    Listener mListener;
    Context mContext;

    DRM(Context context) {
        mContext = context;
        Constant.DEVICE_ID = Constant.getSecureAndroidId(context);
        mSharedPreferences = context.getSharedPreferences(Constant.DRM_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        if (mSharedPreferences != null) {
            ServiceInfo serviceInfo = getProServiceInfo(context);
            sLicenseState = serviceInfo != null ? decryptSharedPreferencesInt(mSharedPreferences, LICENSE_KEY, LICENSE_UNKNOWN) : LICENSE_INVALID;
        }
        MainApplication.registerForBus(context, this);
    }

    void initialize(Application application, Listener listener) {
        mListener = listener;

        if (mProServiceBound == false) {
            boolean serviceBound = bindProService(application);

            // First time only
            sLicenseState = serviceBound ? decryptSharedPreferencesInt(mSharedPreferences, LICENSE_KEY, LICENSE_UNKNOWN) : LICENSE_INVALID;
            mFirstInstallTime = decryptSharedPreferencesLong(mSharedPreferences, FIRST_INSTALL_TIME_KEY, 0);
            mUsageTimeLeft = decryptSharedPreferencesLong(mSharedPreferences, USAGE_TIME_LEFT_KEY, 0);

            // DRM: Save the time the app was first installed
            if (mFirstInstallTime == 0) {
                mFirstInstallTime = System.currentTimeMillis();
                final String encryptedFirstInstallTime = encryptLong(mFirstInstallTime);
                new Thread("setLicenseState") {
                    public void run() {
                        mSharedPreferences.edit()
                                .putString(FIRST_INSTALL_TIME_KEY, encryptedFirstInstallTime)
                                .commit();
                    }
                }.start();
            }
        }
    }

    private ServiceInfo getProServiceInfo(Context context) {
        final Intent mainIntent = new Intent(Constant.PRO_DRM_SERVICE_ACTION, null);
        mainIntent.setPackage(Constant.PRO_LAUNCHER_PACKAGE_NAME);
        List<ResolveInfo> services = context.getPackageManager().queryIntentServices(mainIntent, 0);
        if (services != null && services.size() > 0) {
            return services.get(0).serviceInfo;
        }
        return null;
    }

    private String getValuePrefix() {
        return Constant.DEVICE_ID + "->:";
    }

    int decryptSharedPreferencesInt(SharedPreferences sharedPreferences, String key, int defaultValue) {
        String encryptedValue = sharedPreferences.getString(key, null);
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

    long decryptSharedPreferencesLong(SharedPreferences sharedPreferences, String key, long defaultValue) {
        String encryptedValue = sharedPreferences.getString(key, null);
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

    void setListener(Listener listener) {
        mListener = listener;
    }

    void onDestroy() {
        MainApplication.unregisterForBus(mContext, this);
    }

    public boolean bindProService(Context context) {
        boolean serviceBound = false;
        ServiceInfo serviceInfo = getProServiceInfo(context);
        if (serviceInfo != null) {
            Intent intent = new Intent();
            intent.setClassName(serviceInfo.packageName, serviceInfo.name);
            serviceBound = context.bindService(intent, mProConnection, Context.BIND_AUTO_CREATE);
        }
        return serviceBound;
    }

    public void requestLicenseStatus() {
        if (!mProServiceBound)
            return;

        Message msg = Message.obtain(null, MSG_CHECK_LICENSE);
        msg.replyTo = mProMessenger;
        try {
            mProService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSetLicenseState(SetLicenseStateEvent event) {
        Log.d(TAG, "onSetLicenseState CALLED!!!");
        setLicenseState(event.mState);
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
            if (sLicenseState != licenseState && MainController.get() != null) {
                MainController.get().onStateChange(licenseState == LICENSE_VALID);
            }

            sLicenseState = licenseState;
            mUsageTimeLeft = usageTimeLeft;

            if (mSharedPreferences != null) {
                new SetStateThread(mSharedPreferences, encryptInt(licenseState), encryptLong(usageTimeLeft)).start();
                Log.d(TAG, "call SetStateThread() - licenseState:" + licenseState);
            }
        }

        if (licenseState == LICENSE_INVALID) {
            Log.d(TAG, "LICENSE_INVALID");
            if (mListener != null) {
                mListener.onDrmInvalid();
            }
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

    /*
    private static class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        public MyLicenseCheckerCallback() {
        }

        public void allow(int policyReason) {

            //if (mApplication.mLauncher == null) {
            //    return;
            //}

            //if (mApplication.mLauncher.isFinishing()) {
                // Don't update UI if Activity is finishing.
            //    return;
            //}

            // Should allow user access.
            //displayResult(getString(R.string.allow));
            //mApplication.mLauncher.setLicenseState(LICENSE_VALID, LICENSE_REASON_NONE);
            if (DEBUG) {
                Log.d(TAG, "MyLicenseCheckerCallback.allow()");
            }
            LauncherBusProvider.getInstance().post(new ALEventSetLicenseState(LICENSE_VALID, LICENSE_REASON_NONE));
        }

        public void dontAllow(int policyReason) {
            //if (mApplication.mLauncher == null) {
            //    return;
            //}

            //if (mApplication.mLauncher.isFinishing()) {
                // Don't update UI if Activity is finishing.
            //    return;
            //}
            //displayResult(getString(R.string.dont_allow));
            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            // If the reason for the lack of license is that the service is
            // unavailable or there is another problem, we display a
            // retry button on the dialog and a different message.
            //displayDialog();
            //mApplication.mLauncher.setLicenseState(LICENSE_INVALID, policyReason == Policy.RETRY ? LICENSE_REASON_RETRY : LICENSE_REASON_NONE);

            boolean ignoreError = false;
            switch (policyReason) {

                case Policy.RETRY:
                    ignoreError = true;
                    break;

                //case LicenseValidator.ERROR_CONTACTING_SERVER:
                //case LicenseValidator.ERROR_SERVER_FAILURE:
                //case LicenseValidator.ERROR_OVER_QUOTA:
                //    ignoreError = true;
                //    break;
            }

            if (ignoreError) {
                LauncherBusProvider.getInstance().post(new ALEventSetLicenseState(LICENSE_IGNORE_ERROR, LICENSE_REASON_NONE));
            } else {
                if (DEBUG) {
                    Log.d(TAG, "MyLicenseCheckerCallback.dontAllow()");
                }
                LauncherBusProvider.getInstance().post(new ALEventSetLicenseState(LICENSE_INVALID,
                        policyReason == Policy.RETRY ? LICENSE_REASON_RETRY : LICENSE_REASON_NONE));
            }
        }

        public void applicationError(int errorCode) {
            //if (mApplication.mLauncher == null) {
            //    return;
            //}
            if (DEBUG) {
                Log.d(TAG, "MyLicenseCheckerCallback.applicationError()");
            }
            LauncherBusProvider.getInstance().post(new ALEventSetLicenseState(LICENSE_INVALID, LICENSE_REASON_NONE));
            //mApplication.mLauncher.setLicenseState(LICENSE_INVALID, LICENSE_REASON_NONE);

        }
    }*/


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
                            MainApplication.postEvent(mContext, new SetLicenseStateEvent(msg.arg1));
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
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mProService = null;
            mProServiceBound = false;
        }
    };
}
