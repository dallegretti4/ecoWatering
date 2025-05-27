package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.DeviceRequestRefreshingRunnable;
import it.uniba.dib.sms2324.ecowateringhub.data.DataObjectRefreshingRunnable;

public class EcoWateringForegroundService extends Service {
    private static final int NOTIFICATION_ID = 3;
    private static final String WAKE_LOCK_TAG = "ecoWateringHub:wakeLockEcoWateringService";
    private static final String CHANNEL_ID = "EcoWateringServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "ecoWatering Service";
    private static final String SIMPLE_DATE_FORMAT = "HH:mm";
    private static final long DEVICE_REQUESTS_REFRESHING_FREQUENCY = 3 * 1000; // MILLISECONDS
    private static final long DATA_OBJECT_REFRESHING_FREQUENCY = 60 * 1000;
    private static final long HUB_REFRESHING_FREQUENCY = 45 * 1000;
    private EcoWateringHub hub;
    private PowerManager.WakeLock wakeLock;
    private Thread deviceRequestRefreshingThread;
    private Thread dataObjectRefreshingThread;
    private Thread hubRefreshingThread;

    public EcoWateringForegroundService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification(this, this.hub));
        Log.i(Common.THIS_LOG, "EcoWateringForegroundService -> onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = intent.getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
        this.hub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
        if(this.hub != null) {
            // DEVICE REQUESTS REFRESHING NEED TO BE STARTED
            this.deviceRequestRefreshingThread = new Thread(() -> {
                while ((this.hub.getRemoteDeviceList() != null) && (!this.hub.getRemoteDeviceList().isEmpty()) && (this.deviceRequestRefreshingThread != null) && (!this.deviceRequestRefreshingThread.isInterrupted())) {
                    Log.i(Common.THIS_LOG, "-------------------------> deviceRequestRefreshingServiceRunning iteration");
                    new Thread(new DeviceRequestRefreshingRunnable(this, this.hub)).start();
                    // WAIT FOR NEXT REFRESHING
                    try { Thread.sleep(DEVICE_REQUESTS_REFRESHING_FREQUENCY); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            this.deviceRequestRefreshingThread.start();

            // DATA OBJECT REFRESHING NEED TO BE STARTED
            this.dataObjectRefreshingThread = new Thread(() -> {
                while ((this.hub.isDataObjectRefreshing()) && (this.dataObjectRefreshingThread != null) && (!this.dataObjectRefreshingThread.isInterrupted())) {
                    Log.i(Common.THIS_LOG, "-------------------------> dataObjectRefreshingServiceRunning iteration");
                    new Thread(new DataObjectRefreshingRunnable(this, this.hub, DataObjectRefreshingRunnable.DATA_OBJECT_REFRESHING_RUNNABLE_DURATION)).start();
                    // TO BE SURE ITERATION START AFTER CORRECT TIMING
                    try { Thread.sleep(DATA_OBJECT_REFRESHING_FREQUENCY); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            this.dataObjectRefreshingThread.start();

            // REFRESH ECO WATERING HUB
            this.hubRefreshingThread = new Thread(() -> {
                while((this.deviceRequestRefreshingThread.isAlive() || this.dataObjectRefreshingThread.isAlive()) && (this.hubRefreshingThread != null) && (!this.hubRefreshingThread.isInterrupted())) {
                    Log.i(Common.THIS_LOG, "-------------------------> hubRefreshingServiceRunning iteration");
                    // WAIT TO BE SURE ITERATIONS ARE FINISHED
                    try { Thread.sleep(HUB_REFRESHING_FREQUENCY); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // REFRESH HUB AND UPDATE NOTIFICATION
                    String jsonResponse = EcoWateringHub.getEcoWateringHubJsonStringNoThread(Common.getThisDeviceID(this));
                    this.hub = new EcoWateringHub(jsonResponse);
                    MainActivity.setThisEcoWateringHub(this.hub);
                    updateNotification(this, this.hub);
                }
            });
            this.hubRefreshingThread.start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if ((this.wakeLock != null) && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        if(this.deviceRequestRefreshingThread != null) this.deviceRequestRefreshingThread.interrupt();
        if(this.dataObjectRefreshingThread != null) this.dataObjectRefreshingThread.interrupt();
        if(this.hubRefreshingThread != null) this.hubRefreshingThread.interrupt();
        stopForeground(true);
        stopSelf();
        Log.i(Common.THIS_LOG, "ecoWateringForegroundService -> onDestroy");
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        this.wakeLock.acquire();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private static Notification getNotification(@NonNull Context context, EcoWateringHub hub) {
        String text;
        if(hub != null) {
            text = context.getString(R.string.sensors_notification_text) + new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.getDefault()).format(new Date()) + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ambient_temperature_label) + ": " + hub.getAmbientTemperature() + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.relative_humidity_label) + ": " + hub.getRelativeHumidity() + "%\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.light_uv_index_label) + ": " + hub.getIndexUV();
        }
        else {
            text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.notification_data_object_refreshing_disabled);
        }

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sensors_notification_title))
                .setContentText(text)
                .setSmallIcon(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.small_simple_logo)
                .build();
    }

    private static void updateNotification(@NonNull Context context, @NonNull EcoWateringHub hub) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(context, hub));
    }

    public static void startEcoWateringForegroundService(@NonNull Context context, @NonNull EcoWateringHub hub) {
        stopEcoWateringForegroundService(context);  // TO BE SURE ECO WATERING FOREGROUND SERVICE IS UNIQUE
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
        Intent startIntent = new Intent(context, EcoWateringForegroundService.class);
        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void stopEcoWateringForegroundService(@NonNull Context context) {
        Intent stopIntent = new Intent(context, EcoWateringForegroundService.class);
        context.stopService(stopIntent);
    }
}
