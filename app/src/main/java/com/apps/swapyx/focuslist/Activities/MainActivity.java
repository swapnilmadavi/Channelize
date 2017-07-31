package com.apps.swapyx.focuslist.Activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.swapyx.focuslist.Adapters.SwipeAdapter;
import com.apps.swapyx.focuslist.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.focuslist.Events.CurrentTaskEditedEvent;
import com.apps.swapyx.focuslist.Events.FocusTaskChangedEvent;
import com.apps.swapyx.focuslist.Events.StartForegroundEvent;
import com.apps.swapyx.focuslist.Fragments.TimerFragment;
import com.apps.swapyx.focuslist.R;
import com.apps.swapyx.focuslist.TimerMode;
import com.apps.swapyx.focuslist.TimerService;
import com.apps.swapyx.focuslist.TimerStatus;
import com.apps.swapyx.focuslist.ToDoItem;
import com.apps.swapyx.focuslist.Utils.BusProvider;
import com.apps.swapyx.focuslist.Utils.TimerProperties;
import com.squareup.otto.Subscribe;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbarMain;
    private TextView mTextNumSessions;
    private ViewPager mViewPager;
    private SwipeAdapter mSwipeAdapter;

    private boolean doubleBackToExitPressedOnce = false;

    private String mCurrentTodoName;
    private int mNumberOfSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mToolbarMain = (Toolbar) findViewById(R.id.toolbar_main);
        mTextNumSessions = (TextView) findViewById(R.id.textView_session);

        mNumberOfSessions = 0;

        mCurrentTodoName = FocusTaskChangedEvent.currentFocusTask.getToDoName();

        if(mToolbarMain != null){
            setSupportActionBar(mToolbarMain);
        }

        Intent timerService = new Intent(this,TimerService.class);
        startService(timerService);

        mSwipeAdapter = new SwipeAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSwipeAdapter);
        setPageChangeListener();
        mViewPager.setCurrentItem(1);

        TimerProperties.getInstance().setNumberOfSessionsChangeListener(new TimerProperties.NumberOfSessionsChangeListener() {
            @Override
            public void onChange(int numberOfSessions) {
                mNumberOfSessions = numberOfSessions;
                mTextNumSessions.setText(String.valueOf(mNumberOfSessions));
            }
        });

        TimerFragment.setChangeToolbarColorListener(new TimerFragment.ChangeToolbarColor() {
            @Override
            public void onTimerModeChanged(TimerMode timerMode) {
                if(timerMode == TimerMode.WORK){
                    mToolbarMain.setBackgroundColor(ContextCompat.getColor(MainActivity.this,R.color.colorPrimary));
                }else if(timerMode == TimerMode.BREAK){
                    mToolbarMain.setBackgroundColor(ContextCompat.getColor(MainActivity.this,R.color.colorBreak));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
            BusProvider.getInstance().post(new StartForegroundEvent(false));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED) {
            BusProvider.getInstance().post(new StartForegroundEvent(true));
        }
        BusProvider.getInstance().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent timerService = new Intent(this,TimerService.class);
        stopService(timerService);
    }

    @Override
    public void onBackPressed() {
        if (TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED) {
            // move app to background
            moveTaskToBack(true);
        }
        else{
            //double press to exit
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.press_exit, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    private void setPageChangeListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case 0:
                        getSupportActionBar().setTitle(R.string.app_name);
                        mTextNumSessions.setText("");
                        break;
                    case 1:
                        getSupportActionBar().setTitle(mCurrentTodoName);
                        mTextNumSessions.setText(String.valueOf(mNumberOfSessions));
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Subscribe
    public void onCurrentTaskEdited(CurrentTaskEditedEvent event){
        mCurrentTodoName = CurrentTaskEditedEvent.editedItem.getToDoName();
        new FocusTaskChangedEvent(CurrentTaskEditedEvent.editedItem);
    }

    @Subscribe
    public void onCurrentFocusTaskChecked(CurrentTaskCheckedEvent event){
        new FocusTaskChangedEvent(new ToDoItem("Free task"));
        TimerProperties.getInstance().initNumberOfSessions();
        mNumberOfSessions = 0;
        mCurrentTodoName = "Free Task";
    }

    @Subscribe
    public void onFocusTaskChange(FocusTaskChangedEvent event){
        mCurrentTodoName = FocusTaskChangedEvent.currentFocusTask.getToDoName();
        mViewPager.setCurrentItem(1);
        TimerProperties.getInstance().initNumberOfSessions();
        mNumberOfSessions = 0;
    }
}
