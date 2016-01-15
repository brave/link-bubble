/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.adblock;

public class EtagObject {
    public String mEtag;
    public long mMilliSeconds;

    public EtagObject() {
        mEtag = "";
        mMilliSeconds = 0;
    }
}
