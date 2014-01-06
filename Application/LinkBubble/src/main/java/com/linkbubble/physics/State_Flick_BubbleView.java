package com.linkbubble.physics;

import com.linkbubble.ui.CanvasView;

/**
 * Created by gw on 24/11/13.
 */
public class State_Flick_BubbleView extends State_Flick {
    public State_Flick_BubbleView(CanvasView canvasView) {
        super(canvasView);
    }
    public boolean isContentView() {
        return false;
    }
}
