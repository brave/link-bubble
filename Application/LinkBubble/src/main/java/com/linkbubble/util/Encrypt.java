/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.util.Base64;
import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

// via https://gist.github.com/aogilvie/6267013#file-string_encrypt_decrypt-md
public class Encrypt {

    private static String TAG = "Encrypt";

    private static String sCryptoPass = "Bl3rGBYl3rG66";

    public static String encryptIt(String value) {
        try {
            DESKeySpec keySpec = new DESKeySpec(sCryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] clearText = value.getBytes("UTF8");
            // Cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            String encrypedValue = Base64.encodeToString(cipher.doFinal(clearText), Base64.DEFAULT);
            Log.d(TAG, "Encrypted: " + value + " -> " + encrypedValue);
            return encrypedValue;

        } catch (java.security.InvalidKeyException e) {
            e.printStackTrace();
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (java.security.spec.InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (javax.crypto.IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String decryptIt(String value) {
        try {
            DESKeySpec keySpec = new DESKeySpec(sCryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.decode(value, Base64.DEFAULT);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            String decrypedValue = new String(decrypedValueBytes);
            Log.d(TAG, "Decrypted: " + value + " -> " + decrypedValue);
            return decrypedValue;

        } catch (java.security.InvalidKeyException e) {
            e.printStackTrace();
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (java.security.spec.InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (javax.crypto.IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return value;
    }
}
