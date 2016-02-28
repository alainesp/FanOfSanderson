// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing all blog posts
 */
public class BlogPostsFragment extends Fragment
{
    /**
     * Formatter to show the date of the blog posts
     */
    private final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    /**
     * Variable to publish the post that is been read.
     */
    public static DB.BlogPost readingPost = null;
    /**
     * All the blog posts in the database
     */
    public static List<DB.BlogPost> blogPosts = null;

    /**
     * Adapter of the blog posts
     */
    private class BlogPostAdapter extends RecyclerView.Adapter<BlogPostAdapter.BlogPostViewHolder>
    {
        /**
         * Provide a reference to the views for each data item
         */
        public class BlogPostViewHolder extends RecyclerView.ViewHolder
        {
            private final View blog_post;
            private final TextView title;
            private final TextView date;
            private final TextView categories;
            private DB.BlogPost current_post = null;

            public BlogPostViewHolder(View v)
            {
                super(v);
                blog_post = v;
                title = (TextView)blog_post.findViewById(R.id.post_title);
                date = (TextView)blog_post.findViewById(R.id.post_date);
                categories = (TextView)blog_post.findViewById(R.id.post_categories);

                // Read a blog post when clicked
                blog_post.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (current_post != null)
                        {
                            current_post.showToUser(v.getContext());
                            setPost(current_post);
                        }
                    }
                });
            }

            /**
             * Update the GUI with data from a given blog post
             * @param post The blog post to show
             */
            public void setPost(DB.BlogPost post)
            {
                current_post = post;
                String date_string = showDateFormat.format(post.publicationDate);
                String categoriesFormatted = post.getCategories();

                if(post.readed)
                {
                    title.setText(post.title);
                    date.setText(date_string);
                    categories.setText(categoriesFormatted);
                }
                else
                {
                    title.setText(Html.fromHtml("<b>" + post.title + "</b>"));
                    date.setText(Html.fromHtml("<b>" + date_string + "</b>"));
                    categories.setText(Html.fromHtml("<b>" + categoriesFormatted + "</b>"));
                }
            }
        }

        /**
         * Provide a suitable constructor (depends on the kind of dataset)
         */
        public BlogPostAdapter(List<DB.BlogPost> myDataset)
        {
            blogPosts = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public BlogPostViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            // TODO: Create an option to mark the post as favorite
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_post, parent, false);
            return new BlogPostViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(BlogPostViewHolder holder, int position)
        {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setPost(blogPosts.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount()
        {
            return blogPosts.size();
        }
    }

    // Create the fragment view
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_recycler_view, container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
        mRecyclerView.setAdapter(new BlogPostAdapter(DB.BlogPost.getBlogPostsFromDB()));

        return rootView;
    }
}
