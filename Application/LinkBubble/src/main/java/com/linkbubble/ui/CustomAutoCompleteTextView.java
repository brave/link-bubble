/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Menu;
import android.widget.AutoCompleteTextView;

import com.linkbubble.MainController;

public class CustomAutoCompleteTextView extends AutoCompleteTextView {
    public static final long MENU_APPEARS_TIME_ATER_DESTROY = 15000;

    public boolean mCopyPasteContextMenuCreated = false;
    public long mCopyPasteDestroyedLastTime = 0;

    private MainController mMainController = null;

    public CustomAutoCompleteTextView(Context context) {
        super(context);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void configure(MainController mainController) {
        mMainController = mainController;
    }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (!focused) {
            onCopyPasteDestroyed();
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    public boolean onTextContextMenuItem(int id) {
        mCopyPasteDestroyedLastTime = System.currentTimeMillis();
        onCopyPasteDestroyed();

        return super.onTextContextMenuItem(id);
    }

    public void onCopyPasteDestroyed() {
        if (null != mMainController && mCopyPasteContextMenuCreated) {
            mMainController.onBubbleFlowContextMenuAppearedGone(false);
            mCopyPasteContextMenuCreated = false;
        }
    }

    public void onCopyPasteCreated() {
        if (null != mMainController && !mCopyPasteContextMenuCreated) {
            mCopyPasteContextMenuCreated = true;
            mMainController.onBubbleFlowContextMenuAppearedGone(true);
        }
    }
}
