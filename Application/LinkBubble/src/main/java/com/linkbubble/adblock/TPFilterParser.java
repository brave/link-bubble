/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.adblock;

import android.content.Context;
import com.linkbubble.R;

public class TPFilterParser {

    static {
        System.loadLibrary("LinkBubble");
    }

    private static final String ETAG_PREPEND = "tp";

    public TPFilterParser(Context context) {
        mVerNumber = ADBlockUtils.getDataVerNumber(context.getString(R.string.tracking_protection_url));
        mBuffer = ADBlockUtils.readData(context, context.getString(R.string.tracking_protection_localfilename),
                context.getString(R.string.tracking_protection_url), ETAG_PREPEND, mVerNumber, false);
        if (mBuffer != null) {
            init(mBuffer);
        }
    }


    public native void init(byte[] data);
    public native boolean matchesTracker(String baseHost);
    public native String findFirstPartyHosts(String baseHost);

    private byte[] mBuffer;
    private String mVerNumber;
}