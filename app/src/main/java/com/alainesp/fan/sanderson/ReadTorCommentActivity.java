// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity to show details of a comment and provide navigation of comments of a post.
 */
public class ReadTorCommentActivity extends AppCompatActivity
{
    public static final String commentsLinkPrefix = "app://fanofsanderson/reread/comments/";
    private ViewPager mPager;
    private static List<DB.TorRereadComment> comments = null;

    /**
     * Fragment to show a given comment
     */
    public static class ScreenSlidePageFragment extends Fragment
    {
        private DB.TorRereadComment comment;
        private TextView date, categories, html_content;
        private static final String COMMENT_INDEX = "COMMENT_INDEX";

        public ScreenSlidePageFragment(){}

        public Fragment setComment(DB.TorRereadComment aComment)
        {
            comment = aComment;
            return this;
        }
        @Override
        public void onSaveInstanceState(Bundle outState)
        {
            if(comment != null)
                outState.putInt(COMMENT_INDEX, comments.indexOf(comment));

            super.onSaveInstanceState(outState);
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_read_blog_post, container, false);

            if(savedInstanceState != null)
                comment = comments.get(savedInstanceState.getInt(COMMENT_INDEX));

            // Date
            date = (TextView) rootView.findViewById(R.id.blog_post_date);
            date.setText(comment.creator);

            categories = (TextView) rootView.findViewById(R.id.blog_post_categories);
            categories.setText(TorRereadCommentsActivity.getDateString(comment.publicationDate));

            // Comment content
            html_content = (TextView) rootView.findViewById(R.id.blog_post_content);
            // Handle mentions in comments
            Pattern p = Pattern.compile("@[ ]?([0-9]+)");
            Matcher m = p.matcher(comment.html_content);
            StringBuilder buffer = new StringBuilder();
            int pos = 0;
            while (m.find())
            {
                String commentIndex = m.group(1);
                int index = Integer.parseInt(commentIndex);
                if(TorRereadCommentsActivity.isValidIndex(index))// The mentioned comment is in DB
                {
                    // The format of the mention is @55 AuthorName
                    String author = TorRereadCommentsActivity.getCommentAuthor(index);
                    if (author != null && comment.html_content.substring(m.end()).startsWith(" " + author))
                    {
                        buffer.append(comment.html_content.substring(pos, m.start()));
                        buffer.append("<a href=\"");
                        buffer.append(commentsLinkPrefix);
                        buffer.append(commentIndex);
                        buffer.append("\">@");
                        buffer.append(commentIndex);
                        buffer.append(" ").append(author);
                        buffer.append("</a>");
                        pos = m.end() + author.length() + 1;
                    }
                    else// The format of the mention is @55
                    {
                        buffer.append(comment.html_content.substring(pos, m.start()));
                        buffer.append("<a href=\"");
                        buffer.append(commentsLinkPrefix);
                        buffer.append(commentIndex);
                        buffer.append("\">@");
                        buffer.append(commentIndex);
                        buffer.append("</a>");
                        pos = m.end();
                    }
                }
                else// The comment isn't in the DB
                {
                    buffer.append(comment.html_content.substring(pos, m.start()));
                    buffer.append("@");
                    buffer.append(commentIndex);
                    pos = m.end();
                }
            }
            //m.appendTail(buffer);
            buffer.append(comment.html_content.substring(pos, comment.html_content.length()));

            html_content.setText(Html.fromHtml(buffer.toString(), new InternetHelper.UrlImageGetter(), null));
            html_content.setMovementMethod(new LocalLink((ImageView)rootView.findViewById(R.id.slider_expanded_image), rootView.findViewById(R.id.slider_container), (ScrollView)rootView.findViewById(R.id.read_blog_post_scroll)));

            // Support zoom
            int fontSize = SettingsFragment.getTextSize();
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            categories.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            html_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            return rootView;
        }
    }

    /**
     * Adapter to provide comments to the pager
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter
    {
        public ScreenSlidePagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return new ScreenSlidePageFragment().setComment(comments.get(position));
        }

        @Override
        public int getCount()
        {
            return comments.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        // Reverse comments to make easier the code
        comments = new ArrayList<>();
        for (int i = TorRereadCommentsActivity.comments.size()-1; i >= 0; i--)
            if(!TorRereadCommentsActivity.comments.get(i).isLinkToOldComments())
                comments.add(TorRereadCommentsActivity.comments.get(i));

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new ScreenSlidePagerAdapter(getSupportFragmentManager()));
        int commentIndex = comments.indexOf(TorRereadCommentsActivity.readingComment);
        mPager.setCurrentItem(commentIndex);
        setTitle(TorWoRRereadFragment.readingPost.title + " Comment " + comments.get(commentIndex).index);
        // Listener to change Title and ActionMenus based on comment selected
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){}

            @Override
            public void onPageSelected(int position)
            {
                TorRereadCommentsActivity.readingComment = comments.get(position);
                setTitle(TorWoRRereadFragment.readingPost.title + " Comment " + TorRereadCommentsActivity.readingComment.index);

                if(prevCommentItem != null) prevCommentItem.setVisible(position > 0);
                if(nextCommentItem != null) nextCommentItem.setVisible((position + 1) < comments.size());

                TorRereadCommentsActivity.readingComment.markAsReaded();
            }

            @Override
            public void onPageScrollStateChanged(int state){}
        });
    }
    /**
     * The ActionMenus to provide navigation in the comments
     */
    private MenuItem nextCommentItem;
    private MenuItem prevCommentItem;
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_book_menu, menu);

        nextCommentItem = menu.findItem(R.id.book_menu_next);
        prevCommentItem = menu.findItem(R.id.book_menu_prev);

        int index = comments.indexOf(TorRereadCommentsActivity.readingComment);
        prevCommentItem.setVisible(index > 0);
        nextCommentItem.setVisible((index+1) < comments.size());

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.book_menu_next:
                mPager.setCurrentItem(mPager.getCurrentItem()+1, true);
                return true;
            case R.id.book_menu_prev:
                mPager.setCurrentItem(mPager.getCurrentItem()-1, true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
