package com.example.notebook;

import android.os.Bundle;

import androidx.lifecycle.ViewModel;

public class NoteActivityViewModel extends ViewModel {
    public static  final String ORIGINAL_COURSE_ID = "com.example.notebook.ORIGINAL_COURSE_ID";
    public static final String ORIGINAL_COURSE_TITLE = "com.example.notebook.ORIGINAL_COURSE_TITLE";
    public static final String ORIGINAL_COURSE_TEXT = "com.example.notebook.ORIGINAL_COURSE_TEXT";

    public String mOriginalCourseId;
    public String mOriginalNoteTitle;
    public String mOriginalNoteText;
    public boolean mIsNewlyCreated = true;

    public void saveState(Bundle outState) {
        outState.putString(ORIGINAL_COURSE_ID, mOriginalCourseId);
        outState.putString(ORIGINAL_COURSE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_COURSE_TEXT, mOriginalNoteText);
    }

    public void restoreState(Bundle inState) {
        mOriginalCourseId = inState.getString(ORIGINAL_COURSE_ID);
        mOriginalNoteTitle = inState.getString(ORIGINAL_COURSE_TITLE);
        mOriginalNoteText = inState.getString(ORIGINAL_COURSE_TEXT);
    }
}
