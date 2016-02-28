// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.res.XmlResourceParser;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: Cache almost all calculation in this classes

/**
 * Provide a Catalog of books
 */
public class Catalog
{
    /**
     * The Brandon Sanderson Catalog of books
     */
    public static final Catalog Brandon = new Catalog();
    /**
     * The book whose details are currently showing
     */
    public static Book readingBook = null;

    /**
     * A list with all book series by Brandon
     */
    private final List<BookSerie> brandonBooks = new ArrayList<>(16);

    private Catalog()
    {
        // Get a XML parser of the in-app copy of books data
        XmlResourceParser parser = MainActivity.staticRef.getResources().getXml(R.xml.books_data);

        try// Parse all
        {
            while (parser.next() != XmlPullParser.END_DOCUMENT)
                if (parser.getEventType() ==  XmlPullParser.START_TAG && "BookSerie".equals(parser.getName()))
                    brandonBooks.add(BookSerie.parseData(parser));
        }
        catch (Exception e)// This never execute given we are currently using local copies
        {
            Logger.reportError(e.toString());
        }
        finally
        {
            parser.close();
        }
    }

    /**
     * Get the number of book series
     * @return The count of book series
     */
    public int getSeriesCount()
    {
        return brandonBooks.size();
    }

    /**
     * Get the given book serie
     * @param index The index of the book serie in the catalog
     * @return The book serie
     */
    public BookSerie getBookSerie(int index)
    {
        return brandonBooks.get(index);
    }

    /**
     * Gte the number of books that are announced but still not published
     * @return The number of books
     */
    public int getUnpublishedBookCount()
    {
        int unpublishedBookCount = 0;
        for (int i = 0; i < brandonBooks.size(); i++)
            unpublishedBookCount += brandonBooks.get(i).getBooksNewCount();

        return unpublishedBookCount;
    }

    /**
     * Get all books count
     * @return The total book count in the catalog
     */
    public int getTotalBookCount()
    {
        int totalBooks = 0;
        for (int i = 0; i < brandonBooks.size(); i++)
            totalBooks += brandonBooks.get(i).getBookCount();

        return totalBooks;
    }

    /**
     * Get the books that are announced but still unpublished
     * @return A list with books
     */
    public List<Book> getUnpublishedBooks()
    {
        List<Book> newBooks = new ArrayList<>(5);

        for (int i = 0; i < brandonBooks.size(); i++)
            brandonBooks.get(i).addNewBooks(newBooks);

        return newBooks;
    }

    /**
     * Get the book given a link
     * @param link The link to search the Catalog
     * @return The book or null if not found
     */
    public Book getBookByOfficialLink(String link)
    {
        for (BookSerie serie : brandonBooks)
        {
            Book linkBook = serie.getBookByOfficialLink(link);
            if(linkBook != null)
                return linkBook;
        }
        return null;
    }
}

/**
 * A serie of books
 */
class BookSerie
{
    /**
     * The title of the serie
     */
    public final String title;
    /**
     * This is used as a cache for miniserie info
     */
    private Spanned miniSeriesDataCache = null;
    /**
     * The miniseries in the serie. See BookMiniSerie for an explanation
     */
    private final ArrayList<BookMiniSerie> miniSeries;

    private BookSerie(String aTitle)
    {
        title = aTitle;
        miniSeries = new ArrayList<>(4);
    }

    /**
     * Get MiniSeries info
     * @return The formatted miniseries information
     */
    public Spanned getMiniSeriesData()
    {
        // Create the cache the first time
        if(miniSeriesDataCache == null)
        {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < miniSeries.size(); i++)
            {
                if (i > 0)
                    builder.append("&nbsp;&nbsp;&nbsp;");

                builder.append(miniSeries.get(i).toString());
            }

            miniSeriesDataCache = Html.fromHtml(builder.toString());
        }

        return miniSeriesDataCache;
    }

    /**
     * Get the number of books announced but still unpublished
     * @return The count of books
     */
    public int getBooksNewCount()
    {
        int newBooksCount = 0;
        for (BookMiniSerie miniSerie : miniSeries)
            newBooksCount += miniSerie.getNewBookCount();

        return newBooksCount;
    }

    /**
     * Get the number of books expected to finish this serie
     * @return The number of books
     */
//    public int getExpectedBooksCount()
//    {
//        int expectedBooksCount = 0;
//        for (BookMiniSerie miniSerie : miniSeries)
//            expectedBooksCount += miniSerie.expectedBooksCount;
//
//        return expectedBooksCount;
//    }

    /**
     * Get the number of already published books
     * @return The number of books
     */
//    public int getPublishedBooksCount()
//    {
//        int publishedBookCount = 0;
//        for (BookMiniSerie miniSerie : miniSeries)
//            publishedBookCount += miniSerie.getPublishedBookCount();
//
//        return publishedBookCount;
//    }

    /**
     * Get a given book in the serie
     * @param index The index inside this serie
     * @return The book
     */
    public Book getBookAt(int index)
    {
        for (BookMiniSerie miniSerie : miniSeries)
        {
            if (miniSerie.getBookCount() <= index)
                index -= miniSerie.getBookCount();
            else
                return miniSerie.books.get(index);
        }

        return null;
    }

    public int bookIndexOf(Book book)
    {
        for (BookMiniSerie miniSerie : miniSeries)
        {
            int bookIndex = miniSerie.books.indexOf(book);
            if(bookIndex >= 0)
                return bookIndex;
        }

        return -1;
    }

    /**
     * The current number of books
     * @return Books count
     */
    public int getBookCount()
    {
        int bookCount = 0;
        for (BookMiniSerie miniSerie : miniSeries)
            bookCount += miniSerie.getBookCount();

        return bookCount;
    }

    /**
     * Get the number of ratings for the serie (Sum of each book).
     * This data is obtained from Goodread.
     * @return The number of ratings
     */
    public int getRatingsCount()
    {
        int ratingCount = 0;
        for (BookMiniSerie miniSerie : miniSeries)
            ratingCount += miniSerie.getRatingsCount();

        return ratingCount;
    }

    /**
     * Get the rate of this serie in the range [0-5] star.
     * This data is obtained from Goodread.
     * @return The rate of the serie
     */
    public float getRate()
    {
        double sum_rate = 0;

        for (BookMiniSerie miniSerie : miniSeries)
            sum_rate += miniSerie.getSumRate();

        return (float)(sum_rate / getRatingsCount());
    }

    /**
     * Add the books that are announced but still unpublished
     * @param newBooks The list to add books
     */
    public void addNewBooks(List<Book> newBooks)
    {
        for (BookMiniSerie miniSerie : miniSeries) miniSerie.addNewBooks(newBooks);
    }

    /**
     * Parse the XML and load a serie
     * @param parser The XML parser
     * @return The BookSerie loaded from the XML
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ParseException
     */
    public static BookSerie parseData(XmlResourceParser parser) throws IOException, XmlPullParserException, ParseException
    {
        BookSerie serie = new BookSerie(parser.getAttributeValue(null, "title"));

        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            // Load MiniSeries
            if (parser.getEventType() == XmlPullParser.START_TAG && "BookMiniSerie".equals(parser.getName()))
            {
                BookMiniSerie miniSerie = BookMiniSerie.parseData(parser);
                serie.miniSeries.add(miniSerie);
                miniSerie.parent = serie;
            }
            // Encounter the end tag of BookSerie
            else if ((parser.getEventType() == XmlPullParser.END_TAG && "BookSerie".equals(parser.getName())))
                break;
        }

        return serie;
    }

    /**
     * Get the book given a link
     * @param link The link to search the serie
     * @return The book or null if not found
     */
    public Book getBookByOfficialLink(String link)
    {
        for (BookMiniSerie miniSerie : miniSeries)
        {
            Book linkBook = miniSerie.getBookByOfficialLink(link);
            if(linkBook != null)
                return linkBook;
        }
        return null;
    }
}
/**
 * Miniserie inside a serie of books
 *
 * This is necessary to handle fine details of series and is used a lot by Brandon.
 * Big example is Mistborn with 4 miniseries planned, but this miniserie thing is used extensively.
 */
class BookMiniSerie
{
    /**
     * The full title of the miniserie
     */
    final String title;
    /**
     * The small title of the serie
     */
    private final String miniTitle;
    /**
     * The number of books brandon expect to conclude this miniserie
     */
    private final int expectedBooksCount;
    /**
     * The genre of this miniserie
     */
    final String genre;
    /**
     * Always true except in first "The Wheel of Time" miniserie
     */
    private final boolean isBrandonAuthor;

    /**
     * The books in this miniserie
     */
    public final ArrayList<Book> books;
    /**
     * The parent serie
     */
    public BookSerie parent = null;

    private BookMiniSerie(String aTitle, String aMiniTitle, int aExpectedBooksCount, String aGenre, boolean aIsBrandonAuthor)
    {
        title = aTitle;
        miniTitle = aMiniTitle;
        expectedBooksCount = aExpectedBooksCount;
        genre = aGenre;
        isBrandonAuthor = aIsBrandonAuthor;

        books = new ArrayList<>(5);
    }

    /**
     * Get the number of books announced or already published in this miniserie
     * @return The number of books
     */
    public int getBookCount()
    {
        return books.size();
    }

    /**
     * Get the number of books already published
     * @return The number of books
     */
    private int getPublishedBookCount()
    {
        if(!isBrandonAuthor)
            return expectedBooksCount;

        int publishedBookCount = 0;
        for (Book book : books)
            if (book.isPublished())
                publishedBookCount++;

        return publishedBookCount;
    }

    /**
     * Add books announced but still unpublished to a list
     * @param newBooks The list to add books
     */
    public void addNewBooks(List<Book> newBooks)
    {
        for (Book book : books)
            if (book.publishedDate.compareTo(new Date()) != -1)
                newBooks.add(book);
    }
    /**
     * Get the number of books announced but still unpublished
     * @return The number of books
     */
    public int getNewBookCount()
    {
        if(!isBrandonAuthor) return 0;

        return getBookCount() - getPublishedBookCount();
    }

    /**
     * Summarise this miniserie in a string representation
     * @return The miniTitle and new and total number of books
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        if(miniTitle != null)
        {
            builder.append(miniTitle);
            builder.append(": ");
        }
        builder.append(getPublishedBookCount());
        if(getNewBookCount() > 0)
        {
            builder.append("<b>+");
            builder.append(getNewBookCount());
            builder.append(" new</b>");
        }
        builder.append("/");
        builder.append(expectedBooksCount);

        return builder.toString();
    }

    /**
     * Parse a XML and load a MiniSerie
     * @param parser The XML parser
     * @return The miniserie
     * @throws XmlPullParserException
     * @throws IOException
     * @throws ParseException
     */
    public static BookMiniSerie parseData(XmlResourceParser parser) throws XmlPullParserException, IOException, ParseException
    {
        String boolVal = parser.getAttributeValue(null, "isBrandonAuthor");

        BookMiniSerie miniSerie = new BookMiniSerie(parser.getAttributeValue(null, "title"),
                                                parser.getAttributeValue(null, "miniTitle"),
                                                Integer.parseInt(parser.getAttributeValue(null, "total")),
                                                parser.getAttributeValue(null, "genre"),
                                                boolVal == null || Boolean.parseBoolean(boolVal));

        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            // Load books
            if (parser.getEventType() == XmlPullParser.START_TAG && "Book".equals(parser.getName()))
            {
                Book book = Book.parseData(parser);
                miniSerie.books.add(book);
                book.parent = miniSerie;
            }
            // TODO: Load Publishers
            // Finish when end tag of BookMiniSerie is encounter
            else if ((parser.getEventType() == XmlPullParser.END_TAG && "BookMiniSerie".equals(parser.getName())))
                break;
        }

        return miniSerie;
    }

    /**
     * Get the number of ratings.
     * The ratings are obtained from Goodread.
     * @return The number of ratings
     */
    public int getRatingsCount()
    {
        int ratingCount = 0;
        for (Book book : books)
            ratingCount += book.ratings;

        return ratingCount;
    }

    /**
     * Get the sum of all ratings for all books in this miniserie.
     * The ratings are obtained from Goodread.
     * @return The sum of the rate of all books in the miniserie.
     */
    public double getSumRate()
    {
        double sumRate = 0;
        for (Book book : books)
            sumRate += book.ratings * ((double)book.rate);

        return sumRate;
    }

    /**
     * Get the book given a link
     * @param link The link to search the serie
     * @return The book or null if not found
     */
    public Book getBookByOfficialLink(String link)
    {
        for (Book book : books)
            if(book.containLink(link))
                return book;

        return null;
    }
}

/**
 * Book information or metadata
 */
class Book
{
    /**
     * Handle publication date format in XML
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * The title of the book
     */
    public final String title;
    /**
     * The url of the cover image
     */
    private final String cover;
    /**
     * The date of publication
     */
    public final Date publishedDate;
    /**
     * General metadata
     */
    int pages, chapters, words, ratings;
    float rate;
    String audioTime;
    /**
     * The miniserie parent
     */
    public BookMiniSerie parent;

    /**
     * The formatted synopsis of the book
     */
    public Spanned synopsis;
    /**
     * The list of awards won by this book
     */
    private ArrayList<String> awards;
    /**
     * Links to useful information about this book
     */
    private ArrayList<String> links;

    private Book(String aTitle, String aCover, Date aPublicationDate, int aPages, int aChapters, int aWords, float aRate, int aRatings, String aAudioTime)
    {
        title = aTitle;
        cover = aCover;
        publishedDate = aPublicationDate;
        pages = aPages;
        chapters = aChapters;
        words = aWords;
        rate = aRate;
        ratings = aRatings;
        audioTime = aAudioTime;

        parent = null;
        awards = new ArrayList<>(5);
        links = new ArrayList<>(5);

        // Ensure cover is ready
        if (cover != null && !cover.isEmpty())
            InternetHelper.getRemoteFile(cover);
    }

    /**
     * Load the cover image of the book
     * @return The cover of the book
     */
    public Drawable loadCover()
    {
        File coverFile = InternetHelper.getRemoteFile(cover);
        try
        {
            if(coverFile != null)
                return new BitmapDrawable(MainActivity.staticRef.getResources(), coverFile.getAbsolutePath());
        }
        catch (Exception e)// Bad image file?
        {
            coverFile.delete();
        }

        return MainActivity.staticRef.getResources().getDrawable(R.drawable.no_cover);
    }

    /**
     * Is the book is already published
     * @return If the book is already published
     */
    public boolean isPublished()
    {
        return publishedDate.compareTo(new Date()) < 0;
    }

    /**
     * Get the awards as a formatted string
     * @return The awards formatted as HTML
     */
    public String getFormattedAwards()
    {
        if(awards.size() == 0)
            return null;

        StringBuilder builder = new StringBuilder();
        builder.append(awards.get(0));

        for (int i = 1; i < awards.size(); i++)
        {
            builder.append("<br/>");
            builder.append(awards.get(i));
        }

        return builder.toString();
    }

    /**
     * Get the links formatted as a string
     * @param linkInNewLine If put each link in a new line
     * @return The links as html text
     */
    public String getFormattedLinks(boolean linkInNewLine)
    {
        if(links.size() == 0)
            return null;

        StringBuilder builder = new StringBuilder();
        builder.append(links.get(0));

        for (int i = 1; i < links.size(); i++)
        {
            builder.append(linkInNewLine ? "<br/>" : "&nbsp;&nbsp;");
            builder.append(links.get(i));
        }

        return builder.toString();
    }

    /**
     * Parse a book from XML
     * @param parser The XML parser
     * @return The book
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ParseException
     */
    public static Book parseData(XmlResourceParser parser) throws IOException, XmlPullParserException, ParseException
    {
        // Create the book
        Book book = new Book(parser.getAttributeValue(null, "title"),
                            parser.getAttributeValue(null, "cover"),
                dateFormat.parse(parser.getAttributeValue(null, "publicationDate")),
                Integer.parseInt(parser.getAttributeValue(null, "pages")),
                Integer.parseInt(parser.getAttributeValue(null, "chapters")),
                            Integer.parseInt(parser.getAttributeValue(null, "words")),
                            Float.parseFloat(parser.getAttributeValue(null, "rate")),
                            Integer.parseInt(parser.getAttributeValue(null, "ratings")),
                            parser.getAttributeValue(null, "audioTime"));

        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if (parser.getEventType() == XmlPullParser.START_TAG)
            {
                String tagName = parser.getName();
                if("Synopsis".equals(tagName))
                {
                    parser.next();
                    String synopsis = parser.getText();
                    book.synopsis = Html.fromHtml(synopsis != null ? synopsis : "");
                }
                else if("Award".equals(tagName) && "Won".equals(parser.getAttributeValue(null, "result")))
                {
                    // TODO: Create an Award class and redefine toString
                    book.awards.add("<b>" + parser.getAttributeValue(null, "title") + "</b> for " + parser.getAttributeValue(null, "category") + " (" + parser.getAttributeValue(null, "year") + ")");
                }
                else if("Link".equals(tagName))
                {
                    // TODO: Create a Link class and redefine toString
                    book.links.add("<a href=\"" + parser.getAttributeValue(null, "url") + "\">" + parser.getAttributeValue(null, "name") + "</a>");
                }
                // TODO: Load bestseller
            }
            // If encounter end tag of Book -> finish parsing
            else if ((parser.getEventType() == XmlPullParser.END_TAG && "Book".equals(parser.getName())))
                break;
        }

        return book;
    }

    /**
     * Get the book index in the serie of books
     * @return The index
     */
    public int getIndexInSerie()
    {
        int indexInSerie = 0;
        BookSerie serie = parent.parent;
        for (; indexInSerie < serie.getBookCount(); indexInSerie++)
            if(serie.getBookAt(indexInSerie) == this)
                break;

        return indexInSerie;
    }

    /**
     * Check if contains this link
     * @param link The link to search
     * @return True if found, false otherwise
     */
    public boolean containLink(String link)
    {
        for (String htmlLink : links)
        {
            if(htmlLink.contains(link))
                return true;
        }
        return false;
    }
}
