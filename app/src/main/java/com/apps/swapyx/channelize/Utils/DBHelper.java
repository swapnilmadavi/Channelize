package com.apps.swapyx.channelize.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.apps.swapyx.channelize.ToDoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SwapyX on 08-07-2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "toDoList.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_TASKS = "toDoTasks";

    public static final String DML_TYPE = "DML_TYPE";
    public static final String DML_INSERT = "INSERT";
    public static final String DML_UPDATE = "UPDATE";

    // Column Names
    public static final String COLUMN_ID = "toDoId";
    public static final String COLUMN_NAME = "toDoName";
    public static final String COLUMN_DUE_DATE = "toDoDueDate";
    public static final String COLUMN_NOTE = "toDoNote";
    public static final String COLUMN_STATUS = "isComplete";
    public static final String COLUMN_SECONDS_WORKED = "secondsWorked";

    public static final byte INCOMPLETE = 0;
    public static final byte COMPLETED = 1;


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_NAME + " TEXT, "
                + COLUMN_DUE_DATE + " TEXT, " + COLUMN_NOTE + " TEXT, "
                + COLUMN_STATUS + " INTEGER DEFAULT 0, "
                + COLUMN_SECONDS_WORKED + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    /*function to return a list of all the tasks from the database according to their status */
    public List<ToDoItem> getAllTasks(int taskType) {
        List<ToDoItem> tasks = new ArrayList<>();
        ToDoItem item;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String query = "SELECT * from " + TABLE_TASKS + " where " + COLUMN_STATUS + " = ?";;
        if (taskType == INCOMPLETE){
            cursor = db.rawQuery(query, new String[]{String.valueOf(0)});
        }else if(taskType == COMPLETED){
            cursor = db.rawQuery(query, new String[]{String.valueOf(1)});
        }
        if(cursor != null && cursor.getCount() > 0){
            while (cursor.moveToNext()) {
                item = new ToDoItem();
                item.setToDoId(cursor.getInt(0));
                item.setToDoName(cursor.getString(1));
                item.setToDoDueDate(cursor.getString(2));
                item.setToDoNote(cursor.getString(3));
                if(cursor.getInt(4) == 0){
                    item.setToDoStatus(false);
                }else{
                    item.setToDoStatus(true);
                }
                item.setSecondsWorked(cursor.getInt(5));
                tasks.add(0,item);
            }
        }
        if(cursor != null){
            cursor.close();
        }
        return tasks;
    }

    /*function to add a task to the database and return the new rowID*/
    public long addTask(ToDoItem newItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME,newItem.getToDoName());
        contentValues.put(COLUMN_DUE_DATE,newItem.getToDoDueDate());
        contentValues.put(COLUMN_NOTE,newItem.getToDoNote());
        return db.insert(TABLE_TASKS, null, contentValues);
    }

    /*function to update a task in the database with given id*/
    public void updateTask(ContentValues contentValues, long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.update(TABLE_TASKS,contentValues,COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /*function to update COLUMN_SECONDS_WORKED of a task in the database
      with given id and seconds worked*/
    public void updateTaskTime(long id, int secondsWorked) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_TASKS + " SET " + COLUMN_SECONDS_WORKED + " = ("
                + COLUMN_SECONDS_WORKED + " + " + secondsWorked + ") WHERE " + COLUMN_ID + " = " + id);
    }

    /*function to delete a task from the database with given id*/
    public void deleteTask(long id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

}
