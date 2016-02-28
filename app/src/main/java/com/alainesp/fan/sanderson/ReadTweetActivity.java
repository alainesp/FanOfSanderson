// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Activity to show details of a tweet
 */
public class ReadTweetActivity extends AppCompatActivity
{
    private TextView tweetText;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_tweet);

        // Show information
        DB.Tweet tweet = DB.Tweet.readExtendedInfo(TwitterFragment.readingTweet);
        TwitterFragment.readingTweet.isReaded = true;
        setTitle(tweet.userName + " - " + TwitterFragment.showDateFormat.format(tweet.date));

        // Set tweet as HTML
        tweetText = (TextView) findViewById(R.id.tweet_content);
        tweetText.setText(Html.fromHtml(tweet.htmlReply + tweet.text, new InternetHelper.UrlImageGetter(), null));
        tweetText.setMovementMethod(new LocalLink((ImageView)findViewById(R.id.slider_expanded_image), findViewById(R.id.slider_container), (ScrollView)findViewById(R.id.read_blog_post_scroll)));

        tweetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsFragment.getTextSize());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.zoom_menu, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.zoom_in:
                tweetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsFragment.incTextSize());
                return true;
            case R.id.zoom_out:
                tweetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsFragment.decTextSize());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
