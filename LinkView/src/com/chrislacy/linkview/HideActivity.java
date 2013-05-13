package com.chrislacy.linkload;


import android.app.Activity;

import android.os.Bundle;
import com.chrislacy.linkload.old.LinkLoadOverlayService;
import com.chrislacy.linkload.old.WebReaderOverlayService;

public class HideActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        WebReaderOverlayService.stop();
        LinkLoadOverlayService.stop();
        LinkViewOverlayService.stop();
			
		finish();
		
	}
    
}
