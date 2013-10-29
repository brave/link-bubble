package com.chrislacy.linkbubble.old;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.chrislacy.linkbubble.R;

public class LinkViewActivity extends Activity {

    public static final String LINK_VIEW_URL = "LINK_VIEW_URL";
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);

    static final int ANIM_ON_TIME = 300;
    static final int ANIM_OFF_TIME = 400;

    static final ColorDrawable OFF_COLOR = new ColorDrawable(0x000000);
    static final ColorDrawable ON_COLOR = new ColorDrawable(0xaa000000);

    private WebView mWebView;
    private View mBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String url = intent.getStringExtra(LINK_VIEW_URL);
        if (url != null)  {
            /*
            getActionBar().hide();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActionBar().show();
                }
            }, 100);
            */

            setContentView(R.layout.linkview_activity);
            mWebView = (WebView) findViewById(R.id.webview);
            loadUri(Uri.parse(url));

            mBackground = findViewById(R.id.background);
            mBackground.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateClosed();
                }
            });

            ColorDrawable[] color = {OFF_COLOR, ON_COLOR};
            TransitionDrawable transitionDrawable = new TransitionDrawable(color);
            mBackground.setBackground(transitionDrawable);
            transitionDrawable.startTransition(ANIM_ON_TIME);

            WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            int edgeMargin = getResources().getDimensionPixelSize(R.dimen.left_edge_margin);

            ObjectAnimator animator = ObjectAnimator.ofFloat(mWebView, "x", size.x, edgeMargin);
            animator.setDuration(ANIM_ON_TIME);
            animator.setInterpolator(DECELERATE_CUBIC);
            animator.start();

        } else {
            finish();
        }
    }

    void animateClosed() {
        //getActionBar().hide();

        ColorDrawable[] color = {ON_COLOR, OFF_COLOR};
        TransitionDrawable transitionDrawable = new TransitionDrawable(color);
        mBackground.setBackground(transitionDrawable);
        transitionDrawable.startTransition(ANIM_OFF_TIME);

        WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ObjectAnimator animator = ObjectAnimator.ofFloat(mWebView, "x", size.x);
        animator.setDuration(ANIM_OFF_TIME);
        animator.setInterpolator(ACCELERATE_CUBIC);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    void loadUri(Uri uri) {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(uri.toString());
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {



            }

            public boolean shouldOverrideUrlLoading(WebView view, String url){

                PackageManager packageManager = LinkViewActivity.this.getPackageManager();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                final ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
                if (resolveInfo != null && resolveInfo.activityInfo != null) {
                    String name = resolveInfo.activityInfo.name;
                    if (//!name.contains("com.android.internal")
                        //    && !name.contains("ResolverActivity")
                        //    && !name.contains("com.chrislacy.linkbubble")) {
                            !name.contains("com.chrislacy.linkbubble")) {
                        ComponentName componentName = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, name);
                        intent.setComponent(componentName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        LinkLoadOverlayService.mInstance.getApplication().startActivity(intent);
                        LinkLoadOverlayService.stop();
                        return true;
                    }
                }

                view.loadUrl(url);
                return false; // then it is not handled by default action
            }
        });
    }

}
