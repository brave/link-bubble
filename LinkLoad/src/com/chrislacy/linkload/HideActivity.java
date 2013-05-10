package com.chrislacy.linkload;


import android.app.Activity;

import android.os.Bundle;

public class HideActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        WebReaderOverlayService.stop();
			
		finish();
		
	}
    
}
