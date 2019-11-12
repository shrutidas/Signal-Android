package org.thoughtcrime.securesms.education;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationChannels;
import org.thoughtcrime.securesms.notifications.NotificationState;
import org.thoughtcrime.securesms.util.ServiceUtil;

public class AppUseNotifierService extends Service {

    public  static final  int FOREGROUND_ID            = 3133999;


    public AppUseNotifierService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Context context = getApplicationContext();
        NotificationState notificationState = new NotificationState();
        long[] vibratePattern = {100,100};

        final int    NOTIFICATION_ID = 13399;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.OTHER);


        builder.setSmallIcon(R.drawable.icon_notification)
                .setColor(context.getResources().getColor(R.color.signal_primary))
                .setContentTitle(context.getString(R.string.study_reminder_title))
                .setContentText(context.getString(R.string.study_reminder))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.study_reminder_action)))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent( context, ConversationListActivity.class), 0));

        Notification tempNotification = builder.build();
        Notification mainNotification = builder.setSound(notificationState.getRingtone(context))
                .setVibrate(vibratePattern)
                .build();

        ServiceUtil.getNotificationManager(context).notify(NOTIFICATION_ID, mainNotification);
        startForeground(FOREGROUND_ID, tempNotification);
        Log.d("notification service.", "is it there?");

        stopSelf();

        return Service.START_STICKY;
    }
}
