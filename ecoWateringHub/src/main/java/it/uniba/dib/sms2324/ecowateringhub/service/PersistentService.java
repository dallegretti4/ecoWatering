package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.runnable.AmbientTemperatureSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.LightSensorEventRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.LightSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.RefreshHubRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.RelativeHumiditySensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.WeatherInfoRunnable;

public class PersistentService extends Service {
    public static final String PERSISTENT_SERVICE_IS_RUNNING_FROM_SHARED_PREFERENCE = "PERSISTENT_SERVICE_IS_RUNNING";
    public static final String PERSISTENT_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE = "PERSISTENT_SERVICE_IS_NOT_RUNNING";
    private static final long SENSORS_WEATHER_POST_DELAY = 15 * 1000;
    private static final long DIFFERENCE_DELAY = 45 * 1000;
    private static final int NOTIFICATION_ID = 3;
    private static boolean isRunning;
    private EcoWateringHub hub;
    private static final String CHANNEL_ID = "PersistentServiceChannel";
    private PowerManager.WakeLock wakeLock;

    public PersistentService() {}

   @Override
   public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
       this.hub = intent.getParcelableExtra(Common.MANAGE_EWH_INTENT_OBJ);
        if(this.hub != null) {
            Log.i(Common.THIS_LOG, "hub != null");
            Log.i(Common.THIS_LOG, "persistent service sp write");
            isRunning = true;
            startPersistentService(this.hub);
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
        isRunning = false;
        stopForeground(true);
        stopSelf();
        Log.i(Common.THIS_LOG, "persistent service onDestroy");
   }

   private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ecoWateringHub:wakeLockPersistentService");
        wakeLock.acquire();
   }

   private void startPersistentService(EcoWateringHub hub) {
        new Thread(() -> {
            while(isRunning) {
                new Thread(new AmbientTemperatureSensorRunnable(this, hub, 5000)).start();
                new Thread(new LightSensorRunnable(this, hub, 5000)).start();
                new Thread(new RelativeHumiditySensorRunnable(this, hub, 5000)).start();
                new Thread(new WeatherInfoRunnable(this)).start();
                // TO DEBUG BACKGROUND SERVICE WORK
                new Thread(new LightSensorEventRunnable()).start();

                try { Thread.sleep(SENSORS_WEATHER_POST_DELAY); }
                catch (InterruptedException e) { e.printStackTrace(); }
                new Thread(new RefreshHubRunnable(hub.getDeviceID())).start();

                try { Thread.sleep(DIFFERENCE_DELAY); }
                catch (InterruptedException e) { e.printStackTrace(); }
                updateNotification();
            }
        }).start();
   }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor & Weather Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        String text;
        if(this.hub != null) {
            text = getString(R.string.sensors_notification_text) + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()) + "\n" +
                    getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ambient_temperature_label) + ": " + this.hub.getAmbientTemperature() + "\n" +
                    getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.relative_humidity_label) + ": " + this.hub.getRelativeHumidity() + "%\n" +
                    getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.light_uv_index_label) + ": " + this.hub.getIndexUV();
        }
        else {
            text = getString(R.string.sensors_notification_text) + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.sensors_notification_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.main_app_logo_no_bg)
                .build();
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }
}
