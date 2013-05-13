package com.chrislacy.linkload;

import android.animation.ObjectAnimator;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
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
import android.widget.ProgressBar;
import com.jawsware.core.share.OverlayService;
import com.jawsware.core.share.OverlayView;

/**
 * Created with IntelliJ IDEA.
 * User: chrislacy
 * Date: 5/1/2013
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoadingOverlayView extends OverlayView {

    private View mLoadingView;
    private LinkViewOverlayService mService;

    enum LoadingState {
        NotSet,
        Loading,
        Loaded,
    }

    private LoadingState mLoadingState;

    public LoadingOverlayView(OverlayService service) {
        super(service, R.layout.loading, 1);
        mLoadingState = LoadingState.NotSet;
        mService = (LinkViewOverlayService) service;
    }

    public int getGravity() {
        return Gravity.TOP + Gravity.RIGHT;
    }

    public int getDefaultLayoutGravity() {
        return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
    }

    @Override
    protected void onInflateView() {
        mLoadingView = findViewById(R.id.loading_content);

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        progressBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setLoadingState(LoadingState.Loaded);
            }
        });
    }

    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);

    void setLoadingState(LoadingState loadingState) {

        if (mLoadingState != loadingState) {
            mLoadingState = loadingState;

            switch (mLoadingState) {
                case Loading:
                    mLoadingView.setVisibility(View.VISIBLE);
                    break;

                case Loaded:
                    mLoadingView.setVisibility(View.INVISIBLE);
                    mService.showContent();
                    break;
            }

            updateViewLayout();

        }
    }

    @Override
    protected void onSetupLayoutParams() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width;
        int height;
        int flags;

        width = getResources().getDimensionPixelSize(R.dimen.loading_content_width);
        height = getResources().getDimensionPixelSize(R.dimen.loading_content_height);
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        layoutParams = new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_PHONE, flags, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = getDefaultLayoutGravity();
    }

}
