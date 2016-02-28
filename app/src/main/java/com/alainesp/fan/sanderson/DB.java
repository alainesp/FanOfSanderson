// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handle all Database interaction
 */
public final class DB
{
    private static final int DB_FALSE = 0;
    private static final int DB_TRUE = 1;
    public static Context currentContext;
    /**
     * Date format used by the Database
     */
    private static final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // Config
    private static final int CONFIG_LAST_UPDATE_DATE = 1;
    private static final String SQL_CREATE_CONFIG =
            "CREATE TABLE IF NOT EXISTS Config"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Data TEXT NOT NULL"
            + " )";
    // Current projects
    private static final String SQL_CREATE_PROJECTS_STATUS =
            "CREATE TABLE IF NOT EXISTS ProjectsStatus"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Title TEXT NOT NULL," +
                    "Percent INTEGER NOT NULL,"+
                    "DateChecked DATETIME NOT NULL DEFAULT (datetime('now'))"
            + " )";
    private static final String SQL_CREATE_ACTIVE_PROJECTS_STATUS =
            "CREATE TABLE IF NOT EXISTS ActiveProjectsStatus"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "FOREIGN KEY(_id) REFERENCES ProjectsStatus(_id)"
            + " )";

    // Blog Posts
    private static final String SQL_CREATE_BLOG_POSTS =
            "CREATE TABLE IF NOT EXISTS BlogPosts"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Title TEXT NOT NULL," +
                    "Link TEXT NOT NULL," +
                    "PublicationDate DATETIME NOT NULL," +
                    "Creator TEXT NOT NULL," +
                    "Content TEXT NOT NULL," +
                    "Readed INTEGER NOT NULL DEFAULT 0"
            + " )";
    private static final String SQL_CREATE_CATEGORY =
            "CREATE TABLE IF NOT EXISTS Category"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Name TEXT NOT NULL"
            + " )";
    private static final String SQL_CREATE_CATEGORY_BLOG =
            "CREATE TABLE IF NOT EXISTS CategoryBlog"+
            " (" +
                    "CategoryID INTEGER," +
                    "BlogPostID INTEGER," +
                    "FOREIGN KEY(CategoryID) REFERENCES Category(_id)," +
                    "FOREIGN KEY(BlogPostID) REFERENCES BlogPosts(_id)"
            + " )";
    // Events
    private static final String SQL_CREATE_EVENTS =
            "CREATE TABLE IF NOT EXISTS Events"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Title TEXT NOT NULL," +
                    "InitialDate DATETIME NOT NULL," +
                    "EndDate DATETIME," +
                    "PlaceName TEXT NOT NULL," +
                    "PlaceURL TEXT NOT NULL," +
                    "Address TEXT NOT NULL," +
                    "PhoneNumber TEXT NOT NULL," +
                    "EventType TEXT NOT NULL," +
                    "HtmlRemain TEXT NOT NULL," +
                    "Readed INTEGER NOT NULL DEFAULT 0"
            + " )";
    // Twitter
    private static final String SQL_CREATE_TWEETS =
            "CREATE TABLE IF NOT EXISTS Tweets"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Text TEXT NOT NULL," +
                    "Date DATETIME NOT NULL," +
                    "Username TEXT NOT NULL," +
                    "UserImageUrl TEXT NOT NULL," +
                    "HtmlReply TEXT NOT NULL," +
                    "Readed INTEGER NOT NULL DEFAULT 0"
            + " )";
    // Tor Reread Blog Posts
    private static final String SQL_CREATE_TOR_REREAD_BLOG_POSTS =
            "CREATE TABLE IF NOT EXISTS TorRereadPosts"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Title TEXT NOT NULL," +
                    "Link TEXT NOT NULL," +
                    "PublicationDate DATETIME NOT NULL," +
                    "Creator TEXT NOT NULL," +
                    "Content TEXT NOT NULL," +
                    "NumComments INTEGER NOT NULL DEFAULT 0," +
                    "Readed INTEGER NOT NULL DEFAULT 0"
            + " )";
    private static final String SQL_CREATE_TOR_REREAD_COMMENTS =
            "CREATE TABLE IF NOT EXISTS TorRereadComments"+
            " (" +
                    "_id INTEGER," +
                    "TorRereadPost INTEGER," +
                    "PublicationDate DATETIME NOT NULL," +
                    "Creator TEXT NOT NULL," +
                    "Content TEXT NOT NULL," +
                    "Readed INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY(_id, TorRereadPost)," +
                    "FOREIGN KEY(TorRereadPost) REFERENCES TorRereadPosts(_id)"
            + " )";
    // Files
    private static final String SQL_CREATE_FILES =
            "CREATE TABLE IF NOT EXISTS Files"+
            " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "Url TEXT NOT NULL," +
                    "State INTEGER NOT NULL DEFAULT 0"
            + " )";

    /**
     * Helper class to handle DB creating and upgrade
     */
    static class DbHelper extends SQLiteOpenHelper
    {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "data.db";

        public DbHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db)
        {
            db.beginTransaction();

            db.execSQL(DB.SQL_CREATE_CONFIG);
            db.execSQL(DB.SQL_CREATE_PROJECTS_STATUS);
            db.execSQL(DB.SQL_CREATE_ACTIVE_PROJECTS_STATUS);
            db.execSQL(DB.SQL_CREATE_BLOG_POSTS);
            db.execSQL(DB.SQL_CREATE_CATEGORY);
            db.execSQL(DB.SQL_CREATE_CATEGORY_BLOG);
            db.execSQL(DB.SQL_CREATE_EVENTS);
            db.execSQL(DB.SQL_CREATE_TWEETS);
            db.execSQL(DB.SQL_CREATE_TOR_REREAD_BLOG_POSTS);
            db.execSQL(DB.SQL_CREATE_TOR_REREAD_COMMENTS);
            db.execSQL(DB.SQL_CREATE_FILES);

            try {db.setTransactionSuccessful();}
            finally {db.endTransaction();}
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }
    }

    /**
     * Get the number of seconds since last update or -1 if no update
     * @return The number of seconds
     */
    public static long getLastUpdateSpan()
    {
        long num_seconds = -1;

        DbHelper mDbHelper = new DbHelper(currentContext);

        // Gets the data repository in read mode
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT strftime('%s','now') - strftime('%s',Data) FROM Config WHERE _id=" + CONFIG_LAST_UPDATE_DATE, null);
        if(c.moveToFirst())
            num_seconds = c.getLong(0);

        c.close();
        db.close();

        return num_seconds;
    }
    /**
     * Mark 'now' as the last time the DB data was updated
     */
    public static void updateLastDate()
    {
        DbHelper mDbHelper = new DbHelper(currentContext);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.execSQL("INSERT OR REPLACE INTO Config (_id,Data) VALUES (" + CONFIG_LAST_UPDATE_DATE + ",datetime('now'))");

        db.close();
    }

    /**
     * Utility for labels in Summary fragment
     */
    public static class LabelStatus
    {
        public int countUnread = 0;
        public int total = 0;
    }

    /**
     * Projects Status. Obtained from Sanderson website
     */
    public static class ProjectsStatus
    {
        /**
         * The title of the project
         */
        public String title;
        /**
         * The percent of completion
         */
        public int percent;
        /**
         * If the user is notified of this change
         */
        public final boolean viewed;

        public ProjectsStatus()
        {
            title = null;
            percent = -1;
            viewed = true;
        }

        public ProjectsStatus(String aTitle, int aPercent, boolean aViewed)
        {
            title = aTitle;
            percent = aPercent;
            viewed = aViewed;
        }

        /**
         * If this status had all the information necessary
         * @return If is valid
         */
        public boolean isValid()
        {
            return title != null && percent >= 0 && percent <= 100;
        }

        /**
         * Update the DB with projects data
         * @param progressBookList The list of projects with the data
         */
        public static void updateCurrentProjects(List<ProjectsStatus> progressBookList)
        {
            if(progressBookList != null && progressBookList.size() > 0)
            {
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                db.beginTransaction();

                //  Compile SQL statements
                SQLiteStatement insertProjectsStatus = db.compileStatement("INSERT INTO ProjectsStatus (Title,Percent) VALUES (?,?)");
                SQLiteStatement selectProjectsStatus = db.compileStatement("SELECT _id FROM ProjectsStatus WHERE Title=? AND Percent=?");
                SQLiteStatement insertActiveProjectsStatus = db.compileStatement("INSERT INTO ActiveProjectsStatus (_id) VALUES (?)");

                db.delete("ActiveProjectsStatus", null, null);

                // Iterate all books
                for (int i = 0; i < progressBookList.size(); i++)
                {
                    long rowId;
                    // Check if already exits
                    selectProjectsStatus.bindString(1, progressBookList.get(i).title);
                    selectProjectsStatus.bindLong(2, progressBookList.get(i).percent);
                    try { rowId = selectProjectsStatus.simpleQueryForLong(); }
                    catch (SQLiteDoneException e)// Not exists -> Insert new data
                    {
                        insertProjectsStatus.bindString(1, progressBookList.get(i).title);
                        insertProjectsStatus.bindLong(2, progressBookList.get(i).percent);

                        rowId = insertProjectsStatus.executeInsert();
                    }
                    // Set active
                    insertActiveProjectsStatus.bindLong(1, rowId);
                    insertActiveProjectsStatus.executeInsert();
                }

                insertProjectsStatus.close();
                selectProjectsStatus.close();
                insertActiveProjectsStatus.close();

                try { db.setTransactionSuccessful(); }
                finally { db.endTransaction(); }

                db.close();
            }
        }

        /**
         * Get the current working Projects
         * @return The projects
         */
        public static List<ProjectsStatus> getCurrentProjects()
        {
            List<ProjectsStatus> progressBookList = new ArrayList<>(5);

            // Get the database
            DbHelper mDbHelper = new DbHelper(currentContext);
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT Title,Percent,strftime('%s','now') - strftime('%s',DateChecked) FROM ProjectsStatus INNER JOIN ActiveProjectsStatus ON ActiveProjectsStatus._id==ProjectsStatus._id", null);

            // Read all Active books
            boolean had_data = c.moveToFirst();
            while (had_data)
            {
                // Is viewed if checked more than one day ago
                progressBookList.add(new ProjectsStatus(c.getString(0), c.getInt(1), c.getLong(2) > 60*60*24));
                had_data = c.moveToNext();
            }
            c.close();

            db.close();
            return progressBookList;
        }
    }

    /**
     * Blog post by Sanderson.
     * Obtained from Sanderson website main RSS feed.
     */
    public static class BlogPost
    {
        public String title = null;
        public URL link = null;
        public Date publicationDate = null;
        public String creator = "";
        final List<String> categories = new ArrayList<>(5);
        public int guid = -1;
        public String html_content = null;
        public boolean readed = false;

        public BlogPost(){}
        public BlogPost(int ID, String aTitle, String aDate, boolean isReaded)
        {
            guid = ID;
            title = aTitle;
            readed = isReaded;
            try{ publicationDate = dbDateFormat.parse(aDate); }
            catch (ParseException ignored){ }
        }

        /**
         * Get the categories of this blog post as a String
         * @return The categories
         */
        public String getCategories()
        {
            StringBuilder categoriesBuilder = new StringBuilder();
            for (int i = 0; i < categories.size(); i++)
            {
                if (i > 0)
                    categoriesBuilder.append(", ");
                categoriesBuilder.append(categories.get(i));
            }
            return categoriesBuilder.toString();
        }

        /**
         * Add a category. Used when parsing the RSS feed.
         * @param category A category to add
         */
        public void addCategory(String category)
        {
            categories.add(category);
        }

        /**
         * If this blog post contains all necessary data.
         * @return If is valid
         */
        public boolean isValid()
        {
            return (title != null && link != null && publicationDate != null && categories.size() > 0 && guid >= 0 && html_content != null);
        }

        /**
         * Update the DB with a list of blog posts
         * @param blogPosts The list of blog posts
         */
        public static void updateBlogPosts(List<BlogPost> blogPosts)
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.beginTransaction();

            //  Compile SQL statements
            SQLiteStatement insertBlogPost = db.compileStatement("INSERT INTO BlogPosts (_id,Title,Link,PublicationDate,Creator,Content) VALUES (?,?,?,datetime(?),?,?)");
            SQLiteStatement selectBlogPost = db.compileStatement("SELECT Readed FROM BlogPosts WHERE _id=?");

            SQLiteStatement insertCategory = db.compileStatement("INSERT INTO Category (Name) VALUES (?)");
            SQLiteStatement selectCategory = db.compileStatement("SELECT _id FROM Category WHERE Name=?");

            SQLiteStatement insertCategoryBlog = db.compileStatement("INSERT INTO CategoryBlog (CategoryID,BlogPostID) VALUES (?,?)");

            // Iterate all blog posts
            for (int i = 0; i < blogPosts.size(); i++)
            {
                // Check if already exits
                selectBlogPost.bindLong(1, blogPosts.get(i).guid);
                try{ blogPosts.get(i).readed = selectBlogPost.simpleQueryForLong() == DB_TRUE; }
                catch (SQLiteDoneException e)// Not exists -> Insert new data
                {
                    insertBlogPost.bindLong(1, blogPosts.get(i).guid);
                    insertBlogPost.bindString(2, blogPosts.get(i).title);
                    insertBlogPost.bindString(3, blogPosts.get(i).link.toString());
                    insertBlogPost.bindString(4, dbDateFormat.format(blogPosts.get(i).publicationDate));
                    insertBlogPost.bindString(5, blogPosts.get(i).creator);
                    insertBlogPost.bindString(6, blogPosts.get(i).html_content);

                    insertBlogPost.executeInsert();
                    blogPosts.get(i).readed = false;

                    // Set Categories
                    List<String> categories = blogPosts.get(i).categories;
                    for (int j = 0; j < categories.size(); j++)
                    {
                        long categoryID;
                        selectCategory.bindString(1, categories.get(j));
                        try{ categoryID = selectCategory.simpleQueryForLong(); }
                        catch (SQLiteDoneException ee)// Not exists -> Insert new data
                        {
                            insertCategory.bindString(1, categories.get(j));
                            categoryID = insertCategory.executeInsert();
                        }
                        insertCategoryBlog.bindLong(1, categoryID);
                        insertCategoryBlog.bindLong(2, blogPosts.get(i).guid);
                        insertCategoryBlog.executeInsert();
                    }
                }
            }

            insertBlogPost.close();
            selectBlogPost.close();
            insertCategory.close();
            selectCategory.close();
            insertCategoryBlog.close();

            try {db.setTransactionSuccessful();}
            finally {db.endTransaction();}

            db.close();
        }

        /**
         * Get general information about blog posts
         * @return The total and read number of blog posts
         */
        public static LabelStatus getStatus()
        {
            LabelStatus status = new LabelStatus();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM BlogPosts WHERE Readed=0", null);
            c.moveToFirst();
            status.countUnread = c.getInt(0);
            c.close();
            // Count all
            c = db.rawQuery("SELECT Count(*) FROM BlogPosts", null);
            c.moveToFirst();
            status.total = c.getInt(0);
            c.close();

            db.close();

            return status;
        }

        /**
         * Get all blog posts from DB
         * @return A list of blog posts with minimal data
         */
        public static List<BlogPost> getBlogPostsFromDB()
        {
            ArrayList<BlogPost> posts = new ArrayList<>();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // TODO: Don't return all posts, only the latest
            Cursor c = db.rawQuery("SELECT _id,Title,PublicationDate,Readed FROM BlogPosts ORDER BY PublicationDate DESC", null);
            boolean had_more = c.moveToFirst();
            while(had_more)
            {
                posts.add(new BlogPost(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3)==DB_TRUE));
                had_more = c.moveToNext();
            }
            c.close();

            // Load the categories
            for (BlogPost post : posts)
            {
                c = db.rawQuery("SELECT DISTINCT Category.Name FROM Category INNER JOIN CategoryBlog ON Category._id=CategoryBlog.CategoryID WHERE BlogPostID="+post.guid, null);
                had_more = c.moveToFirst();
                while(had_more)
                {
                    post.categories.add(c.getString(0));
                    had_more = c.moveToNext();
                }
                c.close();
            }

            db.close();

            return posts;
        }

        /**
         * Get the db id from a link
         * @param link The link of the blog post
         * @return The db id of the post or -1 if not found
         */
        public static long getBlogPostIDFromLink(String link)
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            SQLiteStatement selectBlogPost = db.compileStatement("SELECT _id FROM BlogPosts WHERE Link=?");
            selectBlogPost.bindString(1, link);

            long id = -1;
            try{ id = selectBlogPost.simpleQueryForLong(); }
            catch (SQLiteDoneException ignored){}

            selectBlogPost.close();
            db.close();

            return id;
        }

        /**
         * Show the blog post to the user.
         * @param context The context to use.
         */
        public void showToUser(Context context)
        {
            if(!readed)
                MainActivity.decBadgeNumber(MainActivity.APP_STATE_BLOG);
            loadContentFromDB();
            Intent intent = new Intent(context, ReadBlogPostActivity.class);
            BlogPostsFragment.readingPost = this;
            context.startActivity(intent);
        }

        /**
         * Load content of a blog post to be read by the user
         */
        private void loadContentFromDB()
        {
            if(html_content == null)
            {
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getReadableDatabase();

                Cursor c = db.rawQuery("SELECT Content FROM BlogPosts WHERE _id=" + guid, null);
                if (c.moveToFirst())
                    html_content = c.getString(0);

                c.close();
                db.close();
            }
            if(!readed)
            {
                readed=true;
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("Readed", DB_TRUE);
                db.update("BlogPosts", values, "_id="+guid, null);

                db.close();
            }
        }
    }

    /**
     * Events in which Sanderson will participate.
     * Obtained from Sanderson website upcoming events.
     */
    public static class Event
    {
        public String title;
        public Date initialDate;
        /**
         * Used when the event last days
         */
        public Date endDate;
        public String placeName;
        public URL placeURL;
        public String address;
        public String phoneNumber;
        public String eventType;
        /**
         * Notes and other information about an event in HTML
         */
        public String htmlRemain;
        public boolean readed = false;
        private long id = -1;

        public Event(){}
        public Event(long aID, String aTitle, Date aInitialDate, Date aEndDate, String aPlaceName, String aPlaceURL,String aAddress,String aPhoneNumber,String aEventType,String aHtmlRemain,boolean aReaded)
        {
            id = aID;
            title = aTitle;
            initialDate = aInitialDate;
            endDate = aEndDate;
            placeName = aPlaceName;
            try { placeURL = new URL(aPlaceURL); }
            catch (MalformedURLException ignored){ }
            address = aAddress;
            phoneNumber = aPhoneNumber;
            eventType = aEventType;
            htmlRemain = aHtmlRemain;
            readed = aReaded;
        }

        /**
         * If this event contains all necessary information
         * @return If is a valid event
         */
        public boolean isValid()
        {
            return (title!=null && initialDate!=null && placeName != null && placeURL != null && address!=null && phoneNumber != null && eventType!= null && htmlRemain!=null);
        }

        /**
         * Update the DB with a list of events
         * @param events The list of events
         */
        public static void updateEvents(List<Event> events)
        {
            if(events != null && events.size() > 0)
            {
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                db.beginTransaction();

                //  Compile SQL statements
                SQLiteStatement insertEvents = db.compileStatement("INSERT INTO Events (Title,InitialDate,EndDate,PlaceName,PlaceURL,Address,PhoneNumber,EventType,HtmlRemain) VALUES (?,?,?,?,?,?,?,?,?)");
                SQLiteStatement selectEvents = db.compileStatement("SELECT Readed FROM Events WHERE Title=? AND InitialDate=?");

                // Iterate all events
                for (int i = 0; i < events.size(); i++)
                {
                    // Check if already exits
                    selectEvents.bindString(1, events.get(i).title);
                    selectEvents.bindString(2, dbDateFormat.format(events.get(i).initialDate));
                    try
                    {
                        events.get(i).readed = selectEvents.simpleQueryForLong() == DB_TRUE;
                        // TODO: Check that all data is updated
                    }
                    catch (SQLiteDoneException e)// Not exists -> Insert new data
                    {
                        insertEvents.bindString(1, events.get(i).title);
                        insertEvents.bindString(2, dbDateFormat.format(events.get(i).initialDate));
                        if (events.get(i).endDate != null)
                            insertEvents.bindString(3, dbDateFormat.format(events.get(i).endDate));
                        else
                            insertEvents.bindNull(3);
                        insertEvents.bindString(4, events.get(i).placeName);
                        insertEvents.bindString(5, events.get(i).placeURL.toString());
                        insertEvents.bindString(6, events.get(i).address);
                        insertEvents.bindString(7, events.get(i).phoneNumber);
                        insertEvents.bindString(8, events.get(i).eventType);
                        insertEvents.bindString(9, events.get(i).htmlRemain);

                        insertEvents.executeInsert();
                    }
                }

                insertEvents.close();
                selectEvents.close();

                try { db.setTransactionSuccessful(); }
                finally { db.endTransaction(); }

                db.close();
            }
        }

        /**
         * Get general information about events
         * @return The unread and total number of upcoming events
         */
        public static LabelStatus getStatus()
        {
            LabelStatus status = new LabelStatus();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM Events WHERE Readed=0 AND (InitialDate > date('now') OR (EndDate IS NOT NULL AND EndDate > date('now')))", null);
            c.moveToFirst();
            status.countUnread = c.getInt(0);
            c.close();
            // Count all
            c = db.rawQuery("SELECT Count(*) FROM Events WHERE (InitialDate > date('now') OR (EndDate IS NOT NULL AND EndDate > date('now')))", null);
            c.moveToFirst();
            status.total = c.getInt(0);
            c.close();

            db.close();

            return status;
        }

        /**
         * Get all upcoming events from DB
         * @return The upcoming events
         */
        public static List<Event> getEventsFromDB()
        {
            ArrayList<Event> events = new ArrayList<>();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT _id,Title,InitialDate,EndDate,PlaceName,PlaceURL,Address,PhoneNumber,EventType,HtmlRemain,Readed FROM Events WHERE "+
                    "(InitialDate > date('now') OR (EndDate IS NOT NULL AND EndDate > date('now'))) ORDER BY InitialDate", null);
            boolean had_more = c.moveToFirst();
            while(had_more)
            {
                try
                {
                    if(c.isNull(3))
                        events.add(new Event(c.getLong(0), c.getString(1), dbDateFormat.parse(c.getString(2)), null, c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getInt(10)==DB_TRUE));
                    else
                        events.add(new Event(c.getLong(0), c.getString(1), dbDateFormat.parse(c.getString(2)), dbDateFormat.parse(c.getString(3)), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getInt(10)==DB_TRUE));
                }
                catch (ParseException ignored){}

                had_more = c.moveToNext();
            }
            c.close();

            db.close();

            return events;
        }

        /**
         * Mark this event as read by the user
         */
        public void setRead()
        {
            if(!readed)
            {
                readed = true;
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("Readed", DB_TRUE);
                db.update("Events", values, "_id="+id, null);

                db.close();
            }
        }

        /**
         * Date formatters
         */
        private static final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        private static final SimpleDateFormat showDateTimeFormat = new SimpleDateFormat("MMM d, yyyy K:mm a", Locale.US);

        /**
         * Get formatted date information as a string
         * @return The date formatted
         */
        public String getDateString()
        {
            String date_string;

            if(endDate != null)
                date_string = showDateFormat.format(initialDate) + " - " + showDateFormat.format(endDate);
            else
                date_string = showDateTimeFormat.format(initialDate);

            return date_string;
        }

        /**
         * Format the place as HTML text
         * @return The HTML text with the place
         */
        public String getPlaceHTML()
        {
            return "<a href=\"" + placeURL + "\">" + placeName + "</a>";
        }
    }

    /**
     * Tweets generate by Sanderson
     */
    public static class Tweet
    {
        private long guid;
        public String text;
        public Date date;
        public String userName;
        public String userImageUrl;

        public String htmlReply;
        public boolean isReaded;

        public Tweet(long aGuid, String aText, Date aDate, boolean aIsReaded, String aUserName, String aUserImageUrl)
        {
            guid = aGuid;
            text = aText;
            date = aDate;
            userName = aUserName;
            userImageUrl = aUserImageUrl;

            isReaded = aIsReaded;
            htmlReply = "";
        }

        /**
         * Load the user profile image
         * @return The image
         */
        public Drawable loadUserImage()
        {
            File userImageFile = InternetHelper.getRemoteFile(userImageUrl);
            try
            {
                if(userImageFile != null)
                    return new BitmapDrawable(MainActivity.staticRef.getResources(), userImageFile.getAbsolutePath());
            }
            catch (Exception e)// Bad image file?
            {
                userImageFile.delete();
            }

            return MainActivity.staticRef.getResources().getDrawable(R.drawable.ic_action_picture);
        }

        /**
         * Update the database with new tweets
         * @param tweets The tweets to add
         */
        public static void updateTwitter(List<Tweet> tweets)
        {
            if(tweets != null && tweets.size() > 0)
            {
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                db.beginTransaction();

                //  Compile SQL statements
                SQLiteStatement insertTweets = db.compileStatement("INSERT INTO Tweets (_id,Text,Date,Username,UserImageUrl,HtmlReply) VALUES (?,?,datetime(?),?,?,?)");

                // Iterate all tweets
                for (Tweet tweet : tweets)
                    try
                    {
                        insertTweets.bindLong(1, tweet.guid);
                        insertTweets.bindString(2, tweet.text);
                        insertTweets.bindString(3, dbDateFormat.format(tweet.date));
                        insertTweets.bindString(4, tweet.userName);
                        insertTweets.bindString(5, tweet.userImageUrl);
                        insertTweets.bindString(6, tweet.htmlReply);

                        insertTweets.executeInsert();
                    }
                    catch (Exception ignored){}

                insertTweets.close();

                // Delete old tweets
                db.delete("Tweets", "(strftime('%s','now') - strftime('%s',Date)) > " + SettingsFragment.getDeleteTweetsDaysInSeconds(currentContext), null);

                try { db.setTransactionSuccessful(); }
                finally { db.endTransaction(); }

                db.close();
            }
        }

        /**
         * Get the tweets in db
         * @return The Sanderson tweets
         */
        public static List<Tweet> getTweetsFromDB()
        {
            ArrayList<Tweet> tweets = new ArrayList<>();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT _id,Text,Date,Readed,Username,UserImageUrl FROM Tweets ORDER BY Date DESC", null);
            boolean had_more = c.moveToFirst();
            while(had_more)
            {
                try { tweets.add(new Tweet(c.getLong(0), c.getString(1), dbDateFormat.parse(c.getString(2)), c.getInt(3)==DB_TRUE, c.getString(4), c.getString(5))); }
                catch (Exception ignored){}

                had_more = c.moveToNext();
            }
            c.close();

            db.close();

            return tweets;
        }

        /**
         * Get the Label Status of the tweets in db.
         * @return The read and total number of tweets.
         */
        public static LabelStatus getStatus()
        {
            LabelStatus status = new LabelStatus();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM Tweets WHERE Readed=0", null);
            c.moveToFirst();
            status.countUnread = c.getInt(0);
            c.close();
            // Count all
            c = db.rawQuery("SELECT Count(*) FROM Tweets", null);
            c.moveToFirst();
            status.total = c.getInt(0);
            c.close();

            db.close();

            return status;
        }

        /**
         * Read extended information about a tweet.
         * @param original The original tweet.
         * @return The new tweet with extended information.
         */
        public static Tweet readExtendedInfo(Tweet original)
        {
            Tweet tweet = new Tweet(original.guid, original.text, original.date, original.isReaded, original.userName, original.userImageUrl);

            DbHelper mDbHelper = new DbHelper(currentContext);

            if(!tweet.isReaded)
            {
                tweet.isReaded = true;

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("Readed", DB_TRUE);
                db.update("Tweets", values, "_id=" + tweet.guid, null);

                db.close();
            }

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT HtmlReply FROM Tweets WHERE _id=" + tweet.guid, null);
            boolean had_more = c.moveToFirst();
            if(had_more)
                try { tweet.htmlReply = c.getString(0); }
                catch (Exception ignored){}

            c.close();
            db.close();

            return tweet;
        }

        /**
         * Get the id of the last tweet in db. Useful to request only newest tweets.
         * @return The id of the last tweet we know.
         */
        public static long getLastTweetID()
        {
            long rowId = 0;
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            SQLiteStatement selectMaxID = db.compileStatement("SELECT max(_id) FROM Tweets");

            try { rowId = selectMaxID.simpleQueryForLong(); }
            catch (SQLiteDoneException ignored){}

            selectMaxID.close();
            db.close();

            return rowId;
        }
    }

    /**
     * Comments in the Tor Stormlight Reread.
     */
    public static class TorRereadComment
    {
        public long guid = -1;
        public Date publicationDate = null;
        public String creator = null;
        public String html_content = null;
        public boolean readed = false;
        public int index = 0;

        public TorRereadPost parent = null;
        private CommentsStatusNotification onReadStatusChange = null;

        public TorRereadComment(){}
        public TorRereadComment(long ID, String aDate, boolean isReaded, String aCreator, String aContent, int aIndex, TorRereadPost aParent)
        {
            guid = ID;
            readed = isReaded;
            creator = aCreator;
            html_content = aContent;
            index = aIndex;
            parent = aParent;
            try{ publicationDate = dbDateFormat.parse(aDate); }
            catch (Exception ignored){ }
        }

        public boolean isValid()
        {
            return (publicationDate != null && guid >= 0 && html_content != null && creator != null);
        }

        public boolean isLinkToOldComments()
        {
            return guid == -1;
        }

        public void setOnReadStatusChange(CommentsStatusNotification aOnReadStatusChange)
        {
            onReadStatusChange = aOnReadStatusChange;
        }
        public void markAsReaded()
        {
            if(!readed)
            {
                readed = true;
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("Readed", DB_TRUE);
                db.update("TorRereadComments", values, "_id=" + guid, null);

                db.close();

                if(parent != null)
                    parent.notifyCommentsStatusChange();
                if(onReadStatusChange != null)
                    onReadStatusChange.notifyChange();

                MainActivity.decBadgeNumber(MainActivity.APP_STATE_WOR);
            }
        }
    }
    public interface CommentsStatusNotification
    {
        void notifyChange();
    }
    /**
     * Chapters in the Tor Stormlight Reread.
     */
    public static class TorRereadPost
    {
        public String title = null;
        public URL link = null;
        public Date publicationDate = null;
        public String creator = "";
        public long guid = -1;
        public String html_content = null;
        public boolean readed = false;

        public ArrayList<TorRereadComment> comments = new ArrayList<>();
        public int totalNumComments = 0;

        public TorRereadPost(){}
        public TorRereadPost(long ID, String aTitle, String aDate, boolean isReaded, String aCreator, int aTotalNumComments)
        {
            guid = ID;
            title = aTitle;
            readed = isReaded;
            creator = aCreator;
            totalNumComments = aTotalNumComments;
            try{ publicationDate = dbDateFormat.parse(aDate); }
            catch (Exception ignored){ }
        }
        /**
         * If this post contains all necessary data.
         * @return If is valid
         */
        public boolean isValid()
        {
            return (title != null && link != null && publicationDate != null && guid >= 0 && html_content != null);
        }

        public static void updateBlogPosts(List<TorRereadPost> blogPosts)
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.beginTransaction();

            //  Compile SQL statements
            SQLiteStatement insertBlogPost = db.compileStatement("INSERT INTO TorRereadPosts (_id,Title,Link,PublicationDate,Creator,Content,NumComments) VALUES (?,?,?,datetime(?),?,?,?)");
            SQLiteStatement selectBlogPost = db.compileStatement("SELECT NumComments FROM TorRereadPosts WHERE _id=?");
            SQLiteStatement countComments = db.compileStatement("SELECT Count(*) FROM TorRereadComments WHERE TorRereadPost=?");

            SQLiteStatement insertComment = db.compileStatement("INSERT INTO TorRereadComments (_id,PublicationDate,Creator,Content,TorRereadPost) VALUES (?,datetime(?),?,?,?)");
            SQLiteStatement selectComment = db.compileStatement("SELECT Readed FROM TorRereadComments WHERE _id=?");

            // Iterate all posts
            for (TorRereadPost post : blogPosts)
            {
                // Check if already exits
                int numComments;
                selectBlogPost.bindLong(1, post.guid);
                try { numComments = (int) selectBlogPost.simpleQueryForLong(); }
                catch (SQLiteDoneException e)// Not exists -> Insert new data
                {
                    insertBlogPost.bindLong(1, post.guid);
                    insertBlogPost.bindString(2, post.title);
                    insertBlogPost.bindString(3, post.link.toString());
                    insertBlogPost.bindString(4, dbDateFormat.format(post.publicationDate));
                    insertBlogPost.bindString(5, post.creator);
                    insertBlogPost.bindString(6, post.html_content);
                    insertBlogPost.bindLong(7, post.totalNumComments);
                    numComments = post.totalNumComments;

                    insertBlogPost.executeInsert();
                }
                // Save comments
                for (TorRereadComment comment : post.comments)
                {
                    // Check if already exits
                    selectComment.bindLong(1, comment.guid);
                    try{ comment.readed = selectComment.simpleQueryForLong() == DB_TRUE; }
                    catch (SQLiteDoneException e)// Not exists -> Insert new data
                    {
                        insertComment.bindLong(1, comment.guid);
                        insertComment.bindString(2, dbDateFormat.format(comment.publicationDate));
                        insertComment.bindString(3, comment.creator);
                        insertComment.bindString(4, comment.html_content);
                        insertComment.bindLong(5, post.guid);

                        insertComment.executeInsert();
                        comment.readed = false;
                    }
                }
                // Update NumComments
                countComments.bindLong(1, post.guid);
                int realNumComments = Math.max((int) countComments.simpleQueryForLong(), post.totalNumComments);
                if(numComments < realNumComments)
                {
                    ContentValues values = new ContentValues();
                    values.put("NumComments", realNumComments);
                    db.update("TorRereadPosts", values, "_id="+post.guid, null);
                }
            }

            insertBlogPost.close();
            selectBlogPost.close();
            insertComment.close();
            selectComment.close();
            countComments.close();

            try {db.setTransactionSuccessful();}
            finally {db.endTransaction();}

            db.close();
        }

        /**
         * Get all posts from DB
         * @return A list of posts with minimal data
         */
        public static List<TorRereadPost> getBlogPostsFromDB()
        {
            ArrayList<TorRereadPost> posts = new ArrayList<>();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT _id,Title,PublicationDate,Readed,Creator,NumComments FROM TorRereadPosts ORDER BY PublicationDate DESC", null);
            boolean had_more = c.moveToFirst();
            while(had_more)
            {
                posts.add(new TorRereadPost(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)==DB_TRUE, c.getString(4), c.getInt(5)));
                had_more = c.moveToNext();
            }
            c.close();

            db.close();

            return posts;
        }

        /**
         * Get general information about posts
         * @return The total and read number of posts
         */
        private static LabelStatus getPostStatus()
        {
            LabelStatus status = new LabelStatus();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM TorRereadPosts WHERE Readed=0", null);
            c.moveToFirst();
            status.countUnread = c.getInt(0);
            c.close();
            // Count all
            c = db.rawQuery("SELECT Count(*) FROM TorRereadPosts", null);
            c.moveToFirst();
            status.total = c.getInt(0);
            c.close();

            db.close();

            return status;
        }
        private static int getNumUnreadedComments()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM TorRereadComments WHERE Readed=0", null);
            c.moveToFirst();
            int countUnread = c.getInt(0);
            c.close();

            db.close();

            return countUnread;
        }
        private static int getAllNumComments()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT sum(NumComments) FROM TorRereadPosts", null);
            c.moveToFirst();
            int totalComments = c.getInt(0);
            c.close();

            db.close();

            return totalComments;
        }
        public static int getTotalUnread()
        {
            DB.LabelStatus postsStatusNumbers = DB.TorRereadPost.getPostStatus();

            return postsStatusNumbers.countUnread + getNumUnreadedComments();
        }
        public static boolean getStatus(StringBuilder builder)
        {
            DB.LabelStatus postsStatusNumbers = DB.TorRereadPost.getPostStatus();
            int numUnreadComments = getNumUnreadedComments();

            boolean useHighlight = false;
            if(numUnreadComments > 0)
            {
                builder.append("<b>");
                builder.append(numUnreadComments);
                builder.append(" / ");
                builder.append(getAllNumComments());
                builder.append("</b> - ");

                useHighlight = true;
            }
            else
            {
                builder.append(getAllNumComments());
                if(postsStatusNumbers.countUnread > 0)
                    builder.append(" - ");
                else
                    builder.append(" comments, ");
            }
            if(postsStatusNumbers.countUnread > 0)
            {
                builder.append("<b>");
                builder.append(postsStatusNumbers.countUnread);
                builder.append(" / ");
                builder.append(postsStatusNumbers.total);
                builder.append(" posts</b>");

                useHighlight = true;
            }
            else
            {
                builder.append(postsStatusNumbers.total);
                builder.append(" posts");
            }

            return useHighlight;
        }

        public String getHtmlNumComments()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Count Unread
            Cursor c = db.rawQuery("SELECT Count(*) FROM TorRereadComments WHERE Readed=0 AND TorRereadPost=" + guid, null);
            c.moveToFirst();
            int numUnread = c.getInt(0);
            c.close();

            db.close();

            // Format the data
            if(numUnread > 0)
                return "<b>" + numUnread + " / " + totalNumComments + " comments</b>";

            return totalNumComments + " comments";
        }

        /**
         * Show the post to the user
         * @param context The context
         */
        public void showToUser(Context context)
        {
            if(isLinkToOldPosts())
            {
                Uri webpage = Uri.parse("http://www.tor.com/series/words-of-radiance-reread-on-torcom/");
                Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                if(MainActivity.isIntentSafe(webIntent))
                    context.startActivity(webIntent);
            }
            else
            {
                if (!readed)
                    MainActivity.decBadgeNumber(MainActivity.APP_STATE_WOR);
                loadContentFromDB();
                Intent intent = new Intent(context, ReadTorRereadPostActivity.class);
                TorWoRRereadFragment.readingPost = this;
                context.startActivity(intent);
            }
        }

        /**
         * Load content of a post to be read by the user
         */
        public void loadContentFromDB()
        {
            if(html_content == null)
            {
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getReadableDatabase();

                Cursor c = db.rawQuery("SELECT Content FROM TorRereadPosts WHERE _id=" + guid, null);
                if (c.moveToFirst())
                    html_content = c.getString(0);

                c.close();
                db.close();
            }
            if(!readed)
            {
                readed = true;
                DbHelper mDbHelper = new DbHelper(currentContext);

                // Gets the data repository in read mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("Readed", DB_TRUE);
                db.update("TorRereadPosts", values, "_id="+guid, null);

                db.close();
            }
        }

        public boolean isLinkToOldPosts()
        {
            return guid == -1;
        }

        public int getNumComments()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            SQLiteStatement selectBlogPost = db.compileStatement("SELECT NumComments FROM TorRereadPosts WHERE _id=?");
            selectBlogPost.bindLong(1, guid);

            int numComments = 0;
            try{ numComments = (int) selectBlogPost.simpleQueryForLong(); }
            catch (SQLiteDoneException ignored){}

            selectBlogPost.close();
            db.close();

            return numComments;
        }

        public List<TorRereadComment> loadComments()
        {
            ArrayList<TorRereadComment> comments = new ArrayList<>();
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT _id,PublicationDate,Readed,Creator,Content FROM TorRereadComments WHERE TorRereadPost=" + guid + " ORDER BY PublicationDate DESC", null);
            boolean had_more = c.moveToFirst();
            int index  = totalNumComments;
            while (had_more)
            {
                comments.add(new TorRereadComment(c.getLong(0), c.getString(1), c.getInt(2) == DB_TRUE, c.getString(3), c.getString(4), index, this));
                index--;
                had_more = c.moveToNext();
            }
            c.close();

            db.close();

            return comments;
        }

        public String getURL()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);
            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            String url = "";
            Cursor c = db.rawQuery("SELECT Link FROM TorRereadPosts WHERE _id=" + guid, null);
            if (c.moveToFirst())
                url = c.getString(0);

            c.close();
            db.close();

            return url;
        }

        public ArrayList<CommentsStatusNotification> onCommentsStatusChange = new ArrayList<>(3);
        public void notifyCommentsStatusChange()
        {
            for (CommentsStatusNotification notif : onCommentsStatusChange)
                notif.notifyChange();
        }

        public static void markAllPostsAndCommentsAsRead()
        {
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("Readed", DB_TRUE);
            db.update("TorRereadPosts", values, null, null);
            db.update("TorRereadComments", values, null, null);

            try {db.setTransactionSuccessful();}
            finally {db.endTransaction();}
            db.close();
        }

        /**
         * Get the db id from a link
         * @param link The link of the post
         * @return The db id of the post or -1 if not found
         */
        public static TorRereadPost getPostFromLink(String link)
        {
            TorRereadPost post = null;
            DbHelper mDbHelper = new DbHelper(currentContext);

            // Gets the data repository in read mode
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            Cursor c = db.rawQuery("SELECT _id,Title,PublicationDate,Readed,Creator,NumComments FROM TorRereadPosts WHERE Link='"+link+"'", null);
            if(c.moveToFirst())
                post = new TorRereadPost(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)==DB_TRUE, c.getString(4), c.getInt(5));

            c.close();
            db.close();

            return post;
        }
    }
}