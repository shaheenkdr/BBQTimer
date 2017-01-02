/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Jerry Morrison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.onefishtwo.bbqtimer;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.FailureHandler;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

/** Within-app Espresso UI tests. */
// TODO: Add a multi-app test that checks the app's notifications.
@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppUITest {
    private ViewInteraction playPauseButton; // play/pause, formerly known as start/stop
    private ViewInteraction resetButton; // reset to paused @ 0:00; pause/replay icon; hidden on old Androids
    private ViewInteraction stopButton; // goes to stopped @ 0:00
    private ViewInteraction timeView;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void setUp() throws Exception {
        playPauseButton = onView(withId(R.id.startStopButton));
        resetButton = onView(withId(R.id.resetButton));
        stopButton = onView(withId(R.id.stopButton));
        timeView = onView(withId(R.id.display));
    }

    @After
    public void tearDown() throws Exception {
        playPauseButton = null;
        resetButton = null;
        stopButton = null;
        timeView = null;
    }

    /** Tests all the nodes and arcs in the app's play/pause/reset/stop FSM. */
    @Test
    public void playPauseStopUITest() {
        // Click the Stop button if it's visible so the test can begin in a well-defined state.
        // TODO: A clickIfVisible() or ifVisible(click()) ViewAction would be cleaner.
        onView(withId(R.id.stopButton)).withFailureHandler(new FailureHandler() {
            @Override
            public void handle(Throwable error, Matcher<View> viewMatcher) {
            }
        }).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());

        checkStopped();

        resetButton.perform(click());
        checkPausedAt0();

        stopButton.perform(click());
        checkStopped();

        playPauseButton.perform(click());
        checkPlaying();

        // TODO: Delay 2 seconds.
        // TODO: Check that timeView's text is within a given time range.

        stopButton.perform(click());
        checkStopped();

        resetButton.perform(click());
        checkPausedAt0();

        playPauseButton.perform(click());
        checkPlaying();

        // TODO: Delay 2 seconds.
        // TODO: Check that timeView's text is within a given time range.

        playPauseButton.perform(click());
        checkPausedNotAt0();

        playPauseButton.perform(click());
        checkPlaying();

        // TODO: Delay 2 seconds.
        // TODO: Check that timeView's text is within a given time range.

        playPauseButton.perform(click());
        checkPausedNotAt0();
        // TODO: Check that timeView's text is within a given time range.

        resetButton.perform(click());
        checkPausedAt0();

        playPauseButton.perform(click());
        checkPlaying();

        // TODO: Delay 2 seconds.
        // TODO: Check that timeView's text is within a given time range.

        playPauseButton.perform(click());
        checkPausedNotAt0();

        stopButton.perform(click());
        checkStopped();
    }

    /** Checks that the UI is in the fully Stopped at 0:00 state. */
    private void checkStopped() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(isDisplayed()));
        stopButton.check(matches(not(isDisplayed())));
        timeView.check(matches(withText("00:00.0")));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_action_pause)));

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused @ 0:00 state, aka the Reset state. */
    private void checkPausedAt0() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isDisplayed())); // TODO: check its icon
        timeView.check(matches(withText("00:00.0")));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Checks that the UI is in the Playing state, aka Run. */
    private void checkPlaying() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_pause)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused state, not at 0:00. */
    private void checkPausedNotAt0() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(isDisplayed()));
        stopButton.check(matches(isDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_action_replay)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    public void minutePickerUITest() {
        // TODO: Finish writing this test. This rough code is from an Espresso test recording.
        ViewInteraction numberPicker = onView(withId(R.id.minutesPicker));
        numberPicker.perform(longClick());

        pressBack();

        ViewInteraction minutePickerEditText = onView(
                allOf(withClassName(is("android.widget.NumberPicker$CustomEditText")),
                        withParent(withId(R.id.minutesPicker)),
                        isDisplayed()));
        minutePickerEditText.perform(replaceText("0.5"), pressImeActionButton()); // closeSoftKeyboard()

        ViewInteraction enableRemindersCheckBox = onView(withId(R.id.enableReminders));
        enableRemindersCheckBox.perform(click(), click());

        ViewInteraction button = onView(
                allOf(withId(R.id.resetButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.display), withText("00:00.0"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.main_container),
                                        0),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("00:00.0")));

        ViewInteraction editText = onView(
                allOf(IsInstanceOf.<View>instanceOf(android.widget.EditText.class),
                        withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        editText.check(matches(withText("1")));

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    public static Matcher<View> withCompoundDrawable(final int resourceId) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has compound drawable resource " + resourceId);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                for (Drawable drawable : textView.getCompoundDrawables()) {
                    if (sameBitmap(textView.getContext(), drawable, resourceId)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static boolean sameBitmap(Context context, Drawable drawable, int resourceId) {
        Drawable otherDrawable = context.getResources().getDrawable(resourceId);

        if (drawable == null || otherDrawable == null) {
            return false;
        }

        drawable = drawable.getCurrent();
        otherDrawable = otherDrawable.getCurrent();

        if (drawable instanceof BitmapDrawable && otherDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap otherBitmap = ((BitmapDrawable) otherDrawable).getBitmap();
            return bitmap != null && bitmap.sameAs(otherBitmap);
        }
        return false;
    }

}
