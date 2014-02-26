package com.linkbubble.prokey;

import android.app.ApplicationErrorReport;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import com.linkbubble.util.Tamper;

public class ProMessengerService extends Service {
    /** Command to the service to display a message */

    static final int MSG_CHECK_LICENSE = 12345;
    static final int MSG_LICENSE_RESULT = 12346;
    
    static final int LICENSE_INVALID = -1;
    static final int LICENSE_UNKNOWN = 0;
    static final int LICENSE_VALID = 1;
    
    static final int LICENSE_REASON_RETRY = -100;

    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
	
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mReplyTo;
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                
                case MSG_CHECK_LICENSE:
                	//final Messenger replyTo = msg.replyTo;
                	mReplyTo = msg.replyTo;
                	startLicenseCheck();
                	/*
                	final Handler handler = new Handler();
                	handler.postDelayed(new Runnable() {
                	  @Override
                	  public void run() {
                		  if (replyTo != null) {
                			  try {
                				  replyTo.send(Message.obtain(null, MSG_LICENSE_RESULT, 3, 0));
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                		  }
                	  }
                	}, 500);
                	*/
                	break;
                	
                default:
                    super.handleMessage(msg);
            }
        }
    }

    
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {

        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
    	
        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        ServerManagedPolicy serverManagedPolicy = new ServerManagedPolicy(this,
                            new AESObfuscator(Constant.SALT, getPackageName(), deviceId));
        mChecker = new LicenseChecker(this, serverManagedPolicy, Constant.BASE64_PUBLIC_KEY);
        startLicenseCheck();
    	
        return mMessenger.getBinder();
    }
    
	
	@Override
	public boolean onUnbind(Intent intent) {
		mChecker.onDestroy();
        return super.onUnbind(intent);
    }
	
	
	private void startLicenseCheck() {
	    mChecker.checkAccess(mLicenseCheckerCallback);
	}

	int mRetryCount = 0;
	int mLicenseState = LICENSE_UNKNOWN;

	private static final int MAX_RETRY_COUNT = 5;
	
	void setLicenseState(int licenseState, Integer reason) {
        if (Tamper.isTweaked(this)) {
            licenseState = LICENSE_INVALID;
        }

		mLicenseState = licenseState;
		mRetryCount = 0;

        try {
            ComponentName componentName = new ComponentName(getPackageName(), MainActivity.class.getName());
            PackageManager packageManager = getApplicationContext().getPackageManager();
            if (licenseState == LICENSE_VALID) {
                if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            } else if (licenseState == LICENSE_INVALID) {
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception ex) {
            // Be sure to trap any and all potential issues with the above code.
            // It's not critical that it succeeds.
            Log.d("ActionLauncherPro", "Disabling component failed", ex);
            //Crittercism.logHandledException(ex);
        }

		if (mReplyTo != null) {
			try {
				mReplyTo.send(Message.obtain(null, MSG_LICENSE_RESULT, mLicenseState, reason == null ? 0 : reason));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/*
	 * 
	 */
	class MyLicenseCheckerCallback implements LicenseCheckerCallback {
	    public void allow(int policyReason) {
	    	setLicenseState(LICENSE_VALID, null);
	    }
	
	    public void dontAllow(int policyReason) {
	        
	        // Should not allow access. In most cases, the app should assume
	        // the user has access unless it encounters this. If it does,
	        // the app should inform the user of their unlicensed ways
	        // and then either shut down the app or limit the user to a
	        // restricted set of features.
	        // In this example, we show a dialog that takes the user to Market.
	        // If the reason for the lack of license is that the service is
	        // unavailable or there is another problem, we display a
	        // retry button on the dialog and a different message.
	        //displayDialog(policyReason == Policy.RETRY);
	    	
	    	if (policyReason == Policy.RETRY) {
	    		if (mRetryCount < MAX_RETRY_COUNT) {
	    			mRetryCount += 1;
	    			startLicenseCheck();
	    		} else {
	    			setLicenseState(LICENSE_INVALID, LICENSE_REASON_RETRY);
	    		}
	    	} else {
	    		setLicenseState(LICENSE_INVALID, null);
	    	}
	    	
	    }
	
	    public void applicationError(int errorCode) {
	    	setLicenseState(LICENSE_INVALID, null);
	        // This is a polite way of saying the developer made a mistake
	        // while setting up or calling the license checker library.
	        // Please examine the error code and fix the error.
	        //String result = String.format(getString(R.string.application_error), errorCode);
	        //displayResult(result);
	    }
	}
	
    
}