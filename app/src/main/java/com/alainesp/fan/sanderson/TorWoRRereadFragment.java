// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing all posts
 */
public class TorWoRRereadFragment extends Fragment
{
    /**
     * Formatter to show the date of the posts
     */
    private final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    /**
     * Variable to publish the post that has been read.
     */
    public static DB.TorRereadPost readingPost = null;
    /**
     * All the posts in the database.
     */
    private static List<DB.TorRereadPost> chapterPosts = null;

    /**
     * Adapter of the posts
     */
    private class ChaptersAdapter extends RecyclerView.Adapter<ChaptersAdapter.ChapterViewHolder>
    {
        /**
         * Provide a reference to the views for each data item
         */
        public class ChapterViewHolder extends RecyclerView.ViewHolder implements DB.CommentsStatusNotification
        {
            private final View chapterView;
            private final TextView title;
            private final TextView comments;
            private final TextView date;
            private final TextView author;
            private DB.TorRereadPost currentPost = null;

            public ChapterViewHolder(View v)
            {
                super(v);
                chapterView = v;
                title = (TextView) chapterView.findViewById(R.id.post_title);
                comments = (TextView) chapterView.findViewById(R.id.post_comments);
                author = (TextView) chapterView.findViewById(R.id.post_autor);
                date = (TextView) chapterView.findViewById(R.id.post_date);

                // Read a post when clicked
                chapterView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (currentPost != null)
                        {
                            currentPost.showToUser(v.getContext());
                            setPost(currentPost);
                        }
                    }
                });
                // Show comments directly if clicked
                comments.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (currentPost != null)
                        {
                            readingPost = currentPost;
                            Intent intent = new Intent(v.getContext(), TorRereadCommentsActivity.class);
                            v.getContext().startActivity(intent);
                        }
                    }
                });
            }

            /**
             * Update the GUI with data from a given post
             * @param post The post to show
             */
            public void setPost(DB.TorRereadPost post)
            {
                if(post != currentPost)
                {
                    // Remove the old listener
                    if (currentPost != null) currentPost.onCommentsStatusChange.remove(this);
                    // Add the new one
                    post.onCommentsStatusChange.add(this);
                }
                currentPost = post;

                if(post.readed)
                {
                    title.setText(post.title);
                    if(post.isLinkToOldPosts())
                    {
                        author.setText("See all posts on Tor.com");
                        date.setText("");
                        comments.setText("");
                    }
                    else
                    {
                        author.setText(post.creator);
                        date.setText(showDateFormat.format(post.publicationDate));
                        comments.setText(Html.fromHtml(post.getHtmlNumComments()));
                    }
                }
                else
                {
                    title.setText(Html.fromHtml("<b>" + post.title + "</b>"));
                    author.setText(Html.fromHtml("<b>" + post.creator + "</b>"));
                    date.setText(Html.fromHtml("<b>" + showDateFormat.format(post.publicationDate) + "</b>"));
                    comments.setText(Html.fromHtml(post.getHtmlNumComments()));
                }
            }

            @Override
            public void notifyChange()
            {
                if(currentPost != null)
                    setPost(currentPost);
            }
        }

        /**
         * Provide a suitable constructor (depends on the kind of dataset)
         */
        public ChaptersAdapter(List<DB.TorRereadPost> myDataset)
        {
            chapterPosts = myDataset;
            // Provide link to old chapters in Tor.com
            myDataset.add(new DB.TorRereadPost(-1, "Old posts", null, true, "", 0));
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ChapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tor_reread_chapter, parent, false);
            return new ChapterViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ChapterViewHolder holder, int position)
        {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setPost(chapterPosts.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount()
        {
            return chapterPosts.size();
        }
    }

    // Create the fragment view
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_recycler_view, container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.staticRef));
        mRecyclerView.setAdapter(new ChaptersAdapter(DB.TorRereadPost.getBlogPostsFromDB()));

        setHasOptionsMenu(true);

        return rootView;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.wor_reread_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {
            case R.id.wor_mark_read:
                DB.TorRereadPost.markAllPostsAndCommentsAsRead();
                for (DB.TorRereadPost post : chapterPosts)
                {
                    post.readed = true;
                    post.notifyCommentsStatusChange();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
