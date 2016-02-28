// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.internal.NavigationMenuItemView;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Hashtable;
import java.util.List;

/**
 * The main activity of the application.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    private boolean isForeground = false;
    /**
     * A static reference to this activity. Used as a context for example.
     */
    public static MainActivity staticRef;
    /**
     * The navigation view of the drawer
     */
    private static NavigationView navigationView = null;

    // In witch state the app is
    public static final String APP_FRAGMENT_INDEX = "app_state";
    private int app_state = APP_STATE_SUMMARY;
    private static final int APP_STATE_ABOUT = -1;
    static final int APP_STATE_SUMMARY = 0;
    static final int APP_STATE_BOOKS = 1;
    static final int APP_STATE_BLOG = 2;
    static final int APP_STATE_EVENTS = 3;
    static final int APP_STATE_TWITTER = 4;
    private static final int APP_STATE_WOK = 5;
    static final int APP_STATE_WOR = 6;
    private static final int APP_STATE_17SHARD = 7;
    private static final int APP_STATE_SETTINGS = 8;

    /**
     * Navigate to a given menu index
     * @param appState The index
     */
    public static void navigateTo(int appState)
    {
        Menu drawerMenu = navigationView.getMenu();
        MenuItem selectItem;

//        if(appState >= APP_STATE_WOK && appState <= APP_STATE_WOR)
//        {
//            selectItem = drawerMenu.getItem(APP_STATE_WOK).getSubMenu().getItem(appState-APP_STATE_WOK);
//        }
//        else
            selectItem = drawerMenu.getItem(appState);

        staticRef.onNavigationItemSelected(selectItem);
    }

    // Handle badge number in the Drawer. This is something of a hack
    private static final int[] badgeNumbers = new int[8];
    /**
     * A hashtable to obtain menu index from the menu text.
     */
    private static final Hashtable<String, Integer> reverseMenuText = new Hashtable<>(32);

    static {
        // Set the badge numbers
        for (int i = 0; i < badgeNumbers.length; i++)
            badgeNumbers[i] = 0;
    }

    /**
     * Set a menu item in the drawer with a badge.
     * @param menuIndex The index of the menu item
     * @param badge The number ot add as a badge
     */
    public static void setBadgeNumber(int menuIndex, int badge)
    {
        badgeNumbers[menuIndex] = badge;
        setBadgesNumberUI();
    }

    /**
     * Decrement the badge.
     * @param menuIndex The index of the menu item.
     */
    public static void decBadgeNumber(int menuIndex)
    {
        badgeNumbers[menuIndex]--;
        setBadgesNumberUI();
    }

    /**
     * Set the badge number to all menu items visible
     */
    private static void setBadgesNumberUI()
    {
        if(navigationView != null)
        {
            NavigationMenuView v = (NavigationMenuView) navigationView.getChildAt(0);
            Menu drawerMenu = navigationView.getMenu();

            if (v != null)
                // Iterate all children
                for (int childIndex = 0; childIndex < v.getChildCount(); childIndex++)
                {
                    View v1 = v.getChildAt(childIndex);
                    if (v1 instanceof NavigationMenuItemView)
                    {
                        TextView mTextView = (TextView) ((NavigationMenuItemView) v1).getChildAt(0);
                        if (mTextView != null)
                        {
                            // Get the menu index
                            Integer menuIndex = reverseMenuText.get(mTextView.getText().toString());
                            if (menuIndex != null && menuIndex < badgeNumbers.length)
                            {
                                Drawable numberText = null;
                                if (badgeNumbers[menuIndex] > 0)
                                {
                                    int height = mTextView.getHeight();
                                    numberText = new TextDrawable(badgeNumbers[menuIndex], mTextView.getTextSize(), mTextView.getCurrentTextColor(), height);
                                    numberText.setBounds(0, 0, height, height);
                                }

                                // Similar to NavigationMenuItemView.setIcon
                                Drawable icon = drawerMenu.getItem(menuIndex).getIcon();
                                Drawable.ConstantState state = icon.getConstantState();
                                icon = DrawableCompat.wrap(state == null ? icon : state.newDrawable()).mutate();
                                int mIconSize = navigationView.getContext().getResources().getDimensionPixelSize(android.support.design.R.dimen.design_navigation_icon_size);
                                icon.setBounds(0, 0, mIconSize, mIconSize);
                                DrawableCompat.setTintList(icon, navigationView.getItemIconTintList());
                                TextViewCompat.setCompoundDrawablesRelative(mTextView, icon, null, numberText, null);
                            }
                        }
                    }
                }
        }
    }

    /**
     * Initialize the badge numbers
     */
    private static void loadBadgeNumbers()
    {
        // BlogPosts
        setBadgeNumber(APP_STATE_BLOG, DB.BlogPost.getStatus().countUnread);
        // Events
        setBadgeNumber(APP_STATE_EVENTS, DB.Event.getStatus().countUnread);
        // Books
        setBadgeNumber(APP_STATE_BOOKS, Catalog.Brandon.getUnpublishedBookCount());
        // Twitter
        setBadgeNumber(APP_STATE_TWITTER, DB.Tweet.getStatus().countUnread);
        // WoR Reread
        setBadgeNumber(APP_STATE_WOR, DB.TorRereadPost.getTotalUnread());
    }

    /**
     * A Drawable to show number as a text
     */
    private static class TextDrawable extends Drawable
    {
        private final float mTextSize;
        private final int height;
        private final Paint paint;

        public TextDrawable(int level, float aTextSize, int aTextColor, int aHeight)
        {
            mTextSize = aTextSize * 7 / 10;// NOTE: Try to fix the *7/10 patch
            height = aHeight;
            setLevel(level);

            paint = new Paint();
            paint.setColor(aTextColor);
            paint.setTextSize(aTextSize);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        public int getIntrinsicWidth() {
            return height;
        }
        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public void draw(Canvas canvas)
        {
            if(getLevel() > 0)
                canvas.drawText("" + getLevel(), height, (height + mTextSize) / 2, paint);
        }

        @Override
        public void setAlpha(int alpha){}

        @Override
        public void setColorFilter(ColorFilter colorFilter){}

        @Override
        public int getOpacity()
        {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Is an app that can handle this intent?
     * @param intent The intent to check
     * @return If the intent can be handle
     */
    public static boolean isIntentSafe(Intent intent)
    {
        // Verify it resolves
        PackageManager packageManager = staticRef.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        return activities.size() > 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        staticRef = this;

        super.onCreate(savedInstanceState);
        DB.currentContext = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu drawerMenu = navigationView.getMenu();
        // Create the hashtable
        reverseMenuText.clear();
        for (int i = 0; i < drawerMenu.size(); i++)
            reverseMenuText.put(drawerMenu.getItem(i).getTitle().toString(), i);
        navigationView.setNavigationItemSelectedListener(this);
        // 2 listeners to show the badges
        navigationView.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
        {
            @Override
            public void onLayoutChange(View vv, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
            {
                setBadgesNumberUI();
            }
        });
        ((NavigationMenuView) navigationView.getChildAt(0)).addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
                setBadgesNumberUI();
            }
        });

        // Select Summary at the beginning
        loadBadgeNumbers();
        // Try to load the state in which the app was
        if(savedInstanceState != null)
            app_state = savedInstanceState.getInt(APP_FRAGMENT_INDEX);
        else
        {
            Intent intent = getIntent();
            if (intent != null)
                app_state = intent.getIntExtra(APP_FRAGMENT_INDEX, -2);

            if(app_state == -2)
                app_state = APP_STATE_SUMMARY;
        }

        if(app_state==APP_STATE_ABOUT)
            onAboutClick(null);
        else
            navigateTo((app_state));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        isForeground = true;
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        isForeground = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putInt(APP_FRAGMENT_INDEX, app_state);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        loadBadgeNumbers();

        if(savedInstanceState != null)
            app_state = savedInstanceState.getInt(APP_FRAGMENT_INDEX);

        if(app_state==APP_STATE_ABOUT)
            onAboutClick(null);
        else
            navigateTo(app_state);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else// Close app when in Summary, otherwise go to Summary. Consider here other navigation possibilities
        {
            if(app_state == APP_STATE_SUMMARY)
                super.onBackPressed();
            else
                navigateTo(APP_STATE_SUMMARY);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(!item.isChecked())
        {
            if (id != R.id.nav_about)
            {
                this.setTitle(item.getTitle());
                item.setChecked(true);
            }

            if (id == R.id.nav_main)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new SummaryFragment()).commit();
                app_state = APP_STATE_SUMMARY;
            }
            else if (id == R.id.nav_blog)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new BlogPostsFragment()).commit();
                app_state = APP_STATE_BLOG;
            }
            else if (id == R.id.nav_events)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new EventsFragment()).commit();
                app_state = APP_STATE_EVENTS;
            }
            else if (id == R.id.nav_books)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new BooksFragment()).commit();
                app_state = APP_STATE_BOOKS;
            }
            else if (id == R.id.nav_twitter)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new TwitterFragment()).commit();
                app_state = APP_STATE_TWITTER;
            }
            else if (id == R.id.nav_tor0)
            {
                Uri webpage = Uri.parse("http://www.tor.com/features/series/the-way-of-kings-reread-on-torcom/");
                Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                if(isIntentSafe(webIntent))
                    startActivity(webIntent);

                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new TorWoKRereadFragment()).commit();
                app_state = APP_STATE_WOK;
                this.setTitle("The Way of Kings Reread");
            }
            else if (id == R.id.nav_tor1)
            {
                //Uri webpage = Uri.parse("http://www.tor.com/series/words-of-radiance-reread-on-torcom/");
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new TorWoRRereadFragment()).commit();
                app_state = APP_STATE_WOR;
                this.setTitle("Words of Radiance Reread");
            }
            else if (id == R.id.nav_17shard)// TODO: Handle posts and news in the app
            {
                Uri webpage = Uri.parse("http://www.17thshard.com/forum/");
                Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
                if(isIntentSafe(webIntent))
                    startActivity(webIntent);

                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new Shard17thFragment()).commit();
                app_state = APP_STATE_17SHARD;
            }
            else if (id == R.id.nav_settings)
            {
                getFragmentManager().beginTransaction().replace(R.id.main_fragment, new SettingsFragment()).commit();
                app_state = APP_STATE_SETTINGS;
            }
            else if (id == R.id.nav_about)
            {
                try
                {
                    new AlertDialog.Builder(this)
                            .setTitle("About")
                            .setMessage(Html.fromHtml("<em>Fan of Sanderson</em> " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " BETA" +
                                    "<br/><br/>Application made by fans to fans.<br/><br/>"+
                                    "Developer: Alain Espinosa &lt;alainesp@gmail.com&gt;"))
                            .setPositiveButton("OK", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
                catch (PackageManager.NameNotFoundException ignored){}
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle a click to the header of the drawer.
     * @param view Unused.
     */
    public void onAboutClick(View view)
    {
        app_state = APP_STATE_ABOUT;

        for (int i = 0; i < navigationView.getMenu().size(); i++)
            navigationView.getMenu().getItem(i).setChecked(false);

        setTitle("About Brandon Sanderson");
        getFragmentManager().beginTransaction().replace(R.id.main_fragment, new AboutBrandonFragment()).commit();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }
}
