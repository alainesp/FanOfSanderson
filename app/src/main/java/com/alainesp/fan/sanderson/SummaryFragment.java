// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Fragment to show a summary of information. By default is the content showed at start.
 */
public class SummaryFragment extends Fragment implements InternetHelper.DownloadFileNotification, UpdateGUIProgressBook
{
    // Labels of different data showed
    private LabelStatus currentProjects;
    private LabelStatus blogPostLabel;
    private LabelStatus eventsLabel;
    private LabelStatus booksLabel;
    private LabelStatus twitterLabel;
    private LabelStatus worRereadLabel;
    private LabelStatus lastUpdate;

    // The tables to add data
    private TableLayout currentProjectTable;
    private TableLayout booksTable;

    public void updateGUIProgressBook(List<DB.ProjectsStatus> progressBookList)
    {
        // Clear the table
        currentProjectTable.removeAllViews();
        int hmargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        // Text styles
        int highLightColor = LabelStatus.highLightColor;
        int count_non_viewed = 0;

        // Foreach book
        for (int i = 0; i < progressBookList.size(); i++)
        {
            TableRow row_book = new TableRow(MainActivity.staticRef);

            // Title
            TextView book_title = new TextView(MainActivity.staticRef);
            book_title.setGravity(Gravity.START);

            // Style if a Project updates
            if (!progressBookList.get(i).viewed)
            {
                book_title.setTextColor(highLightColor);
                count_non_viewed++;
                book_title.setText(Html.fromHtml("<b>" + progressBookList.get(i).title + "</b>"));
            }
            else
                book_title.setText(progressBookList.get(i).title);
            row_book.addView(book_title);

            // Progress bar
            ProgressBar book_progress = new ProgressBar(MainActivity.staticRef, null, android.R.attr.progressBarStyleHorizontal);
            book_progress.setPadding(hmargin, 0, hmargin, 0);
            book_progress.setProgress(progressBookList.get(i).percent);
            book_progress.setMinimumWidth(10);
            row_book.addView(book_progress);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) book_progress.getLayoutParams();
            params.weight = 1;
            params.gravity = Gravity.CENTER_VERTICAL;

            // Percent text
            TextView book_completion = new TextView(MainActivity.staticRef);
            book_completion.setGravity(Gravity.END);
            if (!progressBookList.get(i).viewed)
            {
                book_completion.setTextColor(highLightColor);
                book_completion.setText(Html.fromHtml("<b>" + progressBookList.get(i).percent + "%</b>"));
            }
            else
                book_completion.setText("" + progressBookList.get(i).percent + "%");
            row_book.addView(book_completion);

            currentProjectTable.addView(row_book);
        }

        currentProjects.setNumberState(count_non_viewed, progressBookList.size());
        MainActivity.setBadgeNumber(MainActivity.APP_STATE_SUMMARY, count_non_viewed);

        currentProjects.setUpdateState(false);
    }
    private void updateBooks()
    {
        StringBuilder booksString = new StringBuilder();
        booksString.append(Catalog.Brandon.getSeriesCount());
        booksString.append(" series / ");
        booksString.append(Catalog.Brandon.getTotalBookCount());
        booksString.append(" books");
        if(Catalog.Brandon.getUnpublishedBookCount() > 0)
        {
            booksString.append(" / <b>");
            booksString.append(Catalog.Brandon.getUnpublishedBookCount());
            booksString.append(" new</b>");
        }
        booksLabel.setNumberState(Html.fromHtml(booksString.toString()), false);

        // Clear the table
        booksTable.removeAllViews();
        DateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        // Populate the new books
        List<Book> newBooks = Catalog.Brandon.getUnpublishedBooks();
        for (int j = 0; j < newBooks.size(); j++)
        {
            TableRow row_book = new TableRow(MainActivity.staticRef);
            row_book.setTag(newBooks.get(j));
            row_book.setOnClickListener(BooksFragment.onBookClick);
            // Same week
            boolean publishIsNear = (newBooks.get(j).publishedDate.getTime() - new Date().getTime())/DateUtils.DAY_IN_MILLIS < 7;

            // Title
            TextView book_title = new TextView(MainActivity.staticRef);
            if(publishIsNear)
            {
                book_title.setText(Html.fromHtml("<b>" + newBooks.get(j).title + "</b>"));
                book_title.setTextColor(Color.BLACK);
            }
            else
                book_title.setText(newBooks.get(j).title);
            row_book.addView(book_title);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) book_title.getLayoutParams();
            params.weight = 1;

            // Date
            TextView book_date = new TextView(MainActivity.staticRef);
            if(publishIsNear)
            {
                book_date.setText(Html.fromHtml("<b>" + formatPublishedDate(newBooks.get(j).publishedDate, format) + "</b>"));
                book_date.setTextColor(Color.BLACK);
            }
            else
                book_date.setText(formatPublishedDate(newBooks.get(j).publishedDate, format));
            row_book.addView(book_date);

            booksTable.addView(row_book);
        }
    }
    private String formatPublishedDate(Date publishedDate, DateFormat format)
    {
        long diffTime = publishedDate.getTime() - new Date().getTime();

        if (diffTime < DateUtils.DAY_IN_MILLIS)
            return "Today";

        if (diffTime < DateUtils.DAY_IN_MILLIS * 2)
            return "Tomorrow";

        if (diffTime < DateUtils.DAY_IN_MILLIS * 45)
            return "in " + (diffTime + DateUtils.DAY_IN_MILLIS / 2) / DateUtils.DAY_IN_MILLIS + " days";

        if (diffTime < DateUtils.YEAR_IN_MILLIS)
            return "in " + (diffTime + DateUtils.DAY_IN_MILLIS * 30 / 2) / (DateUtils.DAY_IN_MILLIS * 30) + " months";

        return format.format(publishedDate);
    }

    // Handle the last part of the refresh: download additional files
    public void beginDownloadingFilesGUI(int totalFiles)
    {
        lastUpdate.setUpdateState(true);
        lastUpdate.setTitle("Downloading " + totalFiles + (totalFiles == 1 ? " image..." : " images..."));
    }
    public void endDownloadingFilesGUI()
    {
        lastUpdate.setUpdateState(false);
        lastUpdate.setTitle("Last Update:");
        setLastUpdateString();

        // Show errors
        Logger.showErrors(MainActivity.staticRef.getApplicationContext());

        //refreshItem.setEnabled(true);
        if(refreshItem != null)
            refreshItem.setVisible(true);

        // Schedule a new refresh
        long syncScheduleTime = SettingsFragment.getSyncScheduleInSeconds(DB.currentContext) - DB.getLastUpdateSpan();
        if(syncScheduleTime > 0 && syncScheduleTime <= 24*60*60)
        {
            AlarmManager alarmManager = (AlarmManager) MainActivity.staticRef.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(MainActivity.staticRef, AlarmRefreshReceiver.class);
            PendingIntent appIntent = PendingIntent.getBroadcast(MainActivity.staticRef, 0, alarmIntent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + syncScheduleTime * 1000, appIntent);
        }
    }
    public void progressDownloadingFilesGUI(int numFiles)
    {
        lastUpdate.setTitle("Downloading " + numFiles + (numFiles == 1 ? " file" : " files"));
    }
    private void setLastUpdateString()
    {
        String lastUpdateString = "<i>Never</i>";
        long diffTime = DB.getLastUpdateSpan();
        if(diffTime >= 0)
        {
            if(diffTime == 0)
            {
                lastUpdateString = "Now";
            }
            else if(diffTime < 60)
            {
                lastUpdateString = "" + diffTime + (diffTime == 1 ? " second ago" : " seconds ago");
            }
            else if(diffTime < 3600)
            {
                diffTime = (diffTime+30)/60;
                lastUpdateString = "" + diffTime + (diffTime == 1 ? " minute ago" : " minutes ago");
            }
            else if(diffTime < 86400)
            {
                diffTime = (diffTime+3600/2)/3600;
                lastUpdateString = "" + diffTime + (diffTime == 1 ? " hour ago" : " hours ago");
            }
            else if(diffTime < 86400*3)
            {
                diffTime = (diffTime+86400/2)/86400;
                lastUpdateString = "" + diffTime + (diffTime == 1 ? " day ago" : " days ago");
            }
            else
            {
                diffTime = (diffTime+86400*30/2)/(86400*30);
                lastUpdateString = "" + diffTime + (diffTime == 1 ? " month ago" : " months ago");
            }
        }
        lastUpdate.setNumberState(Html.fromHtml(lastUpdateString), false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_summary, container, false);

        currentProjectTable = (TableLayout)rootView.findViewById(R.id.current_projects_table);
        booksTable = (TableLayout)rootView.findViewById(R.id.books_table);

        currentProjects = (LabelStatus) rootView.findViewById(R.id.current_projects);
        blogPostLabel = (LabelStatus) rootView.findViewById(R.id.blog_posts);
        blogPostLabel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MainActivity.navigateTo(MainActivity.APP_STATE_BLOG);
            }
        });
        eventsLabel = (LabelStatus) rootView.findViewById(R.id.events);
        eventsLabel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MainActivity.navigateTo(MainActivity.APP_STATE_EVENTS);
            }
        });
        booksLabel = (LabelStatus) rootView.findViewById(R.id.books);
        booksLabel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MainActivity.navigateTo(MainActivity.APP_STATE_BOOKS);
            }
        });
        twitterLabel = (LabelStatus) rootView.findViewById(R.id.twitter);
        twitterLabel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MainActivity.navigateTo(MainActivity.APP_STATE_TWITTER);
            }
        });
        worRereadLabel = (LabelStatus) rootView.findViewById(R.id.wor_reread);
        worRereadLabel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MainActivity.navigateTo(MainActivity.APP_STATE_WOR);
            }
        });
        lastUpdate = (LabelStatus) rootView.findViewById(R.id.last_update);

        // Projects
        updateGUIProgressBook(DB.ProjectsStatus.getCurrentProjects());
        // BlogPosts
        DB.LabelStatus status = DB.BlogPost.getStatus();
        blogPostLabel.setNumberState(status.countUnread, status.total);
        // Events
        // TODO: Calculate near events and show them
        status = DB.Event.getStatus();
        eventsLabel.setNumberState(status.countUnread, status.total);
        // Books
        updateBooks();
        // Twitter
        status = DB.Tweet.getStatus();
        twitterLabel.setNumberState(status.countUnread, status.total);
        // WoR Reread
        StringBuilder builder = new StringBuilder();
        boolean useHighlight = DB.TorRereadPost.getStatus(builder);
        worRereadLabel.setNumberState(Html.fromHtml(builder.toString()), useHighlight);
        // Last update
        setLastUpdateString();

        setHasOptionsMenu(true);

        // Cancel the notification as we show data now in Summary.
        ((NotificationManager)MainActivity.staticRef.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(AlarmRefreshReceiver.NOTIFICATION_ID);

        // If first invocation or the last update is old enough -> trigger a refresh
        if(DB.getLastUpdateSpan() - SettingsFragment.getSyncScheduleInSeconds(DB.currentContext) >= 0)
            refreshData(rootView);

        return rootView;
    }

    /**
     * Connect to internet and synchronize data
     */
    private void refreshData(View rootView)
    {
        // Cancel alarm
        AlarmManager alarmManager = (AlarmManager) MainActivity.staticRef.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(MainActivity.staticRef, AlarmRefreshReceiver.class);
        PendingIntent appIntent = PendingIntent.getBroadcast(MainActivity.staticRef, 0, alarmIntent, 0);
        alarmManager.cancel(appIntent);

        if (DownloadAndParseWorker.isLocalTest || InternetHelper.isOnline(DB.currentContext))
        {
            //refreshItem.setEnabled(false);
            if(refreshItem != null)
                refreshItem.setVisible(false);
            lastUpdate.setUpdateState(true);
            lastUpdate.setTitle("Updating...");
            new DownloadCurrentProjectsTask(currentProjects, blogPostLabel, eventsLabel, twitterLabel, worRereadLabel, this, this).execute();
        }
        else
        {
            Snackbar.make(rootView, "Can't refresh data in offline mode.", Snackbar.LENGTH_LONG).show();
            // Try again in 15 minutes
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 15 * 60 * 1000, appIntent);
        }
    }
    private MenuItem refreshItem = null;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.summary_menu, menu);

        refreshItem = menu.findItem(R.id.summary_refresh);

        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {
            case R.id.summary_refresh:
                refreshData(getView());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

/**
 * View to show in the summary
 */
class LabelStatus extends LinearLayout
{
    private final TextView label;
    private ProgressBar update_bar;
    private TextView number_info;

    // Text styles
    public static final int highLightColor = Color.BLACK;
    private int normalColorText = 0;

    public LabelStatus(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        setGravity(Gravity.CENTER_VERTICAL);
        setOrientation(LinearLayout.HORIZONTAL);

        // Label
        label = new TextView(context, null, android.R.attr.textAppearanceMedium);
        label.setTextColor(highLightColor);
        label.setSingleLine(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1;
        addView(label, layoutParams);
        // Set title from xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LabelStatus, 0, 0);
        try { label.setText(a.getString(R.styleable.LabelStatus_Title)); }
        finally { a.recycle();}

        // The right part
        update_bar = new ProgressBar(context);
        update_bar.setVisibility(View.GONE);
        addView(update_bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        number_info = new TextView(context);
        number_info.setText("0");
        number_info.setGravity(Gravity.CENTER_VERTICAL);
        // Text style
        normalColorText = number_info.getCurrentTextColor();

        addView(number_info, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, update_bar.getMinimumHeight()));
    }

    public void setTitle(String title)
    {
        label.setText(title);
    }

    public void setUpdateState(boolean is_updating)
    {
        update_bar.setVisibility(is_updating ? View.VISIBLE : View.GONE);
        number_info.setVisibility(is_updating ? View.GONE : View.VISIBLE);
    }
    public void setNumberState(int count_new, int total)
    {
        if(count_new == 0)
        {
            number_info.setText("" + total);
            number_info.setTextColor(normalColorText);
        }
        else
        {
            number_info.setText(Html.fromHtml("<b>" + count_new + " / " + total + "</b>"));
            number_info.setTextColor(highLightColor);
        }
    }
    public void setNumberState(Spanned text, boolean useHighlightColor)
    {
        number_info.setText(text);
        number_info.setTextColor(useHighlightColor ? highLightColor : normalColorText);
    }
}

// Workers
abstract class DownloadAndParseWorker<T>
{
    private final String url;

//    // Testing without Internet
//    public static final boolean isLocalTest = true;
//    protected static final String WEB_MAIN_URL = "/sdcard/Brandon Sanderson.html";
//    protected static final String WEB_BLOG_FEED = "/sdcard/feed.xml";
//    protected static final String WEB_EVENTS = "/sdcard/events.html";
//    protected static final String WOR_REREAD = "/sdcard/wor.xml";

    // Normal state
    static final boolean isLocalTest = false;
    static final String WEB_MAIN_URL = "http://brandonsanderson.com/";
    static final String WEB_BLOG_FEED = "http://brandonsanderson.com/feed/";
    static final String WEB_EVENTS = "http://brandonsanderson.com/upcoming-events/";
    static final String WOR_REREAD = "http://www.tor.com/series/words-of-radiance-reread-on-torcom/feed/";

    protected DownloadAndParseWorker(String url)
    {
        this.url = url;
    }

    protected abstract List<T> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException;
    protected abstract void updateDB(List<T> objects);

    protected void parseImages(String htmlText)
    {
        try
        {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
            parser.setInput(new ByteArrayInputStream(htmlText.getBytes()), null);
            parser.nextTag();

            // Begin parsing
            boolean complete_parsing = false;
            while (!complete_parsing)
            {
                int next_result = XmlPullParser.END_TAG;

                try { next_result = parser.next(); }
                catch (XmlPullParserException ignored){}

                switch (next_result)
                {
                    case XmlPullParser.START_TAG:
                        if ("img".equals(parser.getName()))
                        {
                            String imgSource = parser.getAttributeValue(null, "src");
                            if(imgSource != null)
                                InternetHelper.getRemoteFile(imgSource);
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        complete_parsing = true;
                        break;
                }
            }
        }
        catch (Exception ignored) { }
    }

    protected int doWork()
    {
        InputStream in = null;
        List<T> objects = null;
        int state = DownloadParseSaveTask.STATE_SUCCESS;

        try
        {
            if(isLocalTest)
                in = new FileInputStream(url);
            else
                in = InternetHelper.connectUrl(url);

            if(in != null)
            {
                MyXMLParser parser = new MyXMLParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                parser.setInput(in, null);

                objects = downloadParse(parser);
            }
            else
                state = DownloadParseSaveTask.STATE_ERROR_URL;
        }
        catch (Exception e)
        {
            Logger.reportError(e.toString());
            state = DownloadParseSaveTask.STATE_ERROR_PARSING;
        }
        finally
        {
            try { if(in != null) in.close(); }
            catch (Exception ignored){}
        }

        try
        {
            if (objects != null)
                updateDB(objects);
        }
        catch (Exception e)
        {
            Logger.reportError(e.toString());
            state = DownloadParseSaveTask.STATE_ERROR_DB;
        }

        return state;
    }
}
// Retrieve the progress of books currently working on
class DownloadCurrentProjectsWorker extends DownloadAndParseWorker<DB.ProjectsStatus>
{
    DownloadCurrentProjectsWorker()
    {
        super(WEB_MAIN_URL);
    }

    @Override
    protected List<DB.ProjectsStatus> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        ArrayList<DB.ProjectsStatus> current_projects = new ArrayList<>(5);
        // Begin parsing
        boolean complete_parsing = false;
        while (!complete_parsing)
        {
            int next_result = XmlPullParser.END_TAG;

            try { next_result = parser.next(); }
            catch (XmlPullParserException ignored){ }

            switch (next_result)
            {
                case XmlPullParser.START_TAG:
                    // This is what we look for: Now parse the Current Projects progress
                    if ("div".equals(parser.getName()) && "progress-titles".equals(parser.getAttributeValue(null, "class")))
                    {
                        int progress_depth = parser.getDepth();

                        DB.ProjectsStatus current_book = new DB.ProjectsStatus();
                        while (parser.next() != XmlPullParser.START_TAG) ;
                        // Finish when out of this Tag
                        while (parser.getDepth() >= progress_depth)
                        {
                            if (parser.getEventType() == XmlPullParser.START_TAG)
                            {
                                String aClass = parser.getAttributeValue(null, "class");
                                // Read book title
                                if ("book-title".equals(aClass))
                                {
                                    while (parser.next() != XmlPullParser.TEXT) ;
                                    current_book.title = parser.getText();
                                    current_book.percent = -1;
                                }

                                // Read progress
                                if ("after".equals(aClass))
                                {
                                    // Find the percent value
                                    while (parser.next() != XmlPullParser.TEXT) ;
                                    String percent_str = parser.getText();

                                    // Get only the digits
                                    int count_digits = 0;
                                    for (; count_digits < percent_str.length(); count_digits++)
                                        if (!Character.isDigit(percent_str.charAt(count_digits)))
                                            break;
                                    percent_str = percent_str.substring(0, count_digits);

                                    // Make sure we get a number
                                    if (!"".equals(percent_str))
                                    {
                                        current_book.percent = Integer.parseInt(percent_str);

                                        if (current_book.isValid())
                                            current_projects.add(current_book);
                                    }
                                    current_book = new DB.ProjectsStatus();
                                }
                            }
                            parser.next();
                        }

                        complete_parsing = true;
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    complete_parsing = true;
                    break;
            }
        }

        return current_projects;
    }

    @Override
    protected void updateDB(List<DB.ProjectsStatus> current_projects)
    {
        DB.ProjectsStatus.updateCurrentProjects(current_projects);
    }
}
// Retrieve the blog posts
class DownloadBlogPostsWorker extends DownloadAndParseWorker<DB.BlogPost>
{
    DownloadBlogPostsWorker()
    {
        super(WEB_BLOG_FEED);
    }

    @Override
    protected List<DB.BlogPost> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        List<DB.BlogPost> blogPosts = new ArrayList<>(10);

        // Begin parsing
        boolean complete_parsing = false;
        DB.BlogPost current_post = null;
        while (!complete_parsing)
        {
            int next_result = XmlPullParser.END_TAG;

            try{ next_result = parser.next(); }
            catch (XmlPullParserException ignored){}

            String tag = parser.getName();

            switch (next_result)
            {
                case XmlPullParser.END_TAG:
                    if ("item".equals(tag) && current_post != null)
                    {
                        if(current_post.isValid())
                            blogPosts.add(current_post);

                        current_post = null;
                    }
                    break;
                case XmlPullParser.START_TAG:
                    // This is what we look for: Now parse the Current Projects progress
                    if ("item".equals(tag))
                    {
                        current_post = new DB.BlogPost();
                    }
                    else if ("title".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.title = parser.getText();
                    }
                    else if ("link".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.link = new URL(parser.getText());
                    }
                    else if ("pubDate".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);
                        try{ current_post.publicationDate = format.parse(parser.getText());}
                        catch (ParseException ignored){ }
                    }
                    else if ("dc".equals(parser.getPrefix()) && "creator".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.creator = parser.getText();
                    }
                    else if ("category".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        // Ignore twitter archive posts given we provide them
                        String category = parser.getText();
                        if("Tweet Archive".equals(category))
                            current_post = null;
                        else
                            current_post.addCategory(category);
                    }
                    else if ("guid".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        String guid_url = parser.getText();
                        int len = 0;
                        for (; len < guid_url.length(); len++)
                            if(!Character.isDigit(guid_url.charAt(guid_url.length() - 1 - len)))
                                break;

                        guid_url = guid_url.substring(guid_url.length() - len, guid_url.length());
                        current_post.guid = Integer.parseInt(guid_url);
                    }
                    else if ("content".equals(parser.getPrefix()) && "encoded".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.html_content = parser.getText();
                        parseImages(current_post.html_content);
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    complete_parsing = true;
                    break;
            }
        }

        return blogPosts;
    }

    @Override
    protected void updateDB(List<DB.BlogPost> blogPosts)
    {
        DB.BlogPost.updateBlogPosts(blogPosts);
    }
}
// Retrieve the events
class DownloadEventsWorker extends DownloadAndParseWorker<DB.Event>
{
    DownloadEventsWorker()
    {
        super(WEB_EVENTS);
    }

    private String getTextInsideTags(XmlPullParser parser, String tag) throws IOException
    {
        boolean complete_parse_tag = false;
        StringBuilder stringBuilder = new StringBuilder();
        while (!complete_parse_tag)
        {
            int next_result = -1;
            try { next_result = parser.next(); }
            catch (XmlPullParserException ignored){ }
            switch (next_result)
            {
                case XmlPullParser.END_TAG:
                    if (tag.equals(parser.getName()))
                        complete_parse_tag = true;
                    break;
                case XmlPullParser.END_DOCUMENT:
                    complete_parse_tag = true;
                    break;
                case XmlPullParser.TEXT:
                    stringBuilder.append(parser.getText());
                    break;
            }
        }

        if(stringBuilder.length()==0)
            return null;

        for (int i = 0; i < stringBuilder.length(); i++)
            if(stringBuilder.charAt(i) == '\n' || stringBuilder.charAt(i) == '\t')
            {
                stringBuilder.deleteCharAt(i);
                i--;
            }

        return stringBuilder.toString().trim();
    }

    @Override
    protected List<DB.Event> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        List<DB.Event> events = new ArrayList<>(10);

        // Begin parsing
        boolean complete_parsing = false;
        boolean isParsingEvents = false;
        DB.Event current_event = null;
        while (!complete_parsing)
        {
            int next_result = XmlPullParser.END_TAG;

            try{ next_result = parser.next(); }
            catch (XmlPullParserException ignored){}

            String tag = parser.getName();

            switch (next_result)
            {
                case XmlPullParser.END_TAG:
                    if ("ul".equals(tag) && isParsingEvents)
                        complete_parsing = true;
                    break;
                case XmlPullParser.START_TAG:
                    // This is what we look for: Now parse the event
                    if ("ul".equals(tag) && "accordion events".equals(parser.getAttributeValue(null, "class")))
                    {
                        isParsingEvents = true;
                    }
                    else if ("a".equals(tag) && "event-title".equals(parser.getAttributeValue(null, "class")))
                    {
                        current_event = new DB.Event();
                    }
                    else if ("h2".equals(tag) && current_event != null)
                    {
                        current_event.title = getTextInsideTags(parser, "h2");
                    }
                    else if ("h3".equals(tag) && current_event != null)
                    {
                        DateFormat format = new SimpleDateFormat("E, MMM dd, yyyy", Locale.US);
                        ParsePosition pos = new ParsePosition(6);
                        String date = getTextInsideTags(parser, "h3");
                        current_event.initialDate = format.parse(date, pos);

                        for (; pos.getIndex() < date.length() && Character.isWhitespace(date.charAt(pos.getIndex()));

                        pos.setIndex(pos.getIndex()+1));

                        if(date.charAt(pos.getIndex()) == '-')
                        {
                            pos.setIndex(pos.getIndex()+2);
                            current_event.endDate = format.parse(date, pos);
                        }
                        else if(date.substring(pos.getIndex(), pos.getIndex()+6).equals("Time: "))
                        {
                            format = new SimpleDateFormat("K:mm a", Locale.US);
                            pos.setIndex(pos.getIndex() + 6);
                            Date time = format.parse(date, pos);
                            if(time != null)
                            {
                                current_event.initialDate.setHours(time.getHours());
                                current_event.initialDate.setMinutes(time.getMinutes());
                            }
                        }
                    }
                    else if ("b".equals(tag) && current_event != null && parser.next() == XmlPullParser.TEXT)
                    {
                        if("Place:".equals(parser.getText().trim()))
                        {
                            if(parser.next() == XmlPullParser.END_TAG && parser.next() == XmlPullParser.START_TAG && "a".equals(parser.getName()))
                            {
                                current_event.placeURL = new URL(parser.getAttributeValue(null, "href"));
                                parser.next();
                            }
                            if(parser.getEventType() == XmlPullParser.TEXT)
                                current_event.placeName = parser.getText();

                            // Address
                            while(parser.next() != XmlPullParser.START_TAG || !"b".equals(parser.getName()));
                            if(parser.next() == XmlPullParser.TEXT && "Address:".equals(parser.getText().trim()))
                            {
                                StringBuilder stringBuilder = new StringBuilder();
                                while (parser.next() != XmlPullParser.START_TAG || "br".equals(parser.getName()))
                                    if (parser.getEventType() == XmlPullParser.TEXT)
                                        stringBuilder.append(parser.getText().trim());
                                    else if(parser.getEventType() == XmlPullParser.START_TAG)
                                        stringBuilder.append(", ");

                                current_event.address = stringBuilder.toString().replace(",,", ",");
                                if(current_event.address.endsWith(", "))
                                    current_event.address = current_event.address.substring(0, current_event.address.length()-2);
                                if(current_event.address.startsWith(", "))
                                    current_event.address = current_event.address.substring(2);

                                if("b".equals(parser.getName()) && parser.next() == XmlPullParser.TEXT && "Phone Number:".equals(parser.getText().trim()))
                                {
                                    stringBuilder = new StringBuilder();
                                    while (parser.next() != XmlPullParser.START_TAG || "br".equals(parser.getName()))
                                        if (parser.getEventType() == XmlPullParser.TEXT)
                                            stringBuilder.append(parser.getText());

                                    current_event.phoneNumber = stringBuilder.toString().trim();

                                    if("b".equals(parser.getName()) && parser.next() == XmlPullParser.TEXT && "Event Type:".equals(parser.getText().trim()))
                                    {
                                        stringBuilder = new StringBuilder();
                                        while (parser.next() != XmlPullParser.START_TAG)
                                            if (parser.getEventType() == XmlPullParser.TEXT)
                                                stringBuilder.append(parser.getText());

                                        current_event.eventType = stringBuilder.toString().trim();
                                        parser.next();
                                        parser.next();

                                        // Last
                                        stringBuilder = new StringBuilder();
                                        boolean is_div = false;
                                        while (!is_div)
                                        {
                                            // Handle errors in Sanderson Website events html: Ignore them
                                            next_result = -1;
                                            while(next_result == -1)
                                            {
                                                try{ next_result = parser.next(); }
                                                catch (XmlPullParserException ignored){}
                                            }

                                            switch (next_result)
                                            {
                                                case XmlPullParser.END_TAG:
                                                    if ("div".equals(parser.getName()) || "li".equals(parser.getName()))
                                                    {
                                                        is_div = true;
                                                    }
                                                    else
                                                        stringBuilder.append("</").append(parser.getName()).append(">");
                                                    break;
                                                case XmlPullParser.START_TAG:
                                                    if ("li".equals(parser.getName()))
                                                    {
                                                        is_div = true;
                                                    }
                                                    else
                                                    {
                                                        stringBuilder.append("<").append(parser.getName());
                                                        for (int i = 0; i < parser.getAttributeCount(); i++)
                                                        {
                                                            stringBuilder.append(" ").append(parser.getAttributeName(i));
                                                            stringBuilder.append("=\"");
                                                            stringBuilder.append(parser.getAttributeValue(i));
                                                            stringBuilder.append("\"");
                                                        }
                                                        stringBuilder.append(">");
                                                    }
                                                    break;
                                                case XmlPullParser.TEXT:
                                                    stringBuilder.append(parser.getText());
                                                    break;
                                            }
                                        }

                                        current_event.htmlRemain = stringBuilder.toString().trim();
                                        if(current_event.isValid())
                                            events.add(current_event);

                                        current_event = null;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    complete_parsing = true;
                    break;
            }
        }

        return events;
    }

    @Override
    protected void updateDB(List<DB.Event> events)
    {
        DB.Event.updateEvents(events);
    }
}
// Retrieve the tweets
class DownloadTweetsWorker extends DownloadAndParseWorker<DB.Tweet>
{
    DownloadTweetsWorker()
    {
        super(null);
    }

    @Override
    protected List<DB.Tweet> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        return null;
    }

    @Override
    protected void updateDB(List<DB.Tweet> objects){}

    // Do the long-running work in here
    protected int doWork()
    {
        int state = DownloadParseSaveTask.STATE_SUCCESS;

        try
        {
            List<twitter4j.Status> tweets;

            // Twitter client configuration
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TwitterAPISecrets.CONSUMER_KEY).setOAuthConsumerSecret(TwitterAPISecrets.CONSUMER_SECRET);
            // TODO: Use guest authentication instead of application
            builder.setApplicationOnlyAuthEnabled(true).setDebugEnabled(false).setGZIPEnabled(true);

            Twitter twitter = new TwitterFactory(builder.build()).getInstance();
            twitter.getOAuth2Token();

            // Get the tweets
            long lastTweetID = DB.Tweet.getLastTweetID();
            if(lastTweetID <= 0)
                tweets = twitter.getUserTimeline("BrandSanderson");
            else
                tweets = twitter.getUserTimeline("BrandSanderson", new Paging(lastTweetID));

            if (tweets != null)
            {
                long brandonID = 28187205;
                Hashtable<Long, DB.Tweet> findTweet = new Hashtable<>(tweets.size()*16, 0.25f);
                List<DB.Tweet> dbTweets = new ArrayList<>(tweets.size());
                for (twitter4j.Status tweet : tweets)
                {
                    // Create the tweet
                    if(tweet.isRetweet())
                        tweet = tweet.getRetweetedStatus();

                    long tweetID = tweet.getId();
                    // TODO: Include the tweets in the DB
                    if(findTweet.get(tweetID) == null)// This eliminate tweets already in the replies tree
                    {
                        DB.Tweet dbTweet = new DB.Tweet(tweetID, getTweetText(tweet), tweet.getCreatedAt(), false, tweet.getUser().getName(), tweet.getUser().getBiggerProfileImageURLHttps());
                        InternetHelper.getRemoteFile(tweet.getUser().getBiggerProfileImageURLHttps());
                        dbTweets.add(dbTweet);
                        findTweet.put(tweetID, dbTweet);

                        // Traverse the tree of the replies tweets
                        twitter4j.Status treeTweet = tweet;
                        while (treeTweet != null && treeTweet.getInReplyToStatusId() >= 0)
                        {
                            try
                            {
                                long id = treeTweet.getInReplyToStatusId();
                                long userID = treeTweet.getUser().getId();
                                treeTweet = null;
                                if(findTweet.get(id) == null || brandonID == userID)
                                    treeTweet = twitter.showStatus(id);
                                else// Remove duplicates. Not sure why they appear, but the difference of the text is a dot at the end.
                                    dbTweets.remove(dbTweets.size()-1);
                            }
                            catch (Exception ignore){}

                            if (treeTweet != null)
                            {
                                findTweet.put(treeTweet.getId(), dbTweet);
                                StringBuilder replyBuilder = new StringBuilder();

                                replyBuilder.append("<blockquote>");

                                // Profile image
                                replyBuilder.append("<img src=\"");
                                InternetHelper.getRemoteFile(treeTweet.getUser().getBiggerProfileImageURLHttps());
                                replyBuilder.append(treeTweet.getUser().getBiggerProfileImageURLHttps());
                                replyBuilder.append("\"/>&nbsp;<b>");

                                // Username - date
                                replyBuilder.append(treeTweet.getUser().getName());
                                replyBuilder.append("</b> @");
                                replyBuilder.append(treeTweet.getUser().getScreenName());
                                replyBuilder.append(" - ");
                                replyBuilder.append(TwitterFragment.showDateFormat.format(treeTweet.getCreatedAt()));

                                // Tweet text
                                replyBuilder.append("<br/>");
                                replyBuilder.append(getTweetText(treeTweet));

                                // Remaining
                                replyBuilder.append(dbTweet.htmlReply);
                                replyBuilder.append("</blockquote>");

                                dbTweet.htmlReply = replyBuilder.toString();
                            }
                        }
                    }
                }
                DB.Tweet.updateTwitter(dbTweets);
            }
        }
        catch (Exception e)
        {
            Logger.reportError(e.toString());
            state = DownloadParseSaveTask.STATE_ERROR_PARSING;
        }

        return state;
    }
    private String getTweetText(twitter4j.Status tweet)
    {
        SpannableStringBuilder text = new SpannableStringBuilder(tweet.getText());

        URLEntity[] urls = tweet.getURLEntities();
        MediaEntity[] medias = tweet.getMediaEntities();

        for (MediaEntity media : medias)
            if ("photo".equals(media.getType()))
            {
                InternetHelper.getRemoteFile(media.getMediaURLHttps());
                text.setSpan(new ImageSpan(null, media.getMediaURLHttps()), media.getStart(), media.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        for (URLEntity url : urls)
        {
            text.setSpan(new URLSpan(url.getURL()), url.getStart(), url.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            text.replace(url.getStart(), url.getEnd(), url.getExpandedURL());
        }

        return Html.toHtml(text);
    }
}
// Retrieve the Tor Reread Posts
class DownloadTorRereadPostsWorker extends DownloadAndParseWorker<DB.TorRereadPost>
{
    DownloadTorRereadPostsWorker()
    {
        super(WOR_REREAD);
    }

    @Override
    protected List<DB.TorRereadPost> downloadParse(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        List<DB.TorRereadPost> blogPosts = new ArrayList<>(10);

        // Begin parsing
        boolean complete_parsing = false;
        DB.TorRereadPost current_post = null;
        String commentsRSS = null;
        while (!complete_parsing)
        {
            int next_result = XmlPullParser.END_TAG;

            try{ next_result = parser.next(); }
            catch (XmlPullParserException ignored){}

            String tag = parser.getName();

            switch (next_result)
            {
                case XmlPullParser.END_TAG:
                    if ("item".equals(tag) && current_post != null)
                    {
                        if(current_post.isValid())
                            blogPosts.add(current_post);

                        current_post = null;
                        commentsRSS = null;
                    }
                    break;
                case XmlPullParser.START_TAG:
                    if ("item".equals(tag))
                    {
                        current_post = new DB.TorRereadPost();
                    }
                    else if ("title".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.title = parser.getText();
                        if(current_post.title.startsWith("Words of Radiance Reread: "))
                            current_post.title = current_post.title.substring(26);
                    }
                    else if ("link".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.link = new URL(parser.getText());
                    }
                    else if ("pubDate".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);
                        try{ current_post.publicationDate = format.parse(parser.getText());}
                        catch (ParseException ignored){ }
                    }
                    else if ("dc".equals(parser.getPrefix()) && "creator".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.creator = parser.getText();
                    }
                    else if ("guid".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        // TODO: Stop parsing if we are in some old post
                        String guid_url = parser.getText();
                        int len = 0;
                        for (; len < guid_url.length(); len++)
                            if(!Character.isDigit(guid_url.charAt(guid_url.length() - 1 - len)))
                                break;

                        guid_url = guid_url.substring(guid_url.length() - len, guid_url.length());
                        current_post.guid = Integer.parseInt(guid_url);
                    }
                    else if ("content".equals(parser.getPrefix()) && "encoded".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.html_content = parser.getText();
                        parseImages(current_post.html_content);
                    }
                    else if ("wfw".equals(parser.getPrefix()) && "commentRss".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        commentsRSS = parser.getText();
                    }
                    else if ("slash".equals(parser.getPrefix()) && "comments".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                    {
                        current_post.totalNumComments = Integer.parseInt(parser.getText());
                        int numDBComments = current_post.getNumComments();

                        // Read comments if there are new ones
                        if(current_post.totalNumComments != numDBComments && commentsRSS != null)
                            current_post.comments = getComments(commentsRSS);
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    complete_parsing = true;
                    break;
            }
        }

        return blogPosts;
    }

    private ArrayList<DB.TorRereadComment> getComments(String commentsRSS)
    {
        InputStream in = null;
        ArrayList<DB.TorRereadComment> blogPosts = new ArrayList<>(50);

        try
        {
            in = InternetHelper.connectUrl(commentsRSS);

            if(in != null)
            {
                MyXMLParser parser = new MyXMLParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                parser.setInput(in, null);

                // Begin parsing
                boolean complete_parsing = false;
                DB.TorRereadComment current_post = null;
                while (!complete_parsing)
                {
                    int next_result = XmlPullParser.END_TAG;

                    try{ next_result = parser.next(); }
                    catch (XmlPullParserException ignored){}

                    String tag = parser.getName();

                    switch (next_result)
                    {
                        case XmlPullParser.END_TAG:
                            if ("item".equals(tag) && current_post != null)
                            {
                                if(current_post.isValid())
                                    blogPosts.add(current_post);

                                current_post = null;
                            }
                            break;
                        case XmlPullParser.START_TAG:
                            if ("item".equals(tag))
                            {
                                current_post = new DB.TorRereadComment();
                            }
                            else if ("pubDate".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                            {
                                DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);
                                try{ current_post.publicationDate = format.parse(parser.getText());}
                                catch (ParseException ignored){ }
                            }
                            else if ("dc".equals(parser.getPrefix()) && "creator".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                            {
                                current_post.creator = parser.getText();
                            }
                            else if ("guid".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                            {
                                String guid_url = parser.getText();
                                int len = 0;
                                for (; len < guid_url.length(); len++)
                                    if(!Character.isDigit(guid_url.charAt(guid_url.length() - 1 - len)))
                                        break;

                                guid_url = guid_url.substring(guid_url.length() - len, guid_url.length());
                                current_post.guid = Integer.parseInt(guid_url);
                            }
                            else if ("content".equals(parser.getPrefix()) && "encoded".equals(tag) && current_post != null && parser.next() == XmlPullParser.TEXT)
                            {
                                current_post.html_content = parser.getText();
                                parseImages(current_post.html_content);
                            }
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            complete_parsing = true;
                            break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Logger.reportError(e.toString());
        }
        finally
        {
            try { if(in != null) in.close(); }
            catch (Exception ignored){}
        }

        return blogPosts;
    }

    @Override
    protected void updateDB(List<DB.TorRereadPost> blogPosts)
    {
        DB.TorRereadPost.updateBlogPosts(blogPosts);
    }
}


/**
 * Here begin Downloading and Parsing code
 */
abstract class DownloadParseSaveTask extends AsyncTask<Object, Integer, Integer>
{
    /**
     * The notification interface to download files.
     */
    protected static InternetHelper.DownloadFileNotification downloadNotif;

    protected LabelStatus statusLabel;
    private DownloadParseSaveTask next;
    private DownloadAndParseWorker currentWorker;

    static final int STATE_SUCCESS = 0;
    static final int STATE_ERROR_URL = 1;
    static final int STATE_ERROR_PARSING = 2;
    static final int STATE_ERROR_DB = 3;

    public DownloadParseSaveTask(DownloadAndParseWorker aCurrentWorker, LabelStatus aStatus, DownloadParseSaveTask aNext)
    {
        statusLabel = aStatus;
        next = aNext;
        currentWorker = aCurrentWorker;
    }

    protected abstract void onCompleteSuccessfully();

    // After we begin
    protected void onPreExecute ()
    {
        if(statusLabel != null)
            statusLabel.setUpdateState(true);
    }

    // Do the long-running work in here
    protected Integer doInBackground(Object... unused)
    {
        return currentWorker.doWork();
    }

    // This is called each time you call publishProgress()
    protected void onProgressUpdate(Integer... progress){}

    // This is called when doInBackground() is finished
    protected void onPostExecute(Integer state)
    {
        if(statusLabel != null)
        {
            statusLabel.setUpdateState(false);

            if (state != STATE_SUCCESS)
                statusLabel.setNumberState(Html.fromHtml("<em>fail</em>"), false);
            else
                onCompleteSuccessfully();
        }

        if(next != null)
            next.execute((Object)null);
        else// Finish downloading and parsing data
        {
            DB.updateLastDate();
            InternetHelper.downloadAllFiles(downloadNotif, true);
        }
    }
}
interface UpdateGUIProgressBook
{
    void updateGUIProgressBook(List<DB.ProjectsStatus> progressBookList);
}
// Retrieve the progress of books currently working on
class DownloadCurrentProjectsTask extends DownloadParseSaveTask
{
    private final UpdateGUIProgressBook guiUpdate;

    public DownloadCurrentProjectsTask(LabelStatus currentProjects, LabelStatus blogPostLabel, LabelStatus eventsLabel, LabelStatus twitterLabel, LabelStatus worRereadLabel,
                                       InternetHelper.DownloadFileNotification aDownloadNotif, UpdateGUIProgressBook aGUIUpdate)
    {
        super(new DownloadCurrentProjectsWorker(), currentProjects, new DownloadBlogPostsTask(blogPostLabel, eventsLabel, twitterLabel, worRereadLabel));

        downloadNotif = aDownloadNotif;
        guiUpdate = aGUIUpdate;
    }
    @Override
    protected void onCompleteSuccessfully()
    {
        if(guiUpdate != null)
            guiUpdate.updateGUIProgressBook(DB.ProjectsStatus.getCurrentProjects());
    }
}
// Retrieve the blog posts
class DownloadBlogPostsTask extends DownloadParseSaveTask
{
    public DownloadBlogPostsTask(LabelStatus blogPostLabel, LabelStatus eventsLabel, LabelStatus twitterLabel, LabelStatus worRereadLabel)
    {
        super(new DownloadBlogPostsWorker(), blogPostLabel, new DownloadEventsTask(eventsLabel, twitterLabel, worRereadLabel));
    }

    @Override
    protected void onCompleteSuccessfully()
    {
        DB.LabelStatus statusNumbers = DB.BlogPost.getStatus();
        statusLabel.setNumberState(statusNumbers.countUnread, statusNumbers.total);
        MainActivity.setBadgeNumber(MainActivity.APP_STATE_BLOG, statusNumbers.countUnread);
    }
}
// Retrieve the events
class DownloadEventsTask extends DownloadParseSaveTask
{
    public DownloadEventsTask(LabelStatus eventsLabel, LabelStatus twitterLabel, LabelStatus worRereadLabel)
    {
        super(new DownloadEventsWorker(), eventsLabel, new DownloadTweetsTask(twitterLabel, worRereadLabel));
    }

    @Override
    protected void onCompleteSuccessfully()
    {
        DB.LabelStatus status = DB.Event.getStatus();
        statusLabel.setNumberState(status.countUnread, status.total);
        MainActivity.setBadgeNumber(MainActivity.APP_STATE_EVENTS, status.countUnread);
    }
}
// Retrieve the tweets
class DownloadTweetsTask extends DownloadParseSaveTask
{
    public DownloadTweetsTask(LabelStatus twitterLabel, LabelStatus worRereadLabel)
    {
        super(new DownloadTweetsWorker(), twitterLabel, new DownloadTorRereadPostsTask(worRereadLabel));
    }

    @Override
    protected void onCompleteSuccessfully()
    {
        DB.LabelStatus status = DB.Tweet.getStatus();
        statusLabel.setNumberState(status.countUnread, status.total);
        MainActivity.setBadgeNumber(MainActivity.APP_STATE_TWITTER, status.countUnread);
    }
}
// Retrieve the Tor Reread Posts
class DownloadTorRereadPostsTask extends DownloadParseSaveTask
{
    public DownloadTorRereadPostsTask(LabelStatus worRereadLabel)
    {
        super(new DownloadTorRereadPostsWorker(), worRereadLabel, null);
    }

    @Override
    protected void onCompleteSuccessfully()
    {
        StringBuilder builder = new StringBuilder();
        boolean useHighlight = DB.TorRereadPost.getStatus(builder);
        statusLabel.setNumberState(Html.fromHtml(builder.toString()), useHighlight);

        MainActivity.setBadgeNumber(MainActivity.APP_STATE_WOR, DB.TorRereadPost.getTotalUnread());
    }
}

