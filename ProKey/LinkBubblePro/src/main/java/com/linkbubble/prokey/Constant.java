package com.linkbubble.prokey;

import android.content.Context;
import android.util.Log;


public class Constant {

    public static void initCrittercism(Context appContext) {
        try {
            //Crittercism.initialize(appContext, CRITTERCISM_APP_ID);

            /*
            AccountManager am = AccountManager.get(appContext);
            Account[] accounts = am.getAccountsByType("com.google");
            String username = "";
            for (Account account : accounts) {
                username += account.name + " ";
            }
            Crittercism.setUsername(username);
            */
        } catch (SecurityException sex) {
            Log.d("Crittercism", sex.getLocalizedMessage(), sex);
        } catch (Exception ex) {
            Log.d("Crittercism", ex.getLocalizedMessage(), ex);
        }
    }

}
