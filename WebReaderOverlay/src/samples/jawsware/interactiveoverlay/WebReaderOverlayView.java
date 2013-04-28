package samples.jawsware.interactiveoverlay;

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

import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.jawsware.core.share.OverlayService;
import com.jawsware.core.share.OverlayView;

import java.net.URL;

public class WebReaderOverlayView extends OverlayView {

	//private TextView info;
    private WebView mWebView;

    private Uri mUri;
	
	public WebReaderOverlayView(OverlayService service) {
		super(service, R.layout.overlay, 1);
	}

	public int getGravity() {
		return Gravity.TOP + Gravity.RIGHT;
	}
	
	@Override
	protected void onInflateView() {
        mWebView = (WebView) findViewById(R.id.web_view);
        ImageView background = (ImageView)findViewById(R.id.background);
        background.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WebReaderOverlayService.stop();
            }
        });
	}

    public void setUri(Uri uri) {
        mUri = uri;

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(uri.toString());
        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                mWebView.setVisibility(View.VISIBLE);
            }
        });
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
