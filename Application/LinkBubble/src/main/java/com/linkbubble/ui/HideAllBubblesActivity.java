package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;
import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;

public class HideAllBubblesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        if (MainController.get() != null) {
            MainController.get().saveCurrentTabs();
            MainController.get().closeAllBubbles(false);
            MainController.get().finish();
        }

        finish();
    }

}
