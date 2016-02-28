// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment showing information about Brandon Sanderson
 */
public class AboutBrandonFragment extends Fragment
{
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_about, container, false);

        TextView content = (TextView)rootView.findViewById(R.id.about_content);
        content.setText(Html.fromHtml(
                                "<p>Brandon Sanderson was born in December 1975 in Lincoln, Nebraska. As a child Brandon enjoyed reading, but he lost interest in the types of titles often suggested for him, and by junior high he never cracked a book if he could help it. This all changed in eighth grade when an astute teacher, Mrs. Reader, gave Brandon <i>Dragonsbane</i> by Barbara Hambly. Brandon thoroughly enjoyed this book, and went in search of anything similar. He discovered such authors as Robert Jordan, Melanie Rawn, David Eddings, Anne McCaffrey, and Orson Scott Card. Brandon continued to be an avid reader through junior high and high school. He liked epic fantasy so much that he even tried his hand at writing some. His first attempts, he says, were dreadful.</p>" +
                                "<p>In 1994 Brandon enrolled at Brigham Young University as a biochemistry major. From 1995 to 1997 he took time away from his studies to serve as a missionary for The Church of Jesus Christ of Latter-day Saints. Brandon often says that it was during this time in Seoul, Korea that he realized that he didn't miss chemistry one bit, but he did miss writing. Upon his return to BYU, Brandon became an English major, much to the dismay of his mother, who had always hoped he would become a doctor.</p>" +
                                "<p>Brandon began writing in earnest, taking a job as the night desk clerk at a hotel because they allowed him to write while at work. During this era he went to school full time during the day, worked nights to pay for his schooling, and wrote as much as he could. He says it made for a rather dismal social life, but he finished seven novels during his undergraduate years. Brandon submitted many manuscripts for publication . . . and accumulated quite a pile of rejection letters. In spite of this he continued to be a dedicated writer.</p>" +
                                "<p>Volunteering for <i>The Leading Edge</i>, BYU's science fiction/fantasy magazine, was a wonderful experience for Brandon. He read many submissions, formed some lifelong friendships, and served as Editor in Chief during his senior year.</p>" +
                                "<p>Brandon learned much about the business side of being a writer by taking a class from David Farland, author of the popular Runelords series. One piece of advice Dave gave Brandon was to attend conventions, such as WorldCon and World Fantasy, in order to connect with industry professionals. Brandon and a small group of friends who were also aspiring writers began to do so. He eventually met both his current agent and one of his editors at conventions.</p>" +
                                "<p>It was in 2003, while Brandon was in the middle of a graduate program at BYU, that he got a call from editor Moshe Feder at Tor, who wanted to buy one of Brandon's books. Brandon had submitted the manuscript a year and a half earlier, and had almost given up on hearing anything, so he was surprised and delighted to receive the offer. In May 2005 Brandon held his first published novel,<i>Elantris</i>, in his hands. Over the next few years, Tor also published Brandon's Mistborn trilogy, its followup <i>The Alloy of Law</i>, <i>Warbreaker</i>, and <i>The Way of Kings</i>, the first in a projected ten-volume series called The Stormlight Archive. The second book in the series, <em>Words of Radiance</em>, was released on March 4th, 2014. Other projects continue to be in the works.</p>" +
                                "<p>In 2004 after graduating with his Master's degree in creative writing from Brigham Young University, Brandon was asked to teach the class he had taken as an undergraduate student from Dave Farland. In spite of his busy schedule, Brandon continues to teach this one section of creative writing focused on science fiction and fantasy because he enjoys helping aspiring writers. It also gets him out of the house, he says. Additionally, along with Howard Tayler, Mary Robinette Kowal, and Dan Wells, he hosts the doubly Hugo-nominated writing advice podcast Writing Excuses, which has twice won a Parsec Award.</p>" +
                                "<p>In July 2006 Brandon married Emily Bushman. Emily and Brandon ran in many of the same circles at BYU during their student days, since Emily majored in English as well. They never met, however, until a mutual friend set them up on a date in 2005. Emily had spent seven years as a teacher, but chose to quit with the birth of their first child in October 2007. Emily now works from home part time as Brandon's business manager.</p>" +
                                "<p>Brandon's repertoire expanded into the books's market when Scholastic published <i>Alcatraz Versus the Evil Librarians</i>, a middle-grade novel, in October 2007. Nancy Pearl gave this book a very favorable review on National Public Radio, which pleased Sanderson fans. Three more volumes of the series have been released so far. Additionally, Brandon's novella <i>Infinity Blade: Awakening</i> was an ebook bestseller for Epic Games accompanying their acclaimed <i>Infinity Blade</i> iOS video game series.</p>" +
                                "<p>In December 2007 Brandon was chosen by Harriet McDougal Rigney to complete Robert Jordan's Wheel of Time series after his untimely passing. 2009's <i>The Gathering Storm</i> and 2010's <i>Towers of Midnight</i> was followed by the final volume in the series, <i>A Memory of Light</i>, in January 2013.</p>" +
                                "<p>The only author to make the short list for the David Gemmell Legend Award six times in four years, Brandon won that award in 2011 for <i>The Way of Kings</i> and is on the short list again in 2012 for <i>The Alloy of Law</i>. He has also won the <i>Romantic Times</i> Reviewers' Choice award for Best Epic Fantasy twice and has been nominated three other years. He was twice nominated for the John W. Campbell Award for Best New Writer. He has hit the <i>New York Times</i> Hardcover Fiction Best-Seller List six times, with his first Wheel of Time book knocking Dan Brown out of the #1 spot and his second dethroning John Grisham. <i>Alcatraz Versus the Evil Librarians</i> was optioned for film by DreamWorks Animation, <i>Mistborn</i> was optioned by Paloppa Pictures, and a Mistborn video game will be released by Little Orbit in 2013 for all platforms. Brandon's books have been published in over twenty languages.</p>")
        );

        return rootView;
    }
}
