package com.chrislacy.linkview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import com.jawsware.core.share.OverlayService;
import com.jawsware.core.share.OverlayView;

/**
 * Created with IntelliJ IDEA.
 * User: chrislacy
 * Date: 5/1/2013
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ContentOverlayView extends OverlayView {

    static final int ANIM_TIME = 300;
    static final String TAG = "LinkView";

    private View mContentView;
    private Uri mUri;
    private boolean mCurrentUriLoaded;
    private String mCurrentUrl;
    private ContentWebView mWebView;
    private ObjectAnimator mAnimator;

    private enum ContentState {
        NotSet,
        Off,
        TurningOn,
        On,
        TurningOff,
    }
    private ContentState mContentState;

    public ContentOverlayView(OverlayService service) {
        super(service, R.layout.content, 1);
        mContentState = ContentState.NotSet;
    }

    public int getDefaultLayoutGravity() {
        return Gravity.BOTTOM + Gravity.LEFT;
    }

    @Override
    protected void onInflateView() {
        mWebView = (ContentWebView)findViewById(R.id.web_view);
        mWebView.setOnKeyDownListener(new ContentWebView.OnKeyDownListener() {

            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_BACK == keyCode) {
                    setContentState(ContentState.TurningOff);
                    return true;
                }
                return false;
            }
        });

        mContentView = findViewById(R.id.content);

        ImageView closeButton = (ImageView) findViewById(R.id.close_button);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkViewOverlayService.stop();
            }
        });

    }

    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);

    void animateOnscreen() {
        if (mContentState != ContentState.On && mContentState != ContentState.TurningOn) {
            setContentState(ContentState.TurningOn);
        } else {
            Log.d(TAG, "animateOnscreen() - ignore as already on screen");
        }
    }

    void animateOffscreen() {
        setContentState(ContentState.TurningOff);
    }

    private void setContentState(ContentState loadingState) {

        if (mContentState != loadingState) {
            Log.d(TAG, "setContentState() - from " + mContentState + " to " + loadingState);
            mContentState = loadingState;

            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;

            switch (mContentState) {
                case Off:
                    LinkViewOverlayService.stop();
                    break;

                case TurningOn:
                    mContentView.setVisibility(View.VISIBLE);

                    if (LinkViewOverlayService.mInstance != null) {
                        LinkViewOverlayService.mInstance.beginAppPolling(new LinkViewOverlayService.AppPollingListener() {
                            @Override
                            public void onAppChanged() {
                                animateOffscreen();
                            }
                        });
                    }

                    mAnimator = ObjectAnimator.ofFloat(mContentView, "x", width, 0);
                    mAnimator.setDuration(ANIM_TIME);
                    mAnimator.setInterpolator(DECELERATE_CUBIC);
                    mAnimator.addListener(new Animator.AnimatorListener() {
                        @Override public void onAnimationStart(Animator animation) {}
                        @Override public void onAnimationEnd(Animator animation) {
                            setContentState(ContentState.On);
                        }
                        @Override public void onAnimationCancel(Animator animation) {}
                        @Override public void onAnimationRepeat(Animator animation) {}
                    });
                    mAnimator.start();
                    break;

                case On:
                    if (LinkViewOverlayService.mInstance != null) {
                        LinkViewOverlayService.mInstance.hideLoading();
                    }
                    break;

                case TurningOff:
                    mAnimator = ObjectAnimator.ofFloat(mContentView, "x", width);
                    mAnimator.setDuration(ANIM_TIME);
                    mAnimator.setInterpolator(DECELERATE_CUBIC);
                    mAnimator.addListener(new Animator.AnimatorListener() {
                        @Override public void onAnimationStart(Animator animation) {}
                        @Override public void onAnimationEnd(Animator animation) {
                            setContentState(ContentState.Off);
                        }
                        @Override public void onAnimationCancel(Animator animation) {}
                        @Override public void onAnimationRepeat(Animator animation) {}
                    });
                    mAnimator.start();

                    if (LinkViewOverlayService.mInstance != null) {
                        LinkViewOverlayService.mInstance.endAppPolling();
                    }
                    break;
            }

            updateViewLayout();
        }
    }

    interface UriLoadedListener {
        void onPageFinished();
    }

    public void setUri(Uri uri, final UriLoadedListener listener) {
        if (mUri != null && uri.toString().equals(mUri.toString()) == true) {
            Log.d(TAG, "setUri() - early exit because the same - " + uri.toString());
            if (mCurrentUriLoaded) {
                listener.onPageFinished();
            }
            return;
        }

        mUri = uri;
        mCurrentUriLoaded = false;

        //mCurrentUrl = uri.toString();

        mWebView.stopLoading();
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(uri.toString());
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {

                //mWebView.stopLoading();
                mCurrentUriLoaded = true;
                listener.onPageFinished();

                //int delay = 5000;
//                final Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mContentState = ContentState.Loaded;
//                        mContentView.setVisibility(View.VISIBLE);
//                        mLoadingView.setVisibility(View.INVISIBLE);
//
//                        updateViewLayout();
//
//                        WebReaderOverlayService.mInstance.cancelNotification();
//                    }
//                }, 0);

            }

            public boolean shouldOverrideUrlLoading(WebView view, String url){

                /*
                PackageManager packageManager = getContext().getPackageManager();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                final ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
                if (resolveInfo != null && resolveInfo.activityInfo != null) {
                    String name = resolveInfo.activityInfo.name;
                    if (//!name.contains("com.android.internal")
                        //    && !name.contains("ResolverActivity")
                        //    && !name.contains("com.chrislacy.linkview")) {
                        !name.contains("com.chrislacy.linkview")) {
                        ComponentName componentName = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, name);
                        intent.setComponent(componentName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        LinkViewOverlayService.mInstance.getApplication().startActivity(intent);
                        LinkViewOverlayService.stop();
                        return true;
                    }
                    // TODO: Hard-code for YouTube, Instagram, Facebook and Twitter
                }*/

                view.loadUrl(url);
                return false; // then it is not handled by default action
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
        int height = (int) (size.y - Utilities.convertDpToPixel(24, getContext())) - 300;           // TODO: Come up with something better here
        int flags;
        if (isOnScreen()) {
            width = size.x;
            flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        } else {
            width = 0;
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        layoutParams = new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_PHONE, flags, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = getDefaultLayoutGravity();

    }

    boolean isOnScreen() {
        if (mContentState == ContentState.On || mContentState == ContentState.TurningOn || mContentState == ContentState.TurningOff) {
            return true;
        }

        return false;
    }
}
