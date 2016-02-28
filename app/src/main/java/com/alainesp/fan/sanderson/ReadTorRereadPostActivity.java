// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to read a Tor Reread Post
 */
public class ReadTorRereadPostActivity extends AppCompatActivity implements DB.CommentsStatusNotification
{
    /**
     * Date formatter to show post publication date.
     */
    private final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private TextView categories = null, date, html_content;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_blog_post);

        // Post title
        DB.TorRereadPost post = TorWoRRereadFragment.readingPost;
        setTitle(post.title);
        // Date
        date = (TextView) findViewById(R.id.blog_post_date);
        date.setText(post.creator + " - " + showDateFormat.format(post.publicationDate));

        categories = (TextView) findViewById(R.id.blog_post_categories);
        categories.setText(Html.fromHtml(post.getHtmlNumComments()));
        categories.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(), TorRereadCommentsActivity.class);
                v.getContext().startActivity(intent);
            }
        });
        if(!TorWoRRereadFragment.readingPost.onCommentsStatusChange.contains(this))
            TorWoRRereadFragment.readingPost.onCommentsStatusChange.add(this);

        // Post content
        html_content = (TextView) findViewById(R.id.blog_post_content);
        // Eliminate unnecessary spaces between topics
        html_content.setText(Html.fromHtml(post.html_content.replace("<p>&nbsp;</p>", ""), new InternetHelper.UrlImageGetter(), null));
        html_content.setMovementMethod(new LocalLink((ImageView)findViewById(R.id.slider_expanded_image), findViewById(R.id.slider_container), (ScrollView)findViewById(R.id.read_blog_post_scroll)));

        // Support zoom
        int fontSize = SettingsFragment.getTextSize();
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        categories.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        html_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.zoom_menu_visible, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int fontSize;
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.zoom_in:
                fontSize = SettingsFragment.incTextSize();
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                categories.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                html_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                return true;
            case R.id.zoom_out:
                fontSize = SettingsFragment.decTextSize();
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                categories.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                html_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle status change of comments (a comment is read).
     */
    @Override
    public void notifyChange()
    {
        categories.setText(Html.fromHtml(TorWoRRereadFragment.readingPost.getHtmlNumComments()));
    }
}

