package co.banano.natriumwallet;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import co.banano.natriumwallet.broadcastreceiver.CancelNotificationReceiver;
import co.banano.natriumwallet.model.AvailableLanguage;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.NotificationOption;
import co.banano.natriumwallet.util.NumberUtil;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import io.realm.Realm;

public class KaliumMessagingService extends FirebaseMessagingService {
    private static final String TAG = KaliumMessagingService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_ID = "natrium_notification_channel";

    private int NOTIFICATION_ID = 1337;


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferencesUtil sharedPreferencesUtil = new SharedPreferencesUtil(this);
        if (sharedPreferencesUtil.getLanguage() != AvailableLanguage.DEFAULT) {
            Locale locale = new Locale(sharedPreferencesUtil.getLanguage().getLocaleString());
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
        if (remoteMessage.getData() != null && sharedPreferencesUtil.isBackgrounded() && sharedPreferencesUtil.getNotificationSetting() != NotificationOption.OFF) {
            if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.M) {
                sendNotification(remoteMessage);
            } else {
                sendNotificationLegacy(remoteMessage);
            }
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

    private void sendNotificationLegacy(RemoteMessage remoteMessage) {
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

    @TargetApi(Build.VERSION_CODES.M)
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

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Intent onCancelNotificationReceiver = new Intent(this, CancelNotificationReceiver.class);
        PendingIntent onCancelNotificationReceiverPendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0,
                onCancelNotificationReceiver, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String notificationTitle = getString(R.string.notification_title, NumberUtil.getRawAsUsableString(amount));
        for (StatusBarNotification sNotification : manager.getActiveNotifications()) {
            if (sNotification.getPackageName().equals(getApplicationContext().getPackageName())) {
                Intent startNotificationActivity = new Intent(this, MainActivity.class);
                startNotificationActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, startNotificationActivity,
                        PendingIntent.FLAG_ONE_SHOT);
                Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_status_bar)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(notificationTitle)
                        .setContentText(getString(R.string.notification_body))
                        .setAutoCancel(true)
                        .setStyle(getStyleForNotification(notificationTitle))
                        .setGroupSummary(true)
                        .setGroup(TAG)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(onCancelNotificationReceiverPendingIntent)
                        .build();
                SharedPreferences sharedPreferences = getSharedPreferences("NotificationData", 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(String.valueOf(new Random(NOTIFICATION_ID)), notificationTitle);
                editor.apply();
                notificationManager.notify(NOTIFICATION_ID, notification);
                return;
            }
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Notification notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_bar)
                .setContentTitle(notificationTitle)
                .setContentText(getString(R.string.notification_body))
                .setAutoCancel(true)
                .setGroup(TAG)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(onCancelNotificationReceiverPendingIntent)
                .build();
        SharedPreferences sharedPreferences = getSharedPreferences("NotificationData", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(String.valueOf(new Random(NOTIFICATION_ID)), notificationTitle);
        editor.apply();
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder);
    }

    private NotificationCompat.InboxStyle getStyleForNotification(String messageBody) {
        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        SharedPreferences sharedPref = getSharedPreferences("NotificationData", 0);
        Map<String, String> notificationMessages = (Map<String, String>) sharedPref.getAll();
        Map<String, String> myNewHashMap = new HashMap<>();
        for (Map.Entry<String, String> entry : notificationMessages.entrySet()) {
            myNewHashMap.put(entry.getKey(), entry.getValue());
        }
        inbox.addLine(messageBody);
        for (Map.Entry<String, String> message : myNewHashMap.entrySet()) {
            inbox.addLine(message.getValue());
        }
        inbox.setBigContentTitle(this.getResources().getString(R.string.app_name))
                .setSummaryText(getString(R.string.notificaiton_header_suplement));
        return inbox;
    }
}