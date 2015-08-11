package com.linkbubble.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.linkbubble.R;
import com.linkbubble.ui.Prompt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by kevin on 8/11/15.
 */
public class DownloadImage {

    private Context mContext;
    private String mUrlAsString;

    public DownloadImage(Context context, String urlAsString) {
        mContext = context;
        mUrlAsString = urlAsString;
    }

    public void download() {
        new DownloadImageTask().execute(mUrlAsString);
    }

    private void simplePrompt(String message) {
        Prompt.show(message, mContext.getResources().getString(android.R.string.ok),
                Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                    }

                    @Override
                    public void onClose() {
                    }
                });
    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Boolean> {
        File imagePath;
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                InputStream is = (InputStream) url.getContent();
                byte[] buffer = new byte[8192];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                while ((bytesRead = is.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                File path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(urls[0]);
                String name = URLUtil.guessFileName(urls[0], null, fileExtenstion);
                imagePath = new File(path, name);
                FileOutputStream fos = new FileOutputStream(imagePath);
                fos.write(output.toByteArray());
                fos.flush();
                fos.close();
                return true;
            } catch (IOException e) {
                CrashTracking.logHandledException(e);
                return false;
            }
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Boolean result) {
            Resources resources = mContext.getResources();
            if (result) {
                // Fire an intent to scan for the gallery.
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imagePath);
                mediaScanIntent.setData(contentUri);
                mContext.sendBroadcast(mediaScanIntent);
                simplePrompt(resources.getString(R.string.image_saved));
            } else {
                simplePrompt(resources.getString(R.string.error_saving_image));
            }
        }
    }

}
