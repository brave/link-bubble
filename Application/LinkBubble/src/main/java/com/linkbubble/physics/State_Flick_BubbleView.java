package com.linkbubble.physics;

import com.linkbubble.ui.Canvas;

/**
 * Created by gw on 24/11/13.
 */
public class State_Flick_BubbleView extends State_Flick {
    public State_Flick_BubbleView(Canvas canvas) {
        super(canvas);
    }
    public boolean isContentView() {
        return false;
    }
}
