// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.List;

public class AlarmRefreshReceiver extends BroadcastReceiver
{
    /**
     * Used by Notifications.
     */
    static final int NOTIFICATION_ID = 0;
    private static final String[] badgeText = new String[]{" updated status", " expected book", " blog post", " event", " tweet", " reread post", " reread post", ""};

    /**
     * Show an Android notification of unread/new content
     */
    private static void showNotification(Context context)
    {
        int[] badgeNumbers = new int[8];
        // Summary
        List<DB.ProjectsStatus> progressBookList = DB.ProjectsStatus.getCurrentProjects();
        int count_non_viewed = 0;
        // Foreach book
        for (int i = 0; i < progressBookList.size(); i++)
            if (!progressBookList.get(i).viewed)
                count_non_viewed++;
        badgeNumbers[MainActivity.APP_STATE_SUMMARY] = count_non_viewed;
        // BlogPosts
        badgeNumbers[MainActivity.APP_STATE_BLOG] = DB.BlogPost.getStatus().countUnread;
        // Events
        badgeNumbers[MainActivity.APP_STATE_EVENTS] = DB.Event.getStatus().countUnread;
        // Twitter
        badgeNumbers[MainActivity.APP_STATE_TWITTER] = DB.Tweet.getStatus().countUnread;
        // WoR Reread
        badgeNumbers[MainActivity.APP_STATE_WOR] = DB.TorRereadPost.getTotalUnread();


        int countNew = 0;
        boolean useComma = false;
        StringBuilder messageText = new StringBuilder();

        // Create message notification
        for (int i = 0; i < badgeNumbers.length; i++)
            if(i != MainActivity.APP_STATE_BOOKS)
            {
                int countUnread = badgeNumbers[i];
                countNew += countUnread;
                if(countUnread > 0)
                {
                    if(useComma)
                        messageText.append(", ");
                    messageText.append(countUnread);
                    messageText.append(badgeText[i]);
                    if(i != MainActivity.APP_STATE_SUMMARY && countUnread > 1)
                        messageText.append('s');

                    useComma = true;
                }
            }

        if(countNew > 0)
        {
            Notification.Builder mBuilder = new Notification.Builder(context)
                    .setContentTitle("New Sanderson content")
                    .setContentText(messageText.toString())
                    .setSmallIcon(R.drawable.ic_announcement_white_36dp)
                    .setNumber(countNew)
                            // Creates an explicit intent for an Activity in your app
                            // Because clicking the notification opens a new ("special") activity, there's no need to create an artificial back stack.
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));

            // Notify
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, mBuilder.build());
        }
    }
    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            if(DB.currentContext == null)
                DB.currentContext = context;

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, AlarmRefreshReceiver.class);
            PendingIntent appIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

            if (DownloadAndParseWorker.isLocalTest || InternetHelper.isOnline(context))
            {
                // Download and parse data. Use a thread because network operations need to be outside main thread
                Thread networkOperations = new Thread(new Runnable() {
                    public void run() {
                        new DownloadCurrentProjectsWorker().doWork();
                        new DownloadBlogPostsWorker().doWork();
                        new DownloadEventsWorker().doWork();
                        new DownloadTweetsWorker().doWork();
                        new DownloadTorRereadPostsWorker().doWork();

                        // Finishing tasks
                        DB.updateLastDate();
                        InternetHelper.downloadAllFiles(null, false);
                    }
                });
                networkOperations.start();
                networkOperations.join();

                // Show notifications
                showNotification(context);

                // Schedule a new refresh
                long syncScheduleTime = SettingsFragment.getSyncScheduleInSeconds(context) - DB.getLastUpdateSpan();
                if (syncScheduleTime > 0 && syncScheduleTime <= 24 * 60 * 60)
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + syncScheduleTime * 1000, appIntent);
            }
            else
            {
                // Try again in 15 minutes
                alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 15 * 60 * 1000, appIntent);
            }
        }
        catch (Exception ignored)
        {}
    }
}
