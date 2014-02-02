package com.linkbubble.ui;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

public class Prompt {

    public interface OnPromptClickListener {
        public void onClick();
    }

    public static final int LENGTH_LONG = Toast.LENGTH_LONG;

    public static void show(Context context, CharSequence text, Drawable icon, int duration, OnPromptClickListener listener) {

        Toast.makeText(context, text, duration).show();

    }
}
