package com.apps.swapyx.focuslist.Activities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.swapyx.focuslist.Adapters.CompletedTasksAdapter;
import com.apps.swapyx.focuslist.R;
import com.apps.swapyx.focuslist.ToDoItem;
import com.apps.swapyx.focuslist.Utils.DBHelper;

import java.util.ArrayList;
import java.util.List;

import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_STATUS;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COMPLETED;

public class CompletedTasksActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    TextView mTextEmptyList;
    List<ToDoItem> mCompletedList;
    CompletedTasksAdapter mAdapter;
    DBHelper mDbHelper;
    private boolean mTasksUnticked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_tasks);
        Toolbar toolbarCompletedTasks = (Toolbar) findViewById(R.id.toolbar_task_completed);
        setSupportActionBar(toolbarCompletedTasks);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.completed_tasks);

        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerView_completed);
        mTextEmptyList = (TextView) findViewById(R.id.textView_noTask);

        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mDbHelper = new DBHelper(getApplicationContext());
        mCompletedList = new ArrayList<>();
        mAdapter = new CompletedTasksAdapter(mCompletedList,CompletedTasksActivity.this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
        setClickListeners();
        //Load all saved Tasks
        new LoadCompletedTasks().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if(mTasksUnticked){
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    Log.d("CompletedTaskActivity","Result set OK");
                }
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setClickListeners() {
        mAdapter.setCompletedItemMenuListener(new CompletedTasksAdapter.CompletedItemMenu() {
            @Override
            public void deleteTask(int position) {
                final int taskPosition = position;

                AlertDialog.Builder deleteDialogOk = new AlertDialog.Builder(CompletedTasksActivity.this);
                deleteDialogOk.setTitle("Delete Task?");
                deleteDialogOk.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDbHelper.deleteTask(mCompletedList.get(taskPosition).getToDoId());
                        mCompletedList.remove(taskPosition);
                        mAdapter.notifyItemRemoved(taskPosition);
                        Toast.makeText(getApplicationContext(), "Task deleted",
                                Toast.LENGTH_SHORT).show();
                        if(mCompletedList.isEmpty()){
                            setEmptyListText();
                        }
                    }
                });
                deleteDialogOk.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                deleteDialogOk.show();
            }

            @Override
            public void markAsIncomplete(int position) {
                mTasksUnticked = true;

                //Update the status of the task as completed
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_STATUS,0);
                mDbHelper.updateTask(contentValues,mCompletedList.get(position).getToDoId());

                //Remove the task from the ToDolist and add to CompletedList
                mCompletedList.remove(position);
                mAdapter.notifyItemRemoved(position);
                if(mCompletedList.isEmpty()){
                    setEmptyListText();
                }
            }
        });
    }

    private void setEmptyListText() {
        mTextEmptyList.setVisibility(View.VISIBLE);
    }

    private class LoadCompletedTasks extends AsyncTask<Object,Void,List<ToDoItem>> {

        @Override
        protected List<ToDoItem> doInBackground(Object... params) {
            return mDbHelper.getAllTasks(COMPLETED);
        }

        @Override
        protected void onPostExecute(List<ToDoItem> toDoItemList) {
            if(toDoItemList.isEmpty()){
                setEmptyListText();
            }else{
                mCompletedList.clear();
                mCompletedList.addAll(toDoItemList);
                mAdapter.notifyDataSetChanged();
            }
        }
    }


}
