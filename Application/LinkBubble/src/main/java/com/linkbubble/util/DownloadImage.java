package com.linkbubble.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.linkbubble.MainController;
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

    private static final String TAG = "DownloadImage";

    private Context mContext;
    private String mUrlAsString;

    public DownloadImage(Context context, String urlAsString) {
        mContext = context;
        mUrlAsString = urlAsString;
    }

    public void download() {
        Log.d(TAG, "downloading image: " + mUrlAsString);
        Toast.makeText(mContext, R.string.notice_download_started, Toast.LENGTH_LONG).show();
        new DownloadImageTask().execute(mUrlAsString);
    }

    private void showErrorPrompt() {
        Resources resources = mContext.getResources();
        String message = resources.getString(R.string.error_saving_image);
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

    private void showSuccessPrompt(final Uri imageUri) {
        Resources resources = mContext.getResources();
        String message = resources.getString(R.string.image_saved);
        Prompt.show(message, mContext.getResources().getString(R.string.action_open),
                Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(imageUri, "image/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                        MainController.get().switchToBubbleView();
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
                byte[] buffer;
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                // If the URL is a data url we need to decode it.
                if (URLUtil.isDataUrl(urls[0])) {
                    String[] parts = urls[0].split("base64,");
                    String encoded = parts[1];
                    buffer = Base64.decode(encoded, Base64.DEFAULT);
                    output.write(buffer, 0, buffer.length);
                } else {
                    // For a normal image download we just read the image from the URL.
                    URL url = new URL(urls[0]);
                    InputStream is = (InputStream) url.getContent();
                    buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                File path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(urls[0]);
                String name = URLUtil.guessFileName(urls[0], null, fileExtenstion);

                // Prefix the filename with the date to attempt to prevent overwriting of existing files.
                name = System.currentTimeMillis() + name;

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
            if (result) {
                // Fire an intent to scan for the gallery.
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imagePath);
                mediaScanIntent.setData(contentUri);
                mContext.sendBroadcast(mediaScanIntent);
                showSuccessPrompt(contentUri);
            } else {
                showErrorPrompt();
            }
        }
    }

}
