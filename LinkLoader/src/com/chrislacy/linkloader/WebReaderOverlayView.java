package com.chrislacy.linkloader;

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
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import com.jawsware.core.share.OverlayService;
import com.jawsware.core.share.OverlayView;

public class WebReaderOverlayView extends OverlayView {

    private View mLoadingView;
    private View mContentView;
    private WebView mWebView;

    enum LoadingState {
        NotSet,
        Loading,
        Loaded,
    }

    private LoadingState mLoadingState;
    private Uri mUri;
	
	public WebReaderOverlayView(OverlayService service) {
		super(service, R.layout.overlay, 1);
        mLoadingState = LoadingState.NotSet;
	}

	public int getGravity() {
		return Gravity.TOP + Gravity.RIGHT;
	}
	
	@Override
	protected void onInflateView() {
        mWebView = (WebView) findViewById(R.id.web_view);
        mContentView = findViewById(R.id.content);
        mLoadingView = findViewById(R.id.loading_content);

        ImageButton closeButton = (ImageButton) findViewById(R.id.close_buton);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WebReaderOverlayService.stop();
            }
        });
	}

    public void setUri(Uri uri) {
        mUri = uri;

        mLoadingState = LoadingState.Loading;

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(uri.toString());
        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {

                int delay = 3000;

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingState = LoadingState.Loaded;
                        mContentView.setVisibility(View.VISIBLE);
                        mLoadingView.setVisibility(View.INVISIBLE);

                        updateViewLayout();

                        WebReaderOverlayService.mInstance.cancelNotification();
                    }
                }, delay);
            }
        });
    }



    @Override
    protected void onSetupLayoutParams() {

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width;
        int height;
        if (mLoadingState == LoadingState.Loaded) {
            height = 800;
            width = size.x;
            mContentView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.INVISIBLE);

        } else {
            width = getResources().getDimensionPixelSize(R.dimen.loading_content_width);
            height = getResources().getDimensionPixelSize(R.dimen.loading_content_height);
        }

        layoutParams = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        layoutParams.gravity = getDefaultLayoutGravity();
    }

    /*
	@Override
	protected void refreshViews() {
		info.setText("WAITING\nWAITING");
	}

	@Override
	protected void onTouchEvent_Up(MotionEvent event) {
		info.setText("UP\nPOINTERS: " + event.getPointerCount());
	}

	@Override
	protected void onTouchEvent_Move(MotionEvent event) {
		info.setText("MOVE\nPOINTERS: " + event.getPointerCount());
	}

	@Override
	protected void onTouchEvent_Press(MotionEvent event) {
		info.setText("DOWN\nPOINTERS: " + event.getPointerCount());
	}

	@Override
	public boolean onTouchEvent_LongPress() {
		info.setText("LONG\nPRESS");

		return true;
	}
	*/
	
}
