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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show all tweets
 */
public class TwitterFragment extends Fragment
{
    /**
     * The tweet currently showed to the user
     */
    public static DB.Tweet readingTweet = null;
    /**
     * Formatter to show the date of the tweets
     */
    public static final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    /**
     * An adapter to show tweets
     */
    private class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.TweetViewHolder>
    {
        private final List<DB.Tweet> mDataset;

        // Provide a reference to the views for each data item
        public class TweetViewHolder extends RecyclerView.ViewHolder
        {
            final View tweetView;
            final TextView userName;
            final TextView date;
            final TextView tweetText;
            final ImageView userImage;
            DB.Tweet dbTweet;

            public TweetViewHolder(View v)
            {
                super(v);

                userImage = (ImageView) v.findViewById(R.id.tweet_user_image);
                userName = (TextView) v.findViewById(R.id.tweet_username);
                date = (TextView) v.findViewById(R.id.tweet_date);
                tweetText = (TextView) v.findViewById(R.id.tweet_text);
                tweetView = v.findViewById(R.id.event_clickable);
                // Show an event when clicked
                tweetView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (dbTweet != null)
                        {
                            if (!dbTweet.isReaded)
                                MainActivity.decBadgeNumber(MainActivity.APP_STATE_TWITTER);
                            Intent intent = new Intent(v.getContext(), ReadTweetActivity.class);
                            readingTweet = dbTweet;
                            setTweetRead(dbTweet);
                            startActivity(intent);
                        }
                    }
                });
            }

            /**
             * Set the tweet to show for this ViewHolder
             * @param tweet The tweet to show
             */
            public void setTweet(DB.Tweet tweet)
            {
                dbTweet = tweet;

                if(dbTweet.isReaded)
                {
                    userName.setText(tweet.userName);
                    date.setText(showDateFormat.format(tweet.date));
                    tweetText.setText(Html.fromHtml(tweet.text));
                }
                else// Highlight tweet when not read
                {
                    userName.setText(Html.fromHtml("<b>" + tweet.userName + "</b>"));
                    date.setText(Html.fromHtml("<b>" + showDateFormat.format(tweet.date) + "</b>"));
                    tweetText.setText(Html.fromHtml("<b>" + tweet.text + "</b>"));
                }
                userImage.setImageDrawable(tweet.loadUserImage());
            }
            public void setTweetRead(DB.Tweet tweet)
            {
                dbTweet = tweet;

                userName.setText(tweet.userName);
                date.setText(showDateFormat.format(tweet.date));
                tweetText.setText(Html.fromHtml(tweet.text));

                userImage.setImageDrawable(tweet.loadUserImage());
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public TweetsAdapter(List<DB.Tweet> myDataset)
        {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public TweetViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet, parent, false);
            return new TweetViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(TweetViewHolder holder, int position)
        {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setTweet(mDataset.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount()
        {
            return mDataset.size();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_recycler_view, container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.staticRef));
        List<DB.Tweet> tweets = DB.Tweet.getTweetsFromDB();
        mRecyclerView.setAdapter(new TweetsAdapter(tweets));

        // Begin at last tweet we read
        int beginItemIndex;
        for (beginItemIndex = tweets.size()-1; beginItemIndex > 0; beginItemIndex--)
            if(!tweets.get(beginItemIndex).isReaded)
                break;
        // Give some top margin
        beginItemIndex = Math.max(0, beginItemIndex-2);
        mRecyclerView.scrollToPosition(beginItemIndex);

        return rootView;
    }
}
