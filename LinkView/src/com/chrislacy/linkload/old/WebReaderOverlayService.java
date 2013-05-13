package com.chrislacy.linkload.old;

/*
Copyright 2011 jawsware international

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import android.app.NotificationManager;
import android.net.Uri;
import com.chrislacy.linkload.HideActivity;
import com.chrislacy.linkload.R;
import com.jawsware.core.share.OverlayService;

import android.app.Notification;
import android.app.PendingIntent;

import android.content.Intent;

public class WebReaderOverlayService extends OverlayService {

	public static WebReaderOverlayService mInstance;

	private WebReaderOverlayView mOverlayView;

	@Override
	public void onCreate() {
		super.onCreate();
		
		mInstance = this;

		mOverlayView = new WebReaderOverlayView(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mOverlayView != null) {
			mOverlayView.destory();
		}

	}
	
	static public void stop() {
		if (mInstance != null) {
			mInstance.stopSelf();
		}
	}

    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    void handleCommand(Intent intent) {
        Uri data = intent.getData();
        mOverlayView.setUri(data);
    }
	
	@Override
	protected Notification foregroundNotification(int notificationId) {
		Notification notification;

		notification = new Notification(R.drawable.ic_action_link, getString(R.string.title_notification), System.currentTimeMillis());

		notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;

		notification.setLatestEventInfo(this, getString(R.string.title_notification), getString(R.string.message_notification), notificationIntent());

		return notification;
	}


	private PendingIntent notificationIntent() {
		Intent intent = new Intent(this, HideActivity.class);

		PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pending;
	}

    void cancelNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(id);
    }

}
