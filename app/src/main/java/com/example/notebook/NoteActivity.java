package com.example.notebook;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.example.notebook.NoteBookDatabaseContract.CourseInfoEntry;
import com.example.notebook.NoteBookDatabaseContract.NoteInfoEntry;
import com.example.notebook.NoteBookProviderContract.Courses;
import com.example.notebook.NoteBookProviderContract.Notes;

public class NoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    public final String LOG_TAG = getClass().getSimpleName();
    public static final String NOTE_ID = "com.example.notebook.NOTE_POSITION";
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote;
    private boolean mIsNewNote;
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
    private boolean mIsCoursesQueryFinished;
    private boolean mIsNotesQueryFinished;
    private Uri mNoteUri;

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

        LoaderManager.getInstance(NoteActivity.this).initLoader(LOADER_COURSES, null, this);

        mTextNoteTitle = findViewById(R.id.note_title);
        mTextNoteText = findViewById(R.id.note_text);

        readDisplayStateValue();

        if (!mIsNewNote)
            LoaderManager.getInstance(NoteActivity.this).initLoader(LOADER_NOTES, null, this);

        if (mViewModel.mOriginalCourseId == null)
            saveOriginalNoteValues();

    }

    private void saveOriginalNoteValues() {
        if (mIsNewNote)
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
            if (mIsNewNote) {
                deleteFromDatabase();
            } else {
                //storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
    }

    private void deleteFromDatabase() {
        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();

        MyAsyncTask task = new MyAsyncTask(selection, selectionArgs, db);
        task.execute();
    }

    private static class MyAsyncTask extends AsyncTask<Void, Void, Integer> {
        private String selection;
        private String[] selectionArgs;
        private SQLiteDatabase mDb;

        MyAsyncTask(String selection, String[] selectionArgs, SQLiteDatabase db) {
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            this.mDb = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return mDb.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
        }

        @Override
        protected void onPostExecute(Integer integer) {
           super.onPostExecute(integer);
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
            String courseId = selectedCourseId();
            String noteTitle = mTextNoteTitle.getText().toString();
            String noteText = mTextNoteText.getText().toString();

            saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String selectedCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = mAdapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        return cursor.getString(courseIdPos);
    }

    public void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        ContentValues values = new ContentValues();
        values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
        values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
        values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
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
            if (courseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            more = cursor.moveToNext();
        }

        return courseRowIndex;
    }

    private void readDisplayStateValue() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;
        if (mIsNewNote) {
            createNewNote();
        }
//        mNote = DataManager.getInstance().getNotes().get(mNoteId);
    }

    private void createNewNote() {
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        mNoteUri = getContentResolver().insert(Notes.CONTENT_URI, values);
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
        else if (id == LOADER_COURSES)
            loader = createLoaderCourses();
        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mIsCoursesQueryFinished = false;
        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID};
        return new CursorLoader(this, uri, courseColumns, null, null,
                Courses.COLUMN_COURSE_TITLE);
    }

    private CursorLoader createLoaderNotes() {
        mIsNotesQueryFinished = false;
        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

                String selection = NoteInfoEntry._ID + " = ?";
                String[] selectionArgs = {Integer.toString(mNoteId)};

                String[] noteColumns = {NoteInfoEntry.COLUMN_COURSE_ID,
                        NoteInfoEntry.COLUMN_NOTE_TITLE,
                        NoteInfoEntry.COLUMN_NOTE_TEXT};

                return    db.query(NoteInfoEntry.TABLE_NAME, noteColumns, selection, selectionArgs,
                        null,null,null);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
        else if (loader.getId() == LOADER_COURSES)
            mAdapterCourses.changeCursor(data);
        mIsCoursesQueryFinished = true;
        whenQueriesFinished();
    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToNext();
        mIsNotesQueryFinished = true;
        whenQueriesFinished();
    }

    private void whenQueriesFinished() {
        if (mIsCoursesQueryFinished && mIsNotesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() == LOADER_NOTES) {
            if (mNoteCursor != null)
                mNoteCursor.close();
            else if (loader.getId() == LOADER_COURSES)
                mAdapterCourses.changeCursor(null);
        }
    }
}
