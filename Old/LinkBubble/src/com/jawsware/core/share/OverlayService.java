package com.jawsware.core.share;

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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OverlayService extends Service {

	protected boolean foreground = false;
	protected boolean cancelNotification = false;
	protected int id = 0;
	
	protected Notification foregroundNotification(int notificationId) {
		return null;
	}

	public void moveToForeground(int id, boolean cancelNotification) {
		moveToForeground(id, foregroundNotification(id), cancelNotification);
	}

	public void moveToForeground(int id, Notification notification, boolean cancelNotification) {
		if (! this.foreground && notification != null) {
			this.foreground = true;
			this.id = id;
			this.cancelNotification = cancelNotification;
					
			super.startForeground(id, notification);
		} else if (this.id != id && id > 0 && notification != null) {
			this.id = id;
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(id, notification);
		}
	}
	

	public void moveToBackground(int id, boolean cancelNotification) {
		foreground = false;
		id = 0;
		super.stopForeground(cancelNotification);
	}
	
	public void moveToBackground(int id) {
		moveToBackground(id, cancelNotification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
