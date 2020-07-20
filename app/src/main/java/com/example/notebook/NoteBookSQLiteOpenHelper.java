package com.example.notebook;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

class NoteBookSQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "NoteBook.db";
    public static final int DATABASE_VERSION = 1;
    public NoteBookSQLiteOpenHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(NoteBookDatabaseContract.CourseInfoEntry.SQL_CREATE_TABLE);
        db.execSQL(NoteBookDatabaseContract.NoteInfoEntry.SQL_CREATE_TABLE);

        DatabaseDataWorker worker = new DatabaseDataWorker(db);
        worker.insertCourses();
        worker.insertSampleNotes();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
