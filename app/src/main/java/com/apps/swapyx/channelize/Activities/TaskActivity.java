package com.apps.swapyx.channelize.Activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Toast;

import com.apps.swapyx.channelize.Utils.DBHelper;
import com.apps.swapyx.channelize.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.graphics.Color.RED;
import static android.graphics.Color.BLACK;

public class TaskActivity extends AppCompatActivity {
    //Constants
    public static final String TASK_NAME = "originalName";
    public static final String TASK_DUE_DATE = "originalDueDate";
    public static final String TASK_NOTE = "originalNote";
    public static final String CHANGES_MADE = "changesMade";

    TextInputEditText mEditTaskName;
    TextInputEditText mEditTaskDueDate;
    TextInputEditText mEditTaskNote;
    ImageView mImageViewClearDate;

    String originalName, originalDueDate, originalNote;
    String editedName, editedDueDate, editedNote;

    String request;

    Calendar calendar;
    String myFormat = "EEE, dd MMM yyyy";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.US);

    Date today;

    DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date datePicked = calendar.getTime();
            updateLabel(datePicked);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        Toolbar toolbarTask = (Toolbar) findViewById(R.id.toolbar_task);
        setSupportActionBar(toolbarTask);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditTaskName = (TextInputEditText)findViewById(R.id.edit_task_name);
        mEditTaskDueDate = (TextInputEditText)findViewById(R.id.edit_datePicker);
        mEditTaskNote = (TextInputEditText)findViewById(R.id.edit_note);
        mImageViewClearDate = (ImageView) findViewById(R.id.image_view_clear);

        originalName = originalDueDate = originalNote = "";
        editedName = editedDueDate = editedNote = "";

        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        today = calendar.getTime();

        mEditTaskDueDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(TaskActivity.this, dateSetListener, calendar
                        .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        mImageViewClearDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mEditTaskDueDate.getText().toString().equals(""))){
                    mEditTaskDueDate.setText("");
                    mImageViewClearDate.setVisibility(View.GONE);
                }
            }
        });

        checkRequestedTask();
    }

    private void checkRequestedTask() {
        request = getIntent().getStringExtra(DBHelper.DML_TYPE);
        if (request.equals(DBHelper.DML_UPDATE)){
            getSupportActionBar().setTitle(R.string.edit_task);
            originalName = getIntent().getStringExtra(TASK_NAME);
            originalDueDate = getIntent().getStringExtra(TASK_DUE_DATE);
            originalNote = getIntent().getStringExtra(TASK_NOTE);

            mEditTaskName.setText(originalName);
            if(originalDueDate.equals("")){
                mEditTaskDueDate.setText(R.string.none);
            }else{
                Date currDate = null;
                try {
                    currDate = simpleDateFormat.parse(originalDueDate);
                    updateLabel(currDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            mEditTaskNote.setText(originalNote);
        }
        else if(request.equals(DBHelper.DML_INSERT)){
            getSupportActionBar().setTitle(R.string.new_task);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        editedName = mEditTaskName.getText().toString();
        if(mEditTaskDueDate.getText().toString().equals("Today")){
            editedDueDate = simpleDateFormat.format(today);
        }else{
            editedDueDate = mEditTaskDueDate.getText().toString();
        }
        editedNote = mEditTaskNote.getText().toString();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            if(editedName.equals("")){
                Toast.makeText(getApplicationContext(), R.string.enter_task_first, Toast.LENGTH_SHORT).show();
            }else{
                if(request.equals(DBHelper.DML_UPDATE)){
                    if(changesMade()){
                        Intent editIntent = makeResultIntent();
                        editIntent.putExtra(CHANGES_MADE,true);
                        setResult(RESULT_OK,editIntent);
                    }else{
                        Intent editIntent = new Intent();
                        editIntent.putExtra(CHANGES_MADE,false);
                        setResult(RESULT_OK,editIntent);
                    }
                }else if(request.equals(DBHelper.DML_INSERT)){
                    setResult(RESULT_OK,makeResultIntent());
                }
                finish();
            }
        }else if (id == android.R.id.home){
            if(changesMade()){
                AlertDialog.Builder discardDialogOk = new AlertDialog.Builder(this);
                discardDialogOk.setTitle(R.string.discard_changes);
                discardDialogOk.setPositiveButton(R.string.dicard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NavUtils.navigateUpFromSameTask(TaskActivity.this);
                    }
                });
                discardDialogOk.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                discardDialogOk.show();
                return true;
            }else{
                // Respond to the action bar's Up/Home button
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);

    }

    private Intent makeResultIntent() {
        Intent intent = new Intent();
        intent.putExtra(TASK_NAME, editedName);
        intent.putExtra(TASK_DUE_DATE, editedDueDate);
        intent.putExtra(TASK_NOTE, editedNote);
        return intent;
    }

    private void updateLabel(Date date) {
        if(today.after(date)){
            mEditTaskDueDate.setTextColor(RED);
        }else {
            mEditTaskDueDate.setTextColor(BLACK);
        }
        if(date.equals(today)){
            mEditTaskDueDate.setText(R.string.today);
        }else{
            mEditTaskDueDate.setText(simpleDateFormat.format(date));
        }
        mImageViewClearDate.setVisibility(View.VISIBLE);
    }

    private boolean changesMade(){
        if(!originalName.equals(editedName)){
            return true;
        }else if (!originalDueDate.equals(editedDueDate)){
            return true;
        }else if (!originalNote.equals(editedNote)){
            return true;
        }
        return false;
    }
}
