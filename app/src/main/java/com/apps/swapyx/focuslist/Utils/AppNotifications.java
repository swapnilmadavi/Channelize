package com.apps.swapyx.focuslist.Utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.apps.swapyx.focuslist.Activities.MainActivity;
import com.apps.swapyx.focuslist.R;
import com.apps.swapyx.focuslist.TimerMode;

import static android.media.AudioAttributes.USAGE_ALARM;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by SwapyX on 29-06-2017.
 */

public class AppNotifications {

    public static Notification createForegroundNotifications(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Active Session in progress");

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    public static Notification createFinishNotification(Context context, TimerMode timerMode, boolean sound, boolean vibrate) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Countdown finished")
                .setContentText(getNotificationText(timerMode))
                .setAutoCancel(true);
        if(sound){
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (SDK_INT >= LOLLIPOP) {
                mBuilder.setSound(notificationSound, USAGE_ALARM);
            } else {
                mBuilder.setSound(notificationSound, AudioManager.STREAM_ALARM);
            }
        }
        if(vibrate){
            mBuilder.setVibrate(new long[]{0,300,200,500});
        }

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    private static CharSequence getNotificationText(TimerMode timerMode) {
        if(timerMode == TimerMode.WORK){
            return "Take a break";
        }else{
            return "Let's get back to work";
        }
    }
}
