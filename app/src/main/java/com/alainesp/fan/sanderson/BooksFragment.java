// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import java.text.NumberFormat;

/**
 * Fragment to show the list of books organized by Serie
 */
public class BooksFragment extends Fragment
{
    /**
     * Listener to show a book when clicked
     */
    public static final View.OnClickListener onBookClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Catalog.readingBook = (Book) v.getTag();
            Intent intent = new Intent(v.getContext(), BookActivity.class);
            v.getContext().startActivity(intent);
        }
    };
    /**
     * Floating formatter to show the rate
     */
    private final NumberFormat rateFormat = NumberFormat.getInstance();
    /**
     * General integer formatter showing digits grouping
     */
    private final NumberFormat intFormat = NumberFormat.getIntegerInstance();
    /**
     * The control to show all books organized as series
     */
    private ExpandableListView booksView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Set formatters options
        intFormat.setGroupingUsed(true);
        rateFormat.setMaximumFractionDigits(2);
        rateFormat.setMinimumFractionDigits(2);

        // Here begin some calculations to use all screen width to show books covers
        // Get specs for the book covers
        Resources res = getResources();
        int screenWidth = res.getDisplayMetrics().widthPixels;
        int coverWidth = res.getDimensionPixelSize(R.dimen.book_cover_width);
        int coverHeight = res.getDimensionPixelSize(R.dimen.book_cover_height);
        final int coverPadding = res.getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int totalBooksInRow = screenWidth / (coverWidth+coverPadding);
        // Optimize totalBooksInRow to use all screen width
        int bigCoverWidth   = screenWidth/totalBooksInRow-coverPadding;
        int smallCoverWidth = screenWidth/(totalBooksInRow+1)-coverPadding;
        if((bigCoverWidth-coverWidth) < (coverWidth-smallCoverWidth))
        {
            coverHeight = coverHeight*bigCoverWidth/coverWidth;
            coverWidth = bigCoverWidth;
        }
        else
        {
            coverHeight = coverHeight*smallCoverWidth/coverWidth;
            coverWidth = smallCoverWidth;
            totalBooksInRow++;
        }

        View rootView = inflater.inflate(R.layout.content_books, container, false);

        booksView = (ExpandableListView)rootView.findViewById(R.id.books_list);

        final int finalTotalBooksInRow = totalBooksInRow;
        final int finalCoverWidth = coverWidth;
        final int finalCoverHeight = coverHeight;
        // The adapter to show the books and books series
        booksView.setAdapter(new BaseExpandableListAdapter()
        {
            @Override
            public int getGroupCount()
            {
                return Catalog.Brandon.getSeriesCount();
            }

            @Override
            public int getChildrenCount(int groupPosition)
            {
                return (Catalog.Brandon.getBookSerie(groupPosition).getBookCount() + finalTotalBooksInRow - 1) / finalTotalBooksInRow;
            }

            @Override
            public Object getGroup(int groupPosition)
            {
                return Catalog.Brandon.getBookSerie(groupPosition);
            }

            @Override
            public Object getChild(int groupPosition, int childPosition)
            {
                return Catalog.Brandon.getBookSerie(groupPosition).getBookAt(childPosition);
            }

            @Override
            public long getGroupId(int groupPosition)
            {
                return groupPosition;
            }

            @Override
            public long getChildId(int groupPosition, int childPosition)
            {
                return childPosition;
            }

            @Override
            public boolean hasStableIds()
            {
                return true;
            }

            /**
             * Class to cache references to BookSerie controls
             */
            class ViewHolder
            {
                TextView title, count, rate;
                RatingBar rateBar;

                public ViewHolder(TextView aTitle, TextView aCount, TextView aRate, RatingBar aRateBar)
                {
                    title = aTitle;
                    count = aCount;
                    rate = aRate;
                    rateBar = aRateBar;
                }
            }

            /**
             * Show a Book Serie
             */
            @Override
            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
            {
                View v = convertView;
                ViewHolder guiHolder = null;

                // Get the cache
                if(v != null)
                    guiHolder = (ViewHolder) v.getTag();

                // Create all new if not in cache
                if(guiHolder == null)
                {
                    LayoutInflater inflater = MainActivity.staticRef.getLayoutInflater();
                    v = inflater.inflate(R.layout.book_serie, null);

                    guiHolder = new ViewHolder(
                            (TextView) v.findViewById(R.id.book_serie_title),
                            (TextView) v.findViewById(R.id.book_serie_count),
                            (TextView) v.findViewById(R.id.book_serie_rate),
                            (RatingBar) v.findViewById(R.id.book_serie_ratingbar)
                    );
                    v.setTag(guiHolder);
                }

                // Show book serie info
                BookSerie serie = Catalog.Brandon.getBookSerie(groupPosition);
                guiHolder.title.setText(serie.title);
                guiHolder.count.setText(serie.getMiniSeriesData());
                // Serie Rating
                float totalRate = serie.getRate();
                guiHolder.rate.setText(rateFormat.format(totalRate) + " - " + intFormat.format(serie.getRatingsCount()));
                guiHolder.rateBar.setRating(totalRate);

                return v;
            }

            /**
             * Show a row of Books as a cover
             */
            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
            {
                int childCount = isLastChild ? (Catalog.Brandon.getBookSerie(groupPosition).getBookCount()-childPosition* finalTotalBooksInRow) : finalTotalBooksInRow;
                LinearLayout row = (LinearLayout)convertView;
                // Create a row with books if not in cache
                if(row == null || !row.getTag().equals(childCount))
                {
                    row = new LinearLayout(MainActivity.staticRef);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    row.setTag(childCount);
                    row.setGravity(Gravity.CENTER_HORIZONTAL);

                    for (int i = 0; i < childCount; i++)
                    {
                        ImageView img = new ImageView(MainActivity.staticRef);
                        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        img.setPadding(coverPadding/2, 0, coverPadding/2, 0);
                        img.setImageDrawable(Catalog.Brandon.getBookSerie(groupPosition).getBookAt(childPosition * finalTotalBooksInRow + i).loadCover());
                        img.setOnClickListener(onBookClick);
                        img.setTag(Catalog.Brandon.getBookSerie(groupPosition).getBookAt(childPosition * finalTotalBooksInRow + i));

                        row.addView(img, new ViewGroup.LayoutParams(finalCoverWidth, finalCoverHeight));
                    }
                }
                else// Update the covers
                {
                    for (int i = 0; i < childCount; i++)
                    {
                        ImageView img = (ImageView)row.getChildAt(i);
                        img.setImageDrawable(Catalog.Brandon.getBookSerie(groupPosition).getBookAt(childPosition* finalTotalBooksInRow +i).loadCover());
                        img.setTag(Catalog.Brandon.getBookSerie(groupPosition).getBookAt(childPosition * finalTotalBooksInRow + i));
                    }
                }

                return row;
            }

            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition)
            {
                return false;
            }
        });

        // Expand all books
        for (int i = 0; i < Catalog.Brandon.getSeriesCount(); i++)
            booksView.expandGroup(i, false);

        setHasOptionsMenu(true);
        isExpanded = true;
        return rootView;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.books_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    private boolean isExpanded;
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {
            case R.id.books_toggle_expansion:
                if(isExpanded)
                {
                    item.setTitle("Expand All");
                    item.setIcon(R.drawable.ic_expand_more_white_36dp);
                    // Collapse all books
                    for (int i = 0; i < Catalog.Brandon.getSeriesCount(); i++)
                        booksView.collapseGroup(i);
                }
                else
                {
                    item.setTitle("Collapse All");
                    item.setIcon(R.drawable.ic_expand_less_white_36dp);
                    // Expand all books
                    for (int i = 0; i < Catalog.Brandon.getSeriesCount(); i++)
                        booksView.expandGroup(i, false);
                }
                isExpanded = !isExpanded;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
