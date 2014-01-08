package com.linkbubble.physics;


public interface Draggable {

    public static class TouchEvent {
        public int posX;
        public int posY;
        public float rawX;
        public float rawY;
    }

    public static class MoveEvent {
        public int dx;
        public int dy;
    }

    public static class ReleaseEvent {
        public int posX;
        public int posY;
        public float vx;
        public float vy;
        public float rawX;
        public float rawY;
    }

}
