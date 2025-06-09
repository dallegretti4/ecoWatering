package it.uniba.dib.sms2324.ecowatering.service;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class EcoWateringForegroundDeviceService extends Service {
    private static final String WAKE_LOCK_TAG = "ecoWateringHub:wakeLockEcoWateringDeviceService";
    private static final String CHANNEL_ID = "EcoWateringDeviceServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "ecoWateringDevice Service";
    private static final int NOTIFICATION_ID = 2;
    private EcoWateringDevice thisDevice;
    private List<EcoWateringHub> hubList;
    private PowerManager.WakeLock wakeLock;
    private Thread deviceServiceThread;
    public EcoWateringForegroundDeviceService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
        Log.i(Common.LOG_SERVICE, "EcoWateringForegroundDeviceService -> onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = intent.getBundleExtra(Common.MANAGE_EWD_INTENT_OBJ);
        this.thisDevice = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWD_INTENT_OBJ);
        this.deviceServiceThread = new Thread(() -> {
            while((this.thisDevice != null) && (!this.deviceServiceThread.isInterrupted())) {
                Log.i(Common.LOG_SERVICE, "------------> service device iteration");
                try { Thread.sleep(40000); }
                catch (InterruptedException ignored) {}
                EcoWateringDevice.getEcoWateringDeviceJsonString(Common.getThisDeviceID(this), (jsonDeviceResponse -> {
                    this.thisDevice = new EcoWateringDevice(jsonDeviceResponse);
                    if((this.thisDevice.getEcoWateringHubList() != null) && (!this.thisDevice.getEcoWateringHubList().isEmpty())) {
                        this.hubList = new ArrayList<>();
                        for(String hubID : this.thisDevice.getEcoWateringHubList()) {
                            EcoWateringHub.getEcoWateringHub(hubID, (jsonHubResponse -> this.hubList.add(new EcoWateringHub(jsonHubResponse))));
                        }
                        try { Thread.sleep(10000); }
                        catch (InterruptedException ignored) {}
                        updateNotification();
                    }
                }));
            }
        });
        this.deviceServiceThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if ((this.wakeLock != null) && this.wakeLock.isHeld())
            this.wakeLock.release();
        if(this.deviceServiceThread != null)
            this.deviceServiceThread.interrupt();
        stopForeground(true);
        stopSelf();
        Log.i(Common.LOG_SERVICE, "ecoWateringForegroundDeviceService -> onDestroy");
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
            if(notificationManager != null)
                notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification() {
        StringBuilder stringBuilder = new StringBuilder();
        if((this.hubList != null) && (!this.hubList.isEmpty())) {
            for(EcoWateringHub hub : this.hubList) {
                if(hub.getBatteryPercent() <= 20)
                    stringBuilder.append(hub.getName()).append(getString(it.uniba.dib.sms2324.ecowatering.R.string.hub_need_to_be_recharged, hub.getName())).append("\n");
            }
        }
        if(stringBuilder.toString().equals(Common.VOID_STRING_VALUE))
            stringBuilder.append(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.starting_label));

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(stringBuilder.toString()))
                .setOngoing(true)
                .setSmallIcon(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.small_simple_logo)
                .build();
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    public static void checkIfServiceNeedToBeStarted(@NonNull Context context, @NonNull EcoWateringDevice device) {
        Log.i(Common.LOG_SERVICE, "--------> service device check");
        stopEcoWateringForegroundDeviceService(context);
        if((device.getEcoWateringHubList() != null) && (!device.getEcoWateringHubList().isEmpty()))
            startEcoWateringForegroundDeviceService(context, device);
    }

    private static void startEcoWateringForegroundDeviceService(@NonNull Context context, @NonNull EcoWateringDevice device) {
        stopEcoWateringForegroundDeviceService(context);
        Intent startIntent = new Intent(context, EcoWateringForegroundDeviceService.class);
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWD_INTENT_OBJ, device);
        startIntent.putExtra(Common.MANAGE_EWD_INTENT_OBJ, b);
        ContextCompat.startForegroundService(context, startIntent);
    }

    private static void stopEcoWateringForegroundDeviceService(@NonNull Context context) {
        Intent stopIntent = new Intent(context, EcoWateringForegroundDeviceService.class);
        context.stopService(stopIntent);
    }
}
