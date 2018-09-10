package com.banano.kaliumwallet.task;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import timber.log.Timber;

public class DownloadOrRetreiveFileTask extends AsyncTask<String, Void, File> {
    private DownloadOrRetreiveFileTaskListener listener;
    private File fileDir;
    private String fileName;

    public DownloadOrRetreiveFileTask(File fileDir, String fileName) {
        this.fileDir = fileDir;
        this.fileName = fileName;
    }

    private File downloadFile(String sUrl) {
        // context.getFileDir() may return null
        if (fileDir == null) {
            return null;
        }
        File ret = null;
        try {
            File file = new File(fileDir, fileName);
            if (file.exists()) {
                return file;
            }
            Timber.d("Downloading image %s", sUrl);
            URL imgUrl = new URL(sUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) imgUrl.openConnection();

            Timber.d("Response Code %d", urlConnection.getResponseCode());

            try (InputStream inputStream = urlConnection.getInputStream();
                 FileOutputStream fileOutput = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                }
            }
            if (file.exists()) {
                ret = file;
            }
        } catch (Exception e) {
            Timber.e("Failed to download image from %s", sUrl);
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    protected File doInBackground(String... params) {
        return downloadFile(params[0]);
    }

    @Override
    protected void onPostExecute(File f) {
        super.onPostExecute(f);
        if (listener != null) {
            listener.onDownloadOrRetreiveFileTaskFinished(f);
        }
    }

    public void setListener(DownloadOrRetreiveFileTaskListener listener) {
        this.listener = listener;
    }

    public interface DownloadOrRetreiveFileTaskListener {
        void onDownloadOrRetreiveFileTaskFinished(File f);
    }
}