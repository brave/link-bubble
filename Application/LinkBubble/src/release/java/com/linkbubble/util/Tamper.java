package com.linkbubble.util;

import android.content.Context;

import dexguard.util.CertificateChecker;
import dexguard.util.TamperDetector;

public class Tamper {

    public static boolean isTweaked(Context context) {
        // You can pick your own value or values for OK,
        // to make the code less predictable.
        final int OK = 545635645;

        // Let the DexGuard utility library detect whether the apk has
        // been modified or repackaged in any way (with jar, zip,
        // jarsigner, zipalign, or any other tool), after DexGuard has
        // packaged it. The return value is the value of the optional
        // integer argument OK (default=0) if the apk is unchanged.
        int apkChanged =
                TamperDetector.checkApk(context, OK);

        // Let the DexGuard utility library detect whether the apk has
        // been re-signed with a different certificate, after DexGuard has
        // packaged it.  The return value is the value of the optional
        // integer argument OK (default=0) if the certificate is still
        // the same.
        int certificateChanged =
                CertificateChecker.checkCertificate(context, OK);

        if (apkChanged != OK || certificateChanged != OK) {
            return true;
        }
        return false;
    }

}