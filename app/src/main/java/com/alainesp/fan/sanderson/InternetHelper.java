// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Class to handle Internet related things.
 */
final class InternetHelper
{
    /**
     * A list with urls of files to download
     */
    private static ArrayList<String> filesToDownload = new ArrayList<>();

    /**
     * Check that we have connectivity.
     * @return If the user had access to a Network.
     */
    public static boolean isOnline(Context context)
    {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected())
            return false;

        // Check settings
        return !(SettingsFragment.getUseWifiOnly(context) && (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE));
    }

    /**
     * Connect to a given url.
     * @param urlString The url to connect.
     * @return An InputStream to read data from.
     */
    public static InputStream connectUrl(String urlString)
    {
        InputStream is = null;

        try
        {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            is = conn.getInputStream();
        }
        catch (Exception e)
        {
            Logger.reportError(e.toString());
        }

        return is;
    }

    /**
     * Interface to notify of files downloading.
     */
    public interface DownloadFileNotification
    {
        void beginDownloadingFilesGUI(int numFiles);
        void endDownloadingFilesGUI();
        void progressDownloadingFilesGUI(int numFiles);
    }

    private static void downloadAllFilesWork(DownloadFileTask task)
    {
        if (filesToDownload != null && filesToDownload.size() > 0)
        {
            DB.DbHelper mDbHelper = new DB.DbHelper(DB.currentContext);

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            //  Compile SQL statements
            SQLiteStatement insertFile = db.compileStatement("INSERT INTO Files (Url) VALUES (?)");
            SQLiteStatement selectFile = db.compileStatement("SELECT _id FROM Files WHERE Url=?");

            int numFiles = filesToDownload.size();
            for (String url : filesToDownload)
            {
                // Get ID
                long fileID;
                selectFile.bindString(1, url);
                try// Check if url is already added to Files
                {
                    fileID = selectFile.simpleQueryForLong();
                }
                catch (SQLiteDoneException e)// Not exists -> Insert new data
                {
                    insertFile.bindString(1, url);
                    fileID = insertFile.executeInsert();
                }
                // Download the file if not exists
                File file = new File(DB.currentContext.getExternalFilesDir(null), "" + fileID);
                if(file.length() <= 0)
                    downloadURLToFile(url, file);

                // Publish notification.
                numFiles--;
                if(task != null)
                    task.reportFilesInQueue(numFiles);
            }

            insertFile.close();
            selectFile.close();
            db.close();

            filesToDownload.clear();
        }
    }
    /**
     * An AsyncTask to download files.
     */
    public static class DownloadFileTask extends AsyncTask<Void, Integer, Void>
    {
        /**
         * A callback notification.
         */
        DownloadFileNotification callbackNotif;

        /**
         * Constructor
         * @param aCallback The notification to set
         */
        public DownloadFileTask(@Nullable DownloadFileNotification aCallback)
        {
            callbackNotif = aCallback;
        }
        // Do the long-running work in here
        protected Void doInBackground(Void... ignored)
        {
            downloadAllFilesWork(this);

            return null;
        }

        // After we begin
        protected void onPreExecute ()
        {
            if(callbackNotif != null)
                callbackNotif.beginDownloadingFilesGUI(filesToDownload.size());
        }

        public void reportFilesInQueue(int numFiles)
        {
            publishProgress(numFiles);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress)
        {
            if(callbackNotif != null)
                callbackNotif.progressDownloadingFilesGUI(progress[0]);
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Void param)
        {
            if(callbackNotif != null)
                callbackNotif.endDownloadingFilesGUI();
        }
    }

    /**
     * Begin downloading all files already added to download.
     * @param aCallback The callback to provide notifications.
     * @param isAsync Use asynchronous operation?.
     */
    public static void downloadAllFiles(DownloadFileNotification aCallback, boolean isAsync)
    {
        if(isAsync)
            new DownloadFileTask(aCallback).execute();
        else
            downloadAllFilesWork(null);
    }

    /**
     * Get a remote file as a local cached file. If not exist queue it to download.
     * @param url The remote url.
     * @return The local file or null if not exists.
     */
    public static File getRemoteFile(String url)
    {
        if(url != null && !url.isEmpty())
        {
            long fileID = getFileIDFromDB(url);
            if (fileID >= 0)
            {
                File file = new File(DB.currentContext.getExternalFilesDir(null), "" + fileID);
                if (file.length() > 0)
                    return file;
            }

            if (!filesToDownload.contains(url))
                filesToDownload.add(url);
        }
        return null;
    }

    /**
     * Get the DB id of the given URL.
     * @param url The url.
     * @return The DB id of the URl or -1 if not exist.
     */
    private static long getFileIDFromDB(String url)
    {
        long imgID = -1;
        DB.DbHelper mDbHelper = new DB.DbHelper(DB.currentContext);

        // Gets the data repository in read mode
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT _id FROM Files WHERE Url=?", new String[]{url});
        if (c.moveToFirst())
            imgID = c.getLong(0);
        c.close();

        db.close();

        return imgID;
    }

    /**
     * Download a file.
     * @param url The remote url of the file.
     * @param outFile The local file to write data.
     */
    private static void downloadURLToFile(String url, File outFile)
    {
        try
        {
            InputStream is = connectUrl(url);
            if (is != null)
            {
                OutputStream os = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];

                // Download
                int readBytes;
                while ((readBytes = is.read(buffer)) != -1)
                    os.write(buffer, 0, readBytes);

                is.close();
                os.close();
            }
        }
        catch (IOException ignored){}
    }

    /**
     * Class to handle URL images.
     */
    public static class UrlImageGetter implements Html.ImageGetter
    {
        @Override
        public Drawable getDrawable(String source)
        {
            Drawable d;
            // Get the remote file
            File file = InternetHelper.getRemoteFile(source);

            if(file != null)
                d = new BitmapDrawable(DB.currentContext.getResources(), file.getAbsolutePath());
            else
                d = DB.currentContext.getResources().getDrawable(R.drawable.ic_action_picture);

            // Scale the image
            float scaleFactor = getScaleFactor(d);
            d.setBounds(0, 0, (int) (d.getIntrinsicWidth() * scaleFactor), (int) (d.getIntrinsicHeight() * scaleFactor));

            return d;
        }
        public static float getScaleFactor(Drawable d)
        {
            // Calculate the scaling of the image
            DisplayMetrics screenMetric = DB.currentContext.getResources().getDisplayMetrics();
            float scaleFactor = screenMetric.density;
            float screenWidth = screenMetric.widthPixels;

            // Adjust the image based on the screen
            if(d.getIntrinsicWidth()*scaleFactor > screenWidth)
                scaleFactor = screenWidth/d.getIntrinsicWidth();

            return scaleFactor;
        }
    }
}

class Logger
{
    /**
     * A list with errors that occurred in the application.
     */
    private static final ArrayList<String> errors = new ArrayList<>(5);

    public static void reportError(String message)
    {
        if(!errors.contains(message))
            errors.add(message);
    }
    public static void showErrors(Context context)
    {
        // Show errors
        if(SettingsFragment.getShowErrorMessages(context))
        {
            for (String message : errors)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
        errors.clear();
    }

//    public static  void writeErrorToFile(Exception e)
//    {
//        try
//        {
//            PrintStream logFile = new PrintStream("/sdcard/Android/data/com.alainesp.fan.sanderson/files/log.txt");
//
//            logFile.print("FoS Alarm Error: " + e.toString() + "\n");
//            e.printStackTrace(logFile);
//
//            logFile.close();
//        }
//        catch (Exception ignored){}
//    }
}

