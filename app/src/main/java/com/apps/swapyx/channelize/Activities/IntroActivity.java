package com.apps.swapyx.channelize.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.apps.swapyx.channelize.R;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

/**
 * Created by SwapyX on 03-08-2017.
 */

public class IntroActivity extends AppIntro2 {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SliderPage sliderPage1 = new SliderPage();
        sliderPage1.setTitle("Welcome!");
        sliderPage1.setDescription("Channelize helps you channel your focus on the work and boost your productivity.");
        sliderPage1.setImageDrawable(R.drawable.channelize);
        sliderPage1.setBgColor(Color.parseColor("#388e3c"));
        addSlide(AppIntroFragment.newInstance(sliderPage1));

        SliderPage sliderPage2 = new SliderPage();
        sliderPage2.setTitle("Plan your work");
        sliderPage2.setDescription("Use the app's list to add your tasks and organize them. " +
                "Tap on a task to start working on it.");
        sliderPage2.setImageDrawable(R.drawable.intro1);
        sliderPage2.setBgColor(Color.parseColor("#388e3c"));
        addSlide(AppIntroFragment.newInstance(sliderPage2));

        SliderPage sliderPage3 = new SliderPage();
        sliderPage3.setTitle("Put full attention");
        sliderPage3.setDescription("Break your tasks into focused work sessions, followed by regular breaks. " +
                "You can change the timer durations in the settings.");
        sliderPage3.setImageDrawable(R.drawable.intro2);
        sliderPage3.setBgColor(Color.parseColor("#388e3c"));
        addSlide(AppIntroFragment.newInstance(sliderPage3));

        showStatusBar(false);
        showSkipButton(false);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }
}
