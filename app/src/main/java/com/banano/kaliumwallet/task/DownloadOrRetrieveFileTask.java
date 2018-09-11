package com.banano.kaliumwallet.task;

import android.os.AsyncTask;

import com.banano.kaliumwallet.model.Address;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DownloadOrRetrieveFileTask extends AsyncTask<String, Void, List<File>> {
    private DownloadOrRetreiveFileTaskListener listener;
    private File fileDir;

    public DownloadOrRetrieveFileTask(File fileDir) {
        this.fileDir = fileDir;
    }

    private File downloadFile(String sUrl) {
        // context.getFileDir() may return null
        if (fileDir == null) {
            return null;
        }
        File ret = null;
        HttpURLConnection urlConnection = null;
        try {
            String fileName = Address.findAddress(sUrl).trim() + ".svg";
            File file = new File(fileDir, fileName);
            if (file.exists()) {
                return file;
            }
            Timber.d("Downloading image %s", sUrl);
            URL imgUrl = new URL(sUrl);
            urlConnection = (HttpURLConnection) imgUrl.openConnection();

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
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return ret;
    }

    @Override
    protected List<File> doInBackground(String... params) {
        List<File> ret = new ArrayList<>();
        for (String s: params) {
            File f = downloadFile(s);
            if (f != null && f.exists()) {
                ret.add(f);
            }
        }
        return ret;
    }

    @Override
    protected void onPostExecute(List<File> result) {
        super.onPostExecute(result);
        if (listener != null) {
            listener.onDownloadOrRetreiveFileTaskFinished(result);
        }
    }

    public void setListener(DownloadOrRetreiveFileTaskListener listener) {
        this.listener = listener;
    }

    public interface DownloadOrRetreiveFileTaskListener {
        void onDownloadOrRetreiveFileTaskFinished(List<File> f);
    }
}