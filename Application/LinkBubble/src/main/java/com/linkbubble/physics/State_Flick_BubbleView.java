package com.linkbubble.physics;

import android.content.Context;

import com.linkbubble.ui.CanvasView;

/**
 * Created by gw on 24/11/13.
 */
public class State_Flick_BubbleView extends State_Flick {
    public State_Flick_BubbleView(Context c) {
        super(c);
    }
    public boolean isContentView() {
        return false;
    }
}
