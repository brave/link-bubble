package com.google.android.hotword.client;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowId;

import com.google.android.hotword.service.IHotwordService;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class HotwordServiceClient {
	@SuppressWarnings("unused")
	private static final boolean DBG = false;
	private static final String HOTWORD_SERVICE = "com.google.android.googlequicksearchbox.HOTWORD_SERVICE";
	private static final String TAG = "HotwordServiceClient";
	private static final String VEL_PACKAGE = "com.google.android.googlequicksearchbox";

	private final Activity mActivity;
	private final ServiceConnection mConnection;
	private final WindowId.FocusObserver mFocusObserver;

	private IHotwordService mHotwordService;

	private boolean mHotwordStart;
	private boolean mIsAvailable = true;
	private boolean mIsBound;
	private boolean mIsFocused = false;
	private boolean mIsRequested = true;

	public HotwordServiceClient(Activity activity) {
		mActivity = activity;
		mConnection = new HotwordServiceConnection();
		mFocusObserver = new WindowFocusObserver();
	}

	private void assertMainThread() {
		if (Looper.getMainLooper().getThread() != Thread.currentThread())
			throw new IllegalStateException("Must be called on the main thread.");
	}

	private void internalBind() {
		if (!mIsAvailable || mIsBound) {
			if (!mIsAvailable)
				Log.w(TAG, "Hotword service is not available.");
			return;
		}

		Intent localIntent = new Intent(HOTWORD_SERVICE).setPackage(VEL_PACKAGE);
		mIsAvailable = mActivity.bindService(localIntent, mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = mIsAvailable;
	}

	private void internalRequestHotword() {
		if (mIsFocused && mIsRequested) {
			if (!mHotwordStart) {
				mHotwordStart = true;
				if (!mIsBound) {
					internalBind();
				}
			}
		}

		try {
			if (mHotwordService != null)
				mHotwordService.requestHotwordDetection(mActivity.getPackageName(), mIsFocused && mIsRequested);
		} catch (RemoteException e) {
			Log.w(TAG, "requestHotwordDetection - remote call failed", e);
			return;
		}
	}

	private boolean isPreKitKatDevice() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			Log.w(TAG, "Hotword service isn't usable on pre-Kitkat devices");
			return true;
		}
		return false;
	}

	public final void onAttachedToWindow() {
		if (isPreKitKatDevice())
			return;

		assertMainThread();
		mActivity.getWindow().getDecorView().getWindowId().registerFocusObserver(mFocusObserver);
		internalBind();
	}

	@SuppressLint("MissingSuperCall")
	public final void onDetachedFromWindow() {
		if (isPreKitKatDevice())
			return;

		if (!mIsBound) {
			return;
		}

		assertMainThread();
		mActivity.getWindow().getDecorView().getWindowId().unregisterFocusObserver(mFocusObserver);
		mActivity.unbindService(mConnection);
		mIsBound = false;
	}

	public final void requestHotwordDetection(boolean detect) {
		if (isPreKitKatDevice())
			return;

		assertMainThread();
		mIsRequested = detect;
		internalRequestHotword();
	}

	private class HotwordServiceConnection implements ServiceConnection {
		private HotwordServiceConnection() {}

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mHotwordService = IHotwordService.Stub.asInterface(iBinder);
			internalRequestHotword();
		}

		public void onServiceDisconnected(ComponentName componentName) {
			mIsBound = false;
			mHotwordService = null;
		}
	}

	private class WindowFocusObserver extends WindowId.FocusObserver {
		private WindowFocusObserver() {}

		public void onFocusGained(WindowId wid) {
			mIsFocused = true;
			internalRequestHotword();
		}

		public void onFocusLost(WindowId wid) {
			mIsFocused = false;
			internalRequestHotword();
		}
	}
}
