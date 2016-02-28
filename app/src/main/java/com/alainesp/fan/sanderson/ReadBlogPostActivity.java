// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to read a blog post
 */
public class ReadBlogPostActivity extends AppCompatActivity
{
    private TextView date = null;
    private TextView categories = null;
    private TextView html_content = null;
    /**
     * Date formatter to show blog post publication date.
     */
    private final SimpleDateFormat showDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_blog_post);

        // Blog post title
        setTitle(BlogPostsFragment.readingPost.title);
        // Date
        date = (TextView) findViewById(R.id.blog_post_date);
        date.setText(showDateFormat.format(BlogPostsFragment.readingPost.publicationDate));

        categories = (TextView) findViewById(R.id.blog_post_categories);
        categories.setText(BlogPostsFragment.readingPost.getCategories());
        // Blob post content
        html_content = (TextView) findViewById(R.id.blog_post_content);
        // Handle lists that textview is incapable of handling
        String textViewFriendlyHtml = BlogPostsFragment.readingPost.html_content.replace("<dt>", "<br/><b>- ").replace("</dt>", "</b><br/>");
        html_content.setText(Html.fromHtml(textViewFriendlyHtml, new InternetHelper.UrlImageGetter(), null));
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
        getMenuInflater().inflate(R.menu.zoom_menu, menu);

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
}

/**
 * Class to handle links in-app and images zoom.
 */
class LocalLink extends LinkMovementMethod
{
    public LocalLink(ImageView aExpandedImageView, View aContainer, ScrollView aScrollView)
    {
        expandedImageView = aExpandedImageView;
        container = aContainer;
        scrollView = aScrollView;
        // Retrieve and cache the system's default "short" animation time and the expanded and container view used in cover zoom animation.
        mShortAnimationDuration = expandedImageView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
    }
    // Hold a reference to the current animator,  so that it can be canceled mid-way.
    private Animator mCurrentAnimator;
    /**
     * Provide Expand animation of images
     */
    private ImageView expandedImageView;
    private View container;
    private ScrollView scrollView;
    // The system "short" animation time duration, in milliseconds. This duration is ideal
    // for subtle animations or animations that occur very frequently.
    private int mShortAnimationDuration;

    /**
     * Expand an image from thumbnail to occupy all the screen.
     * This code is from Android Training, Adding Animations, Zooming a View with small changes by Alain Espinosa
     * @param image The image to enlarge
     */
    private void zoomImageFromThumb(int left, int top, final Drawable image)
    {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null)
            mCurrentAnimator.cancel();

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = image.copyBounds();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        container.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(left, top);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageDrawable(image);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        final float startScaleX, startScaleY;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height())
        {
            // Extend start bounds horizontally
            startScaleY = startScaleX = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScaleX * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;

            expandedImageView.getLayoutParams().height = finalBounds.height();
        }
        else
        {
            // Extend start bounds vertically
            startScaleX = (float) startBounds.width() / finalBounds.width();
//            float startHeight = startScale * finalBounds.height();
//            float deltaHeight = (startHeight - startBounds.height()) / 2;
//            startBounds.top -= deltaHeight;
//            startBounds.bottom += deltaHeight;

            // The zooming image will be near the initial one
            int finalHeight = (int) (startBounds.height()/startScaleX + 0.5);
            startScaleY = (float) startBounds.height() / finalHeight;
            int overHeight = finalHeight - startBounds.height();
            finalBounds.top = startBounds.top - overHeight/2;
            // Maintain image inside screen
            if(finalBounds.top < 0)// outside top
            {
                finalBounds.top = 0;
                finalBounds.bottom = finalHeight;
            }
            else if(finalBounds.top + finalHeight > finalBounds.bottom)// outside bottom
                finalBounds.top = finalBounds.bottom - finalHeight;
            else// totally inside
                finalBounds.bottom = finalBounds.top + finalHeight;

            expandedImageView.getLayoutParams().height = finalHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        //thumbView.setAlpha(0f);
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
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleX, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleY, 1f));
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
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleX))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleY));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        //thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                        float scaleFactor = InternetHelper.UrlImageGetter.getScaleFactor(image);
                        image.setBounds(0, 0, (int)(image.getIntrinsicWidth()*scaleFactor), (int)(image.getIntrinsicHeight()*scaleFactor));
                    }

                    @Override
                    public void onAnimationCancel(Animator animation)
                    {
                        //thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                        float scaleFactor = InternetHelper.UrlImageGetter.getScaleFactor(image);
                        image.setBounds(0, 0, (int)(image.getIntrinsicWidth()*scaleFactor), (int)(image.getIntrinsicHeight()*scaleFactor));
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ImageSpan[] images = buffer.getSpans(off, off, ImageSpan.class);
            if(images.length > 0)
            {
                int paddingLeft = expandedImageView.getContext().getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                // Find the image that were clicked
                int imgIndex = 0;
                if(images.length > 1)
                {
                    // Find all images in the last line
                    int imgIndexBase = 0;
                    for (int lineImgsWidth = 0; imgIndex < images.length; imgIndex++)
                    {
                        int imgWidth = images[imgIndex].getDrawable().getBounds().width();
                        lineImgsWidth += imgWidth;
                        if (lineImgsWidth > widget.getWidth())
                        {
                            lineImgsWidth = imgWidth;
                            imgIndexBase = imgIndex;
                        }
                    }
                    // Found index in line
                    imgIndex = imgIndexBase;
                    for (int imgX = x - paddingLeft; imgIndex < (images.length-1); imgIndex++)
                    {
                        int imgWidth = images[imgIndex].getDrawable().getBounds().width();

                        if (imgX < imgWidth)
                            break;
                        else
                        {
                            imgX -= imgWidth;
                            paddingLeft += imgWidth;
                        }
                    }
                }

                zoomImageFromThumb(paddingLeft, (int) (layout.getLineTop(line) - scrollView.getScrollY() + widget.getY()), images[imgIndex].getDrawable());
                return true;
            }
            else
            {
                URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

                if (link.length != 0)
                {
                    // Is an event?
                    if(DownloadAndParseWorker.WEB_EVENTS.equals(link[0].getURL()))
                    {
                        Intent intent = new Intent(container.getContext(), MainActivity.class);
                        intent.putExtra(MainActivity.APP_FRAGMENT_INDEX, MainActivity.APP_STATE_EVENTS);
                        container.getContext().startActivity(intent);
                    }
                    else
                    {
                        // Check if the link is to a book
                        Book refBook = Catalog.Brandon.getBookByOfficialLink(link[0].getURL());
                        if (refBook != null)
                        {
                            Catalog.readingBook = refBook;
                            Intent intent = new Intent(container.getContext(), BookActivity.class);
                            container.getContext().startActivity(intent);
                        }
                        else
                        {
                            String linkUrl = link[0].getURL();
                            // Check if the link is to a blog post
                            long id = DB.BlogPost.getBlogPostIDFromLink(linkUrl);
                            if (id >= 0 && BlogPostsFragment.blogPosts != null)
                            {
                                for (DB.BlogPost post : BlogPostsFragment.blogPosts)
                                    if (post.guid == id)
                                    {
                                        post.showToUser(container.getContext());
                                        break;
                                    }
                            }
                            else
                            {
                                // Handle links to Word of Radiance Reread
                                DB.TorRereadPost post = DB.TorRereadPost.getPostFromLink(linkUrl);
                                if(post != null)
                                {
                                    post.showToUser(container.getContext());
                                }
                                // Handle comments in Tor Stormlight Reread
                                else if (linkUrl.startsWith(ReadTorCommentActivity.commentsLinkPrefix))
                                {
                                    String data = link[0].getURL().substring(ReadTorCommentActivity.commentsLinkPrefix.length());
                                    TorRereadCommentsActivity.showComment(Integer.parseInt(data), container.getContext());
                                }
                                // Otherwise -> open link
                                else// TODO: Handle links to other sections
                                    link[0].onClick(widget);
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }
}
