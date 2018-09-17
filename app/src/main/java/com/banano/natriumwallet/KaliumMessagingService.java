package com.banano.natriumwallet;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.banano.natriumwallet.model.Credentials;
import com.banano.natriumwallet.util.SharedPreferencesUtil;
import com.google.android.gms.common.internal.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;

public class KaliumMessagingService extends FirebaseMessagingService {
    private static final String TAG = KaliumMessagingService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_ID = "natrium_notification_channel";

    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;

    private static boolean isForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(Constants.KEY_APP_PACKAGE_NAME);
    }

    public void onCreate() {
        KaliumApplication.getApplication(this).getApplicationComponent().inject(this);
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification().getBody() != null && !isForeground(this)) {
            sendNotification(remoteMessage);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        sharedPreferencesUtil.setFcmToken(token);
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Natrium transaction alerts");
        notificationManager.createNotificationChannel(channel);
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        initChannels(this);

        try (Realm realm = Realm.getDefaultInstance()) {
            Credentials c = realm.where(Credentials.class).findFirst();
        }

        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);


        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_currency_banano_yellow);
        builder.setContentText(notification.getBody());
        builder.setContentTitle(notification.getTitle());
        builder.setAutoCancel(true);
        builder.setSound(defaultSoundUri);

        Notification pushNotification = builder.build();

        // Maybe use a notifyID so it can be updated
        nm.notify(1, pushNotification);
    }
}