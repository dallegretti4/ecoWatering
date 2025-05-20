package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.runnable.DataObjectRefreshingRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.DeviceRequestRefreshingRunnable;

public class EcoWateringForegroundService extends Service {
    public static final String DATA_OBJECT_REFRESHING_SERVICE_TYPE = "DATA_OBJECT_REFRESHING";
    public static final String DEVICE_REQUEST_REFRESHING_SERVICE_TYPE = "DEVICE_REQUEST_REFRESHING";
    private static final String ECO_WATERING_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE = "ECO_WATERING_SERVICE_IS_RUNNING";
    private static final String ECO_WATERING_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE = "ECO_WATERING_SERVICE_IS_NOT_RUNNING";
    public static final String SUCCESS_RESPONSE_SET_IS_DATA_OBJECT_REFRESHING = "isDataObjectRefreshingSet";
    private static final String WAKE_LOCK_TAG = "ecoWateringHub:wakeLockEcoWateringService";
    private static final String CHANNEL_ID = "EcoWateringServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "ecoWatering Service";
    private static final String SIMPLE_DATE_FORMAT = "HH:mm";
    private static final int NOTIFICATION_ID = 3;
    public static boolean isDataObjectRefreshingServiceRunning;
    public static boolean isRequestRefreshingServiceRunning;
    public static Thread deviceRequestRefrshingThread;
    public static Thread dataObjectRefreshingThread;
    private EcoWateringHub hub;
    private PowerManager.WakeLock wakeLock;

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
            isRequestRefreshingServiceRunning = true;
            isDataObjectRefreshingServiceRunning = true;
            if ((hub.getRemoteDeviceList() != null) && (!hub.getRemoteDeviceList().isEmpty())) {
                deviceRequestRefrshingThread = new Thread(new DeviceRequestRefreshingRunnable(this, this.hub));
                deviceRequestRefrshingThread.start();
            }
            if ((hub.getEcoWateringHubConfiguration() != null) && (!hub.getEcoWateringHubConfiguration().isDataObjectRefreshing())) {
            dataObjectRefreshingThread = new Thread(new DataObjectRefreshingRunnable(this, this.hub));
            }
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
        if((wakeLock != null) && wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        isDataObjectRefreshingServiceRunning = false;
        isRequestRefreshingServiceRunning = false;
        if(deviceRequestRefrshingThread.isAlive()) {
            deviceRequestRefrshingThread.interrupt();
        }
        if(dataObjectRefreshingThread.isAlive()) {
            dataObjectRefreshingThread.interrupt();
        }
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
            text = context.getString(R.string.http_error_message) + new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.getDefault()).format(new Date());
        }

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sensors_notification_title))
                .setContentText(text)
                .setSmallIcon(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.small_simple_logo)
                .build();
    }

    public static void updateNotification(@NonNull Context context, @NonNull EcoWateringHub hub) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(context, hub));
    }

    public static boolean isEcoWateringServiceRunning(@NonNull Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        Log.i(Common.THIS_LOG, sharedPreferences.getString(EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE, EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE));
        return sharedPreferences.getString(EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE, EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE).equals(EcoWateringForegroundService.ECO_WATERING_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE);
    }

    public static void startEcoWateringService(@NonNull EcoWateringHub hub, @NonNull Context context, String serviceType) {
        Log.i(Common.THIS_LOG, "startEcoWateringService method");
        // TO BE SURE ECO WATERING SERVICE RUNNING IS UNIQUE
        Intent stopIntent = new Intent(context, EcoWateringForegroundService.class);
        context.stopService(stopIntent);

        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
        Intent startIntent = new Intent(context, EcoWateringForegroundService.class);
        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        // DATA OBJECT REFRESHING START CASE
        if(serviceType.equals(DATA_OBJECT_REFRESHING_SERVICE_TYPE)) {
            hub.getEcoWateringHubConfiguration().setIsDataObjectRefreshing(context, true, (response) -> {
                if(response.equals(SUCCESS_RESPONSE_SET_IS_DATA_OBJECT_REFRESHING)) {
                    ContextCompat.startForegroundService(context, startIntent);
                }
            });
        }
        // DEVICE REQUEST REFRESHING START CASE
        else if(serviceType.equals(DEVICE_REQUEST_REFRESHING_SERVICE_TYPE)) {
            ContextCompat.startForegroundService(context, startIntent);
        }
    }

    public static void stopEcoWateringService(@NonNull EcoWateringHub hub, @NonNull Context context, String serviceType) {
        Log.i(Common.THIS_LOG, "stopEcoWateringService method");
        Intent stopIntent = new Intent(context, EcoWateringForegroundService.class);
        // DATA OBJECT REFRESHING STOP CASE
        if(serviceType.equals(DATA_OBJECT_REFRESHING_SERVICE_TYPE)) {
            hub.getEcoWateringHubConfiguration().setIsDataObjectRefreshing(context, false, (response) -> {
                if(response.equals(SUCCESS_RESPONSE_SET_IS_DATA_OBJECT_REFRESHING)) {
                    // SERVICE NEED TO RUNNING BECAUSE DEVICE REQUESTS NEED TO BE REFRESHED
                    if((hub.getRemoteDeviceList() != null) && (!hub.getRemoteDeviceList().isEmpty())) {
                        context.stopService(stopIntent);
                        Bundle b = new Bundle();
                        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
                        Intent startIntent = new Intent(context, EcoWateringForegroundService.class);
                        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
                        ContextCompat.startForegroundService(context, startIntent);
                    }
                    else {  // SERVICE CAN BE STOPPED CASE
                        context.stopService(stopIntent);
                    }
                }
            });
        }
        // DEVICE REQUEST REFRESHING STOP CASE
        else if(serviceType.equals(DEVICE_REQUEST_REFRESHING_SERVICE_TYPE)) {
            context.stopService(stopIntent);
            // SERVICE NEED TO RUNNING BECAUSE DATA OBJECT NEED TO BE REFRESHED
            if(hub.getEcoWateringHubConfiguration().isDataObjectRefreshing()) {
                Bundle b = new Bundle();
                b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
                Intent startIntent = new Intent(context, EcoWateringForegroundService.class);
                startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
                ContextCompat.startForegroundService(context, startIntent);
            }
        }
    }
}
