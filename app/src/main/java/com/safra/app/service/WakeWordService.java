package com.safra.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.safra.app.R;
import com.safra.app.util.SosUtil;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class WakeWordService extends Service {

    private static final String TAG = "WakeWordService";
    private static final String CHANNEL_ID = "WakeWordServiceChannel";
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final int ALERT_NOTIFICATION_ID = 3;

    private PorcupineManager porcupineManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification initializingNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safra Wake Word")
                .setContentText("Initializing listener...")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        startForeground(SERVICE_NOTIFICATION_ID, initializingNotification);

        initializePorcupine();
    }

    private void initializePorcupine() {
        try {
            porcupineManager = new PorcupineManager.Builder()
                    // 👇 PASTE YOUR NEW ACCESSKEY FROM THE PICOVOICE CONSOLE HERE 👇
                    .setAccessKey("3H2TUVzmAwfmMFn+BKCU/MWrbwQGzLwiX6HDTyFLAeBpfId8n1xBYg==")
                    // 👇 Make sure this filename matches the one you downloaded and put in assets 👇
                    .setKeywordPath("safra-help_en_android_v3_0_0.ppn")
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(), new PorcupineManagerCallback() {
                        @Override
                        public void invoke(int keywordIndex) {
                            // ✅ Trigger detected!
                            Log.i(TAG, "Wake word 'safra help' detected!");
                            SosUtil.activateInstantSosMode(WakeWordService.this);
                            showConfirmationNotification();
                        }
                    });

            porcupineManager.start();
            Log.i(TAG, "Porcupine initialized and started successfully. Listening...");

            // Update notification to "Listening" state on success
            updateListeningNotification();

        } catch (PorcupineException e) {
            // Initialization failed, inform the user and stop.
            Log.e(TAG, "Failed to initialize Porcupine.", e);
            showErrorNotification("Wake Word Error: " + e.getMessage());
            stopSelf();
        }
    }

    // ... (All other methods like showConfirmationNotification, updateListeningNotification, etc., remain the same)

    private void showConfirmationNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Alert Sent")
                .setContentText("Don't worry. Your trusted contacts have been notified.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }

    private void updateListeningNotification() {
        Notification listeningNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safra is Active")
                .setContentText("Listening for the wake word.")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(SERVICE_NOTIFICATION_ID, listeningNotification);
    }

    private void showErrorNotification(String message) {
        Notification errorNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safra Wake Word Error")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(SERVICE_NOTIFICATION_ID, errorNotification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safra Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setDescription("Channel for Safra wake word and alert notifications.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}