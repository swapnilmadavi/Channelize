package com.apps.swapyx.channelize.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.swapyx.channelize.Events.FocusTaskChangedEvent;
import com.apps.swapyx.channelize.Events.StartForegroundEvent;
import com.apps.swapyx.channelize.Fragments.TimerFragment;
import com.apps.swapyx.channelize.Utils.BusProvider;
import com.apps.swapyx.channelize.Adapters.SwipeAdapter;
import com.apps.swapyx.channelize.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.channelize.Events.CurrentTaskEditedEvent;
import com.apps.swapyx.channelize.R;
import com.apps.swapyx.channelize.TimerMode;
import com.apps.swapyx.channelize.TimerService;
import com.apps.swapyx.channelize.TimerStatus;
import com.apps.swapyx.channelize.ToDoItem;
import com.apps.swapyx.channelize.Utils.TimerProperties;
import com.squareup.otto.Subscribe;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbarMain;
    private TextView mTextNumSessions;
    private ViewPager mViewPager;
    private SwipeAdapter mSwipeAdapter;

    private boolean doubleBackToExitPressedOnce = false;

    private String mCurrentTodoName;
    private int mNumberOfSessions;
    private static FreeTaskListener freeTaskListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mToolbarMain = (Toolbar) findViewById(R.id.toolbar_main);
        mTextNumSessions = (TextView) findViewById(R.id.text_session);

        PreferenceManager.setDefaultValues(this, R.xml.app_prefs, false);

        new FocusTaskChangedEvent(new ToDoItem(99999,"Free task"));

        mCurrentTodoName = FocusTaskChangedEvent.currentFocusTask.getToDoName();

        if(mToolbarMain != null){
            setSupportActionBar(mToolbarMain);
        }

        //start the timer service
        Intent timerService = new Intent(this,TimerService.class);
        startService(timerService);

        mSwipeAdapter = new SwipeAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSwipeAdapter);

        setPageChangeListener();

        //on change in the Timer mode, update the color of the toolbar
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

        //on change in the number of sessions of the timer, update the textView and mNumberOfSessions
        TimerProperties.getInstance().setNumberOfSessionsChangeListener(new TimerProperties.NumberOfSessionsChangeListener() {
            @Override
            public void onChange(int numberOfSessions) {
                mNumberOfSessions = numberOfSessions;
                mTextNumSessions.setText(String.valueOf(mNumberOfSessions));
            }
        });

        //initialize the number of sessions to 0
        TimerProperties.getInstance().initNumberOfSessions();

        showIntro();

        mTextNumSessions.setText("");
    }

    /*Displays Intro on first run*/
    private void showIntro()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean firstRun = prefs.getBoolean("pref_first_run", true);

        if(firstRun)
        {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("pref_first_run", false);
            editor.apply();

            Intent intent = new Intent(this, IntroActivity.class);
            this.startActivity(intent);
        }
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
        //send the service to foreground when a session is active
        if (TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED) {
            BusProvider.getInstance().post(new StartForegroundEvent(true));
        }
        BusProvider.getInstance().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Stop the timer service on closing the app
        Intent timerService = new Intent(this,TimerService.class);
        stopService(timerService);
    }

    @Override
    public void onBackPressed() {
        if (TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED) {
            //move app to background if timer is active
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

            //change the title of toolbar depending on the page selected
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
        new FocusTaskChangedEvent(new ToDoItem(99999,"Free task"));
        TimerProperties.getInstance().initNumberOfSessions();
        mCurrentTodoName = "Free task";
    }

    @Subscribe
    public void onFocusTaskChange(FocusTaskChangedEvent event){
        mCurrentTodoName = FocusTaskChangedEvent.currentFocusTask.getToDoName();
        if(FocusTaskChangedEvent.currentFocusTask.getToDoId() == 99999){
            //if free task is selected, set the title and reset the current task position to NOT_DEFINED
            getSupportActionBar().setTitle(mCurrentTodoName);
            freeTaskListener.resetCurrentTaskPosition();
        }else{
            mViewPager.setCurrentItem(1);
        }
        TimerProperties.getInstance().initNumberOfSessions();
    }

    public static void setFreeTaskSetListener(FreeTaskListener listener){
        freeTaskListener = listener;
    }

    public interface FreeTaskListener{
        void resetCurrentTaskPosition();
    }
}
