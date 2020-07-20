package com.example.notebook;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;


import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.example.notebook.NoteBookDatabaseContract.CourseInfoEntry;
import com.example.notebook.NoteBookDatabaseContract.NoteInfoEntry;

import java.util.List;

public class NoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int LOADER_NOTES = 0;
    public final String LOG_TAG = getClass().getSimpleName();
    public static final String NOTE_ID = "com.example.notebook.NOTE_POSITION";
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote;
    private boolean mIsnewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private NoteActivityViewModel mViewModel;
    private NoteBookSQLiteOpenHelper mDbOpenHelper;
    private Cursor mNoteCursor;
    private int mCourseIdPos;
    private int mNoteTitlePos;
    private int mNoteTextPos;
    private String mCourseId;
    private String mNoteTitle;
    private String mNoteText;
    private SimpleCursorAdapter mAdapterCourses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDbOpenHelper = new NoteBookSQLiteOpenHelper(this);

      mViewModel = new ViewModelProvider(getViewModelStore(),
               ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(NoteActivityViewModel.class);

        if (mViewModel.mIsNewlyCreated && savedInstanceState != null)
            mViewModel.restoreState(savedInstanceState);

        mViewModel.mIsNewlyCreated = false;

        mSpinnerCourses = findViewById(R.id.spinner_courses);

        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[] {CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[] {android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

        loadCourseData();

        mTextNoteTitle = findViewById(R.id.note_title);

        mTextNoteText = findViewById(R.id.note_text);

        readDisplayStateValue();

        if (!mIsnewNote)
            LoaderManager.getInstance(NoteActivity.this).initLoader(LOADER_NOTES, null, this);

        if (mViewModel.mOriginalCourseId == null)
            saveOriginalNoteValues();

    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID};

        Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
                null, null, null, null,
                CourseInfoEntry.COLUMN_COURSE_TITLE);
        mAdapterCourses.changeCursor(cursor);
    }

    private void loadNoteData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        String[] noteColumns = {NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT};

        mNoteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns, selection, selectionArgs,
                null, null, null);

        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToNext();
        displayNote();
    }

    private void saveOriginalNoteValues() {
        if (mIsnewNote)
            return;

        mViewModel.mOriginalCourseId = mCourseId;
        mViewModel.mOriginalNoteTitle = mNoteTitle;
        mViewModel.mOriginalNoteText = mNoteText;
    }


    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsCancelling) {
            if (mIsnewNote) {
                DataManager.getInstance().removeNote(mNoteId);
            } else {
                //storePreviousNoteValues();
            }
        } else {
            //saveNote();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.saveState(outState);

    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mViewModel.mOriginalCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mViewModel.mOriginalNoteTitle);
        mNote.setText(mViewModel.mOriginalNoteText);
    }

    private void saveNote() {
            mNote.setCourse((CourseInfo) mSpinnerCourses.getSelectedItem());
            mNote.setTitle(mTextNoteTitle.getText().toString());
            mNote.setText(mTextNoteText.getText().toString());
    }

    private void displayNote() {
        mCourseId = mNoteCursor.getString(mCourseIdPos);
        mNoteTitle = mNoteCursor.getString(mNoteTitlePos);
        mNoteText = mNoteCursor.getString(mNoteTextPos);

        int courseIndex = getIndexOfCourseId(mCourseId);
        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(mNoteTitle);
        mTextNoteText.setText(mNoteText);
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int coursesIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;

        boolean more = cursor.moveToFirst();
        while (more) {
            String cursorCourseId = cursor.getString(coursesIdPos);
            if (mCourseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            more = cursor.moveToNext();
        }

        return courseRowIndex;
    }

    private void readDisplayStateValue() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsnewNote = mNoteId == ID_NOT_SET;
        if (mIsnewNote) {
            createNewNote();
        }
//        mNote = DataManager.getInstance().getNotes().get(mNoteId);
    }

    private void createNewNote() {
        DataManager dm = DataManager.getInstance();
        mNoteId = dm.createNewNote();
//        mNote = dm.getNotes().get(mNotePosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendMail();
            return true;
        } else if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        } else if (id ==R.id.action_next_note) {
            moveNext();
        }

        return super.onOptionsItemSelected(item);
    }

    private void moveNext() {
        //Save the current Note
        saveNote();
        //Get the position of the new Note
        ++mNoteId;
        //Get the new Note using the position
        mNote= DataManager.getInstance().getNotes().get(mNoteId);
        //Save the Original values of the new Note
        saveOriginalNoteValues();
        //Display the New Note
        displayNote();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next_note);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void sendMail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Check out what l learned in the Pluralsight courses \"" +
                course.getTitle() + "\"\n" + mTextNoteText.getText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        CursorLoader loader = null;
        if (id == LOADER_NOTES)
            loader = createLoaderNotes();
        return loader;
    }

    private CursorLoader createLoaderNotes() {
        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

                String selection = NoteInfoEntry._ID + " = ?";
                String[] selectionArgs = {Integer.toString(mNoteId)};

                String[] noteColumns = {NoteInfoEntry.COLUMN_COURSE_ID,
                        NoteInfoEntry.COLUMN_NOTE_TITLE,
                        NoteInfoEntry.COLUMN_NOTE_TEXT};

                return db.query(NoteInfoEntry.TABLE_NAME, noteColumns, selection, selectionArgs,
                        null, null, null);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToNext();
        displayNote();
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
        if (loader.getId() == LOADER_NOTES) {
            if (mNoteCursor != null)
                mNoteCursor.close();
        }
    }
}
