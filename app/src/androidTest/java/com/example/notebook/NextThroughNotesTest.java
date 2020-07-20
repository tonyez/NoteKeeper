package com.example.notebook;


import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static org.junit.Assert.*;

public class NextThroughNotesTest {

    @Rule
    public ActivityTestRule<MainActivity> mMainActivityActivityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void NextThroughNotes() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_notes));

        onView(withId(R.id.list_items)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        List<NoteInfo> notes = DataManager.getInstance().getNotes();
        for (int index =0; index < notes.size(); index++) {
            NoteInfo note = notes.get(index);

            onView(withId(R.id.spinner_courses)).check(matches(withSpinnerText(note.getCourse().getTitle())));
            onView(withId(R.id.note_title)).check(matches(withText(note.getTitle())));
            onView(withId(R.id.note_text)).check(matches(withText(note.getText())));

            if (index < notes.size()-1)
            onView(allOf(withId(R.id.action_next_note), isEnabled())).perform(click());
        }
        onView(withId(R.id.action_next_note)).check(matches(not(isEnabled())));
        pressBack();
    }

}