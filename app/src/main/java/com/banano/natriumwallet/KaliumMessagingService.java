package com.banano.natriumwallet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.banano.natriumwallet.model.Credentials;
import com.banano.natriumwallet.model.NotificationOption;
import com.banano.natriumwallet.util.NumberUtil;
import com.banano.natriumwallet.util.SharedPreferencesUtil;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import io.realm.Realm;

public class KaliumMessagingService extends FirebaseMessagingService {
    private static final String TAG = KaliumMessagingService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_ID = "natrium_notification_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferencesUtil sharedPreferencesUtil = new SharedPreferencesUtil(this);
        if (remoteMessage.getData() != null && !MainActivity.appInForeground && sharedPreferencesUtil.getNotificationSetting() != NotificationOption.OFF) {
            sendNotification(remoteMessage);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        SharedPreferencesUtil sharedPreferencesUtil = new SharedPreferencesUtil(this);
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
        Map<String, String> data = remoteMessage.getData();
        String amount = data.get("amount");
        if (amount == null) {
            return;
        }

        initChannels(this);

        try (Realm realm = Realm.getDefaultInstance()) {
            Credentials c = realm.where(Credentials.class).findFirst();
            // If not logged in, shouldn't post notifications
            if (c == null) {
                return;
            }
        }

        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_status_bar);
        builder.setContentText(getString(R.string.notification_body));
        builder.setContentTitle(getString(R.string.notification_title, NumberUtil.getRawAsUsableString(amount)));
        builder.setAutoCancel(true);
        builder.setGroup(TAG);
        builder.setSound(defaultSoundUri);

        Notification pushNotification = builder.build();

        nm.notify((int)System.currentTimeMillis(), pushNotification);
    }
}