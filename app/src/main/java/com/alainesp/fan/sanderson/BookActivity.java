// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to show details of a book and provide navigation of books in the same serie
 */
public class BookActivity extends AppCompatActivity
{
    private static final DateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    /**
     * Pager to provide Slide animation from book to book in the same serie
     */
    private ViewPager mPager;
    /**
     * Book serie to show
     */
    private static BookSerie serie;
    // The system "short" animation time duration, in milliseconds. This duration is ideal
    // for subtle animations or animations that occur very frequently.
    private static int mShortAnimationDuration;
    /**
     * Provide Expand animation of the book cover
     */
    private static ImageView expandedImageView;
    private static View container;

    /**
     * Fragment to show a given book
     */
    public static class ScreenSlidePageFragment extends Fragment
    {
        private ViewGroup rootView;
        private Book book;
        //private BookActivity context;
        private static final String BOOK_INDEX = "BOOK_INDEX";
        // Hold a reference to the current animator, so that it can be canceled mid-way.
        private Animator mCurrentAnimator;

        public ScreenSlidePageFragment()
        {
        }
        /**
         * Set a book
         * @param aBook The book whose data is showed
         */
        public Fragment setBook(Book aBook)
        {
            book = aBook;
            return this;
        }

        @Override
        public void onSaveInstanceState(Bundle outState)
        {
            if(book != null)
                outState.putInt(BOOK_INDEX, serie.bookIndexOf(book));

            super.onSaveInstanceState(outState);
        }

        /**
         * Utility method to format and show data as "HEADER: context"
         * @param resID The text view to show data in
         * @param header The header
         * @param value The content
         * @return The text view
         */
        private TextView setTextFormatted(int resID, String header, String value)
        {
            TextView textView = (TextView) rootView.findViewById(resID);
            textView.setText(Html.fromHtml("<b>" + header + ": </b>" + value));

            return textView;
        }

        /**
         * Expand an image from thumbnail to occupy all the screen.
         * This code is from Android Training, Adding Animations, Zooming a View
         * @param thumbView The small view to enlarge
         * @param image The image to enlarge
         */
        private void zoomImageFromThumb(final View thumbView, final Drawable image)
        {
            // If there's an animation in progress, cancel it
            // immediately and proceed with this one.
            if (mCurrentAnimator != null)
                mCurrentAnimator.cancel();

            // Load the high-resolution "zoomed-in" image.
            expandedImageView.setImageDrawable(image);

            // Calculate the starting and ending bounds for the zoomed-in image.
            // This step involves lots of math. Yay, math.
            final Rect startBounds = new Rect();
            final Rect finalBounds = new Rect();
            final Point globalOffset = new Point();

            // The start bounds are the global visible rectangle of the thumbnail,
            // and the final bounds are the global visible rectangle of the container
            // view. Also set the container view's offset as the origin for the
            // bounds, since that's the origin for the positioning animation
            // properties (X, Y).
            thumbView.getGlobalVisibleRect(startBounds);
            container.getGlobalVisibleRect(finalBounds, globalOffset);
            startBounds.offset(-globalOffset.x, -globalOffset.y);
            finalBounds.offset(-globalOffset.x, -globalOffset.y);

            // Adjust the start bounds to be the same aspect ratio as the final
            // bounds using the "center crop" technique. This prevents undesirable
            // stretching during the animation. Also calculate the start scaling
            // factor (the end scaling factor is always 1.0).
            float startScale;
            if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height())
            {
                // Extend start bounds horizontally
                startScale = (float) startBounds.height() / finalBounds.height();
                float startWidth = startScale * finalBounds.width();
                float deltaWidth = (startWidth - startBounds.width()) / 2;
                startBounds.left -= deltaWidth;
                startBounds.right += deltaWidth;
            }
            else
            {
                // Extend start bounds vertically
                startScale = (float) startBounds.width() / finalBounds.width();
                float startHeight = startScale * finalBounds.height();
                float deltaHeight = (startHeight - startBounds.height()) / 2;
                startBounds.top -= deltaHeight;
                startBounds.bottom += deltaHeight;
            }

            // Hide the thumbnail and show the zoomed-in view. When the animation
            // begins, it will position the zoomed-in view in the place of the
            // thumbnail.
            thumbView.setAlpha(0f);
            expandedImageView.setVisibility(View.VISIBLE);

            // Set the pivot point for SCALE_X and SCALE_Y transformations
            // to the top-left corner of the zoomed-in view (the default
            // is the center of the view).
            expandedImageView.setPivotX(0f);
            expandedImageView.setPivotY(0f);

            // Construct and run the parallel animation of the four translation and
            // scale properties (X, Y, SCALE_X, and SCALE_Y).
            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
                    .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                    .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
            set.setDuration(mShortAnimationDuration);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    mCurrentAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation)
                {
                    mCurrentAnimator = null;
                }
            });
            set.start();
            mCurrentAnimator = set;

            // Upon clicking the zoomed-in image, it should zoom back down
            // to the original bounds and show the thumbnail instead of
            // the expanded image.
            final float startScaleFinal = startScale;
            expandedImageView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (mCurrentAnimator != null)
                    {
                        mCurrentAnimator.cancel();
                    }

                    // Animate the four positioning/sizing properties in parallel,
                    // back to their original values.
                    AnimatorSet set = new AnimatorSet();
                    set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                            .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                            .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                            .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                    set.setDuration(mShortAnimationDuration);
                    set.setInterpolator(new DecelerateInterpolator());
                    set.addListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            thumbView.setAlpha(1f);
                            expandedImageView.setVisibility(View.GONE);
                            mCurrentAnimator = null;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation)
                        {
                            thumbView.setAlpha(1f);
                            expandedImageView.setVisibility(View.GONE);
                            mCurrentAnimator = null;
                        }
                    });
                    set.start();
                    mCurrentAnimator = set;
                }
            });
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            rootView = (ViewGroup) inflater.inflate(R.layout.activity_book, container, false);

            if(savedInstanceState != null)
                book = serie.getBookAt(savedInstanceState.getInt(BOOK_INDEX));

            // Normal cover
            final ImageView coverView = (ImageView)rootView.findViewById(R.id.book_cover);
            final Drawable coverImg = book.loadCover();
            coverView.setImageDrawable(coverImg);
            // Provide tap to zoom animation
            coverView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    zoomImageFromThumb(coverView, coverImg);
                }
            });

            // Set small details of book
            NumberFormat intFormat = NumberFormat.getIntegerInstance();
            intFormat.setGroupingUsed(true);
            setTextFormatted(R.id.book_serie, "Serie", book.parent.title != null ? book.parent.title : book.parent.parent.title);
            setTextFormatted(R.id.book_genre, "Genre", book.parent.genre);
            setTextFormatted(R.id.book_pub_date, book.isPublished() ? "Released" : "Expected", dateFormat.format(book.publishedDate));
            setTextFormatted(R.id.book_pages_chapters, "Pages", intFormat.format(book.pages) + " <b>Chapters: </b>" + intFormat.format(book.chapters));
            setTextFormatted(R.id.book_words_audio, "Words", intFormat.format(book.words) + " <b>Audio: </b>" + (book.audioTime != null ? book.audioTime : "0h 0m"));
            // The rate
            TextView rate = (TextView) rootView.findViewById(R.id.book_rate);
            rate.setText("" + book.rate + " - " + intFormat.format(book.ratings) + " ratings");
            RatingBar starBar = (RatingBar)rootView.findViewById(R.id.book_ratingbar);
            starBar.setRating(book.rate);

            // Add awards if any
            String htmlAwards = book.getFormattedAwards();
            LinearLayout bookInfo = (LinearLayout) rootView.findViewById(R.id.book_info);
            Context context = getActivity().getBaseContext();
            if(htmlAwards != null)
            {
                LinearLayout awardsLayout = new LinearLayout(context);

                // Image
                ImageView img = new ImageView(context);
                img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                img.setImageResource(R.drawable.award);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(context.getResources().getDimensionPixelSize(R.dimen.award_width), context.getResources().getDimensionPixelSize(R.dimen.award_height));
                params.gravity = Gravity.CENTER_VERTICAL;
                awardsLayout.addView(img, params);
                // The awards
                TextView book_awards = new TextView(context);
                book_awards.setPadding(context.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0, 0);
                book_awards.setText(Html.fromHtml(htmlAwards));
                book_awards.setTextColor(Color.BLACK);
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER_VERTICAL;
                awardsLayout.addView(book_awards, params);

                bookInfo.addView(awardsLayout);
            }
            else// Add links here to balance the GUI. Without them looks with holes
            {
                TextView book_links = new TextView(context);
                book_links.setText(Html.fromHtml(book.getFormattedLinks(true)));
                book_links.setTextColor(Color.BLACK);
                book_links.setMovementMethod(LinkMovementMethod.getInstance());
                bookInfo.addView(book_links);
            }

            // Synopsis
            TextView synopsis = (TextView) rootView.findViewById(R.id.book_synopsis);
            synopsis.setText(book.synopsis);
            synopsis.setMovementMethod(LinkMovementMethod.getInstance());
            synopsis.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsFragment.getTextSize());

            // Add links at the footer of the view
            TextView links = setTextFormatted(R.id.book_links, "Links", book.getFormattedLinks(false)); links.setMovementMethod(LinkMovementMethod.getInstance());
            links.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsFragment.getTextSize());

            return rootView;
        }
    }

    /**
     * Adapter to provide books to the pager
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
            return new ScreenSlidePageFragment().setBook(serie.getBookAt(position));
        }

        @Override
        public int getCount()
        {
            return serie.getBookCount();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        // Set the title of the book
        setTitle(Catalog.readingBook.title);
        serie = Catalog.readingBook.parent.parent;

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new ScreenSlidePagerAdapter(getSupportFragmentManager()));
        mPager.setCurrentItem(Catalog.readingBook.getIndexInSerie());
        // Listener to change Title and ActionMenus based on book selected
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){}

            @Override
            public void onPageSelected(int position)
            {
                Catalog.readingBook = serie.getBookAt(position);
                setTitle(Catalog.readingBook.title);
                if(prevBookItem != null) prevBookItem.setVisible(position > 0);
                if(nextBookItem != null) nextBookItem.setVisible((position + 1) < serie.getBookCount());
            }

            @Override
            public void onPageScrollStateChanged(int state){}
        });

        // Retrieve and cache the system's default "short" animation time and the expanded and container view used in cover zoom animation.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        expandedImageView = (ImageView) findViewById(R.id.slider_expanded_image);
        container = findViewById(R.id.slider_container);
    }

    /**
     * The ActionMenus to provide navigation in the book serie
     */
    private MenuItem nextBookItem, prevBookItem;
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_book_menu, menu);

        nextBookItem = menu.findItem(R.id.book_menu_next);
        prevBookItem = menu.findItem(R.id.book_menu_prev);

        prevBookItem.setVisible(Catalog.readingBook.getIndexInSerie() > 0);
        nextBookItem.setVisible((Catalog.readingBook.getIndexInSerie() + 1) < serie.getBookCount());

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
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
