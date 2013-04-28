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

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public abstract class OverlayView extends RelativeLayout {

	protected WindowManager.LayoutParams layoutParams;

	private int layoutResId;
	private int notificationId = 0;

	public OverlayView(OverlayService service, int layoutResId, int notificationId) {
		super(service);

		this.layoutResId = layoutResId;
		this.notificationId = notificationId;

		this.setLongClickable(true);
		
		this.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				return onTouchEvent_LongPress();
			}
		});
		
		load();
	}

	public OverlayService getService() {
		return (OverlayService) getContext();
	}

	public int getLayoutGravity() {
		// Override this to set a custom Gravity for the view.

		return Gravity.CENTER;
	}

	private void setupLayoutParams() {
		layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

		layoutParams.gravity = getLayoutGravity();

		onSetupLayoutParams();

	}

	protected void onSetupLayoutParams() {
		// Override this to modify the initial LayoutParams. Be sure to call
		// super.setupLayoutParams() first.
	}

	private void inflateView() {
		// Inflates the layout resource, sets up the LayoutParams and adds the
		// View to the WindowManager service.

		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		inflater.inflate(layoutResId, this);

		onInflateView();

	}

	protected void onInflateView() {
		// Override this to make calls to findViewById() to setup references to
		// the views that were inflated.
		// This is called automatically when the object is created right after
		// the resource is inflated.
	}

	public boolean isVisible() {
		// Override this method to control when the Overlay is visible without
		// destroying it.
		return true;
	}

	public void refreshLayout() {
		// Call this to force the updating of the view's layout.

		if (isVisible()) {
			removeAllViews();
			inflateView();

			onSetupLayoutParams();

			((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(this, layoutParams);

			refresh();
		}

	}

	protected void addView() {
		setupLayoutParams();

		((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).addView(this, layoutParams);

		super.setVisibility(View.GONE);
	}

	protected void load() {
		inflateView();
		addView();
		refresh();
	}

	protected void unload() {
		((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).removeView(this);

		removeAllViews();
	}

	protected void reload() {
		unload();

		load();
	}

	public void destory() {
		((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).removeView(this);
	}

	public void refresh() {
		// Call this to update the contents of the Overlay.

		if (!isVisible()) {
			setVisibility(View.GONE);
		} else {
			setVisibility(View.VISIBLE);

			refreshViews();
		}
	}

	protected void refreshViews() {
		// Override this method to refresh the views inside of the Overlay. Only
		// called when Overlay is visible.
	}

	protected boolean showNotificationHidden() {
		// Override this to configure the notification to remain even when the
		// overlay is invisible.
		return true;
	}

	protected boolean onVisibilityToChange(int visibility) {
		// Catch changes to the Overlay's visibility in order to animate

		return true;
	}

	protected View animationView() {
		return this;
	}

	protected void hide() {
		// Set visibility, but bypass onVisibilityToChange()
		super.setVisibility(View.GONE);
	}

	protected void show() {
		// Set visibility, but bypass onVisibilityToChange()

		super.setVisibility(View.VISIBLE);
	}

	@Override
	public void setVisibility(int visibility) {
		if (visibility == View.VISIBLE) {
			getService().moveToForeground(notificationId, !showNotificationHidden());
		} else {
			getService().moveToBackground(notificationId, !showNotificationHidden());
		}

		if (getVisibility() != visibility) {
			if (onVisibilityToChange(visibility)) {
				super.setVisibility(visibility);
			}
		}
	}

	protected int getLeftOnScreen() {
		int[] location = new int[2];

		getLocationOnScreen(location);

		return location[0];
	}

	protected int getTopOnScreen() {
		int[] location = new int[2];

		getLocationOnScreen(location);

		return location[1];
	}

	protected boolean isInside(View view, int x, int y) {
		// Use this to test if the X, Y coordinates of the MotionEvent are
		// inside of the View specified.

		int[] location = new int[2];

		view.getLocationOnScreen(location);

		if (x >= location[0]) {
			if (x <= location[0] + view.getWidth()) {
				if (y >= location[1]) {
					if (y <= location[1] + view.getHeight()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	protected void onTouchEvent_Up(MotionEvent event) {

	}

	protected void onTouchEvent_Move(MotionEvent event) {

	}

	protected void onTouchEvent_Press(MotionEvent event) {

	}
	
	public boolean onTouchEvent_LongPress()
	{	
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		 if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {

			onTouchEvent_Press(event);

		} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {

			onTouchEvent_Up(event);

		} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

			onTouchEvent_Move(event);

		}

		return super.onTouchEvent(event);

	}

}
