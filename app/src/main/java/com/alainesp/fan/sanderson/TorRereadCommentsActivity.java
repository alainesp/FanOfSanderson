// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity showing all comments
 */
public class TorRereadCommentsActivity extends AppCompatActivity
{
    /**
     * Formatter to show the date of the comment
     */
    private static final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
    public static String getDateString(Date date)
    {
        String dateString = "";
        long diffTime =  new Date().getTime() - date.getTime();
        if(diffTime >= 0)
        {
            if(diffTime == 0)
            {
                dateString = "Now";
            }
            else if(diffTime < DateUtils.MINUTE_IN_MILLIS)
            {
                diffTime = (diffTime+DateUtils.SECOND_IN_MILLIS/2)/DateUtils.SECOND_IN_MILLIS;
                dateString = "" + diffTime + (diffTime == 1 ? " second ago" : " seconds ago");
            }
            else if(diffTime < DateUtils.HOUR_IN_MILLIS)
            {
                diffTime = (diffTime+DateUtils.MINUTE_IN_MILLIS/2)/DateUtils.MINUTE_IN_MILLIS;
                dateString = "" + diffTime + (diffTime == 1 ? " minute ago" : " minutes ago");
            }
            else if(diffTime < DateUtils.DAY_IN_MILLIS)
            {
                diffTime = (diffTime+DateUtils.HOUR_IN_MILLIS/2)/DateUtils.HOUR_IN_MILLIS;
                dateString = "" + diffTime + (diffTime == 1 ? " hour ago" : " hours ago");
            }
            else
            {
                dateString = showDateFormat.format(date);
            }
        }

        return dateString;
    }
    /**
     * Variable to publish the comment that has been read.
     */
    public static DB.TorRereadComment readingComment = null;
    /**
     * All the comments of a given post
     */
    public static List<DB.TorRereadComment> comments = null;

    /**
     * Adapter of the Comments
     */
    private class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>
    {
        /**
         * Provide a reference to the views for each data item
         */
        public class CommentViewHolder extends RecyclerView.ViewHolder implements DB.CommentsStatusNotification
        {
            private final View commentView;
            private final TextView index;
            private final TextView title;
            private final TextView date;
            private final TextView author;
            private DB.TorRereadComment currentComment = null;

            public CommentViewHolder(View v)
            {
                super(v);
                commentView = v;
                index = (TextView) commentView.findViewById(R.id.post_index);
                title = (TextView) commentView.findViewById(R.id.post_title);
                date = (TextView) commentView.findViewById(R.id.post_date);
                author = (TextView) commentView.findViewById(R.id.post_autor);

                // Read a blog post when clicked
                commentView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (currentComment != null)
                        {
                            if (currentComment.isLinkToOldComments())
                            {
                                Uri webpage = Uri.parse(TorWoRRereadFragment.readingPost.getURL() + "#comments");
                                Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                                if (MainActivity.isIntentSafe(webIntent))
                                    v.getContext().startActivity(webIntent);
                            }
                            else
                            {
                                readingComment = currentComment;
                                readingComment.markAsReaded();

                                Intent intent = new Intent(v.getContext(), ReadTorCommentActivity.class);
                                v.getContext().startActivity(intent);
                            }
                        }
                    }
                });
            }
            /**
             * Update the GUI with data from a given comment
             * @param comment The comment to show
             */
            public void setPost(DB.TorRereadComment comment)
            {
                if(currentComment != comment)
                {
                    currentComment = comment;
                    comment.setOnReadStatusChange(this);
                }
                String content = Html.fromHtml(comment.html_content).toString();

                if(comment.readed)
                {
                    title.setText(content);

                    if(comment.isLinkToOldComments())
                    {
                        author.setText(comment.creator);
                        date.setText("");
                        index.setText("1-" + comment.index);
                    }
                    else
                    {
                        author.setText(comment.creator);
                        date.setText(getDateString(comment.publicationDate));
                        index.setText(""+comment.index);
                    }
                }
                else
                {
                    author.setText(Html.fromHtml("<b>" + comment.creator + "</b>"));
                    date.setText(Html.fromHtml("<b>" + getDateString(comment.publicationDate) + "</b>"));
                    title.setText(Html.fromHtml("<b>" + content + "</b>"));
                    index.setText(Html.fromHtml("<b>" + comment.index + "</b>"));
                }
            }

            /**
             * If the comment is read notify to the GUI.
             */
            @Override
            public void notifyChange()
            {
                if(currentComment != null)
                    setPost(currentComment);
            }
        }

        /**
         * Provide a suitable constructor (depends on the kind of dataset)
         */
        public CommentsAdapter(List<DB.TorRereadComment> myDataset)
        {
            comments = myDataset;
            // Check if we had all the comments, if not add a link to Tor.xom
            int numCommentInDB = comments.size();
            if(TorWoRRereadFragment.readingPost.totalNumComments > numCommentInDB)
                myDataset.add(new DB.TorRereadComment(-1, null, true, "See all comments on Tor.com", "Old comments", TorWoRRereadFragment.readingPost.totalNumComments - numCommentInDB, null));
        }

        // Create new views (invoked by the layout manager)
        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tor_reread_comment, parent, false);
            return new CommentViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position)
        {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setPost(comments.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount()
        {
            return comments.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_recycler_view);

        setTitle(TorWoRRereadFragment.readingPost.title + " Comments");

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.staticRef));
        mRecyclerView.setAdapter(new CommentsAdapter(TorWoRRereadFragment.readingPost.loadComments()));

        // Begin at last comment we read
        int beginItemIndex;
        for (beginItemIndex = comments.size()-1; beginItemIndex > 0; beginItemIndex--)
            if(!comments.get(beginItemIndex).readed)
                break;
        // Give some top margin
        beginItemIndex = Math.max(0, beginItemIndex-2);
        mRecyclerView.scrollToPosition(beginItemIndex);
    }

    // Support methods to comment mention (example: @45)
    public static String getCommentAuthor(int index)
    {
        if(TorWoRRereadFragment.readingPost != null && comments != null)
        {
            index = TorWoRRereadFragment.readingPost.totalNumComments - index;
            if (index >= 0 && index < comments.size())
                return comments.get(index).creator;
        }

        return null;
    }
    public static boolean isValidIndex(int index)
    {
        if(TorWoRRereadFragment.readingPost != null && comments != null)
        {
            index = TorWoRRereadFragment.readingPost.totalNumComments - index;
            if (index >= 0 && index < comments.size() && !comments.get(index).isLinkToOldComments())
                return true;
        }
        return false;
    }
    public static void showComment(int index, Context context)
    {
        if(TorWoRRereadFragment.readingPost != null && comments != null)
        {
            index = TorWoRRereadFragment.readingPost.totalNumComments - index;
            if(index >= 0 && index < comments.size() && !comments.get(index).isLinkToOldComments())
            {
                // TODO: Consider move to the comment instead of create a new activity. This need to support the back button
                readingComment = comments.get(index);
                readingComment.markAsReaded();

                Intent intent = new Intent(context, ReadTorCommentActivity.class);
                context.startActivity(intent);
            }
        }
    }
}
