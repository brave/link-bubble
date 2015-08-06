package com.linkbubble.webrender;

import android.content.Context;
import android.view.View;

import com.linkbubble.util.Util;
import com.linkbubble.util.YouTubeEmbedHelper;

import java.net.URL;

class StubRenderer extends WebRenderer {

    View mView;

    public StubRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mView = new View(context);
        mView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mView);
        mView.setBackgroundColor(0xffaa0000);
    }

    @Override
    public void destroy() {

    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {

    }

    @Override
    public void loadUrl(URL url, Mode mode) {

    }

    @Override
    public void reload() {

    }

    @Override
    public void stopLoading() {

    }

    @Override
    public void hidePopups() {

    }

    @Override
    public void resetPageInspector() {

    }

    @Override
    public YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper() {
        return null;
    }

    @Override
    public String getUserAgentString() {
        return null;
    }

    @Override
    public void setUserAgentString(String userAgentString) {
    }
}
