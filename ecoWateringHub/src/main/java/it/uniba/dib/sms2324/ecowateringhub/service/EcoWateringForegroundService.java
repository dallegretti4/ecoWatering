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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.DeviceRequestRefreshingRunnable;
import it.uniba.dib.sms2324.ecowateringhub.data.DataObjectRefreshingRunnable;

public class EcoWateringForegroundService extends Service {
    private static final String TAG_IRRIGATION_SYSTEM_START = "START_IRRIGATION_SYSTEM";
    private static final String NAME_IRRIGATION_SYSTEM_START = "startIrrigationSystem";
    private static final String TAG_IRRIGATION_SYSTEM_STOP = "STOP_IRRIGATION_SYSTEM";
    private static final String NAME_IRRIGATION_SYSTEM_STOP = "stopIrrigationSystem";
    private static final String TAG_IRRIGATION_PLAN_REFRESH = "REFRESH_IRRIGATION_PLAN";
    private static final String NAME_IRRIGATION_PLAN_REFRESH = "refreshIrrigationPlan";
    private static final int NOTIFICATION_ID = 1;
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
        Log.i(Common.LOG_SERVICE, "EcoWateringForegroundService -> onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = intent.getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
        this.hub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
        if(this.hub != null) {
            // DEVICE REQUESTS REFRESHING NEED TO BE STARTED
            this.deviceRequestRefreshingThread = new Thread(() -> {
                while ((this.hub.getRemoteDeviceList() != null) && (!this.hub.getRemoteDeviceList().isEmpty()) && (this.deviceRequestRefreshingThread != null) && (!this.deviceRequestRefreshingThread.isInterrupted())) {
                    Log.i(Common.LOG_SERVICE, "-------------------------> deviceRequestRefreshingServiceRunning iteration");
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
                    Log.i(Common.LOG_SERVICE, "-------------------------> dataObjectRefreshingServiceRunning iteration");
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
                    Log.i(Common.LOG_SERVICE, "-------------------------> hubRefreshingServiceRunning iteration");
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
        Log.i(Common.LOG_SERVICE, "ecoWateringForegroundService -> onDestroy");
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
        if(hub != null) text = context.getString(R.string.sensors_notification_text) + new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.getDefault()).format(new Date()) + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ambient_temperature_label) + ": " + hub.getAmbientTemperature() + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.relative_humidity_label) + ": " + hub.getRelativeHumidity() + "%\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.light_uv_index_label) + ": " + hub.getIndexUV() + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.precipitation_label) + ": " + hub.getWeatherInfo().getPrecipitation() + context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.precipitation_measurement_unit);
        else text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.notification_data_object_refreshing_disabled);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sensors_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setSmallIcon(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.small_simple_logo)
                .build();
    }

    private static void updateNotification(@NonNull Context context, @NonNull EcoWateringHub hub) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(context, hub));
    }

    public static void checkEcoWateringForegroundServiceNeedToBeStarted(@NonNull Context context, @NonNull EcoWateringHub hub) {
        if((hub.isDataObjectRefreshing()) ||
                ((hub.getRemoteDeviceList() != null) && !hub.getRemoteDeviceList().isEmpty())) {
            EcoWateringForegroundService.startEcoWateringForegroundService(context, hub);
        }
        else {  // TO BE SURE ECO WATERING FOREGROUND SERVICE IS NO ALIVE
            EcoWateringForegroundService.stopEcoWateringForegroundService(context);
        }
    }

    public static void checkEcoWateringSystemNeedToBeAutomated(@NonNull Context context, @NonNull EcoWateringHub hub) {
        checkEcoWateringForegroundServiceNeedToBeStarted(context, hub); // CHECK FOR ECO WATERING FOREGROUND SERVICE
        if(hub.isAutomated()) {
            scheduleIrrigationPlanRefreshingWorker(context, false);   // REFRESH IRRIGATION PLAN WORKER
            scheduleIrrigationSystemStartingWorker(context, hub, false);   // START IRRIGATION SYSTEM WORKER
            // TO BE SURE STOPPING TIME IS NOT DELAYED
            if(!SharedPreferencesHelper.readBooleanFromSharedPreferences(context, SharedPreferencesHelper.IRR_SYS_START_DELAYED_FILENAME, SharedPreferencesHelper.IRR_SYS_START_DELAYED_KEY))
                scheduleIrrigationSystemStoppingWorker(context, hub, false);   // STOP IRRIGATION SYSTEM WORKER
        }
    }

    protected static void scheduleIrrigationPlanRefreshingWorker(@NonNull Context context, boolean isRetrying) {
        long currentTime = System.currentTimeMillis();
        Calendar calendar;
        if(isRetrying) calendar = getCalendarPlusXMinutes(10);
        else {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 3);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        long initialDelay = calendar.getTimeInMillis() - currentTime;
        if(initialDelay < 0) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            initialDelay = calendar.getTimeInMillis() - currentTime;
        }
        sendPeriodicWorkRequest(context, IrrigationPlanRefreshWorker.class, initialDelay, TAG_IRRIGATION_PLAN_REFRESH, NAME_IRRIGATION_PLAN_REFRESH);
        Log.i(Common.LOG_SERVICE, "---------------------> IrrigationPlanRefreshingWorker at " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
    }

    protected static void scheduleIrrigationSystemStartingWorker(@NonNull Context context, EcoWateringHub hub, boolean isRetrying) {
        long currentTime = System.currentTimeMillis();
        Calendar calendar;
        if(isRetrying) calendar = getCalendarPlusXMinutes(10);
        else {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hub.getIrrigationPlan().getIrrigationSystemStartingHours());
            calendar.set(Calendar.MINUTE, hub.getIrrigationPlan().getIrrigationSystemStartingMinutes());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        long initialDelay = calendar.getTimeInMillis() - currentTime;
        if(initialDelay < 0) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            initialDelay = calendar.getTimeInMillis() - currentTime;
        }
        sendPeriodicWorkRequest(context, IrrigationSystemStartingWorker.class, initialDelay, TAG_IRRIGATION_SYSTEM_START, NAME_IRRIGATION_SYSTEM_START);
        Log.i(Common.LOG_SERVICE, "---------------------> IrrigationSystemStartingWorker at " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
    }

    protected static void scheduleIrrigationSystemStoppingWorker(@NonNull Context context, EcoWateringHub hub, boolean isRetrying) {
        long currentTime = System.currentTimeMillis();
        Calendar calendar;
        if(isRetrying) calendar = getCalendarPlusXMinutes(2);  // NEED TO BE DELAYED FOR INTERNET CONNECTION CASE
        else {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hub.getIrrigationPlan().getIrrigationSystemStoppingHours(0));
            calendar.set(Calendar.MINUTE, hub.getIrrigationPlan().getIrrigationSystemStoppingMinutes(0));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        long initialDelay = calendar.getTimeInMillis() - currentTime;
        if(initialDelay < 0) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, hub.getIrrigationPlan().getIrrigationSystemStoppingHours(1));
            calendar.set(Calendar.MINUTE, hub.getIrrigationPlan().getIrrigationSystemStoppingMinutes(1));
            initialDelay = calendar.getTimeInMillis() - currentTime;
        }
        sendPeriodicWorkRequest(context, IrrigationSystemStoppingWorker.class, initialDelay, TAG_IRRIGATION_SYSTEM_STOP, NAME_IRRIGATION_SYSTEM_STOP);
        Log.i(Common.LOG_SERVICE, "---------------------> IrrigationSystemStoppingWorker at " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
    }

    private static void sendPeriodicWorkRequest(@NonNull Context context, Class<? extends ListenableWorker> worker, long initialDelay, String tag, String name) {
        PeriodicWorkRequest irrigationSystemStartingRequest = new PeriodicWorkRequest.Builder(worker, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build();

        WorkManager.getInstance(context)
                .cancelUniqueWork(name)
                .getResult()
                .addListener(
                        () -> WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, irrigationSystemStartingRequest),
                        Executors.newSingleThreadExecutor()
                );
    }

    private static Calendar getCalendarPlusXMinutes(int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minute);
        return calendar;
    }

    private static void startEcoWateringForegroundService(@NonNull Context context, @NonNull EcoWateringHub hub) {
        stopEcoWateringForegroundService(context);  // TO BE SURE ECO WATERING FOREGROUND SERVICE IS UNIQUE
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
        Intent startIntent = new Intent(context, EcoWateringForegroundService.class);
        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        ContextCompat.startForegroundService(context, startIntent);
    }

    private static void stopEcoWateringForegroundService(@NonNull Context context) {
        Intent stopIntent = new Intent(context, EcoWateringForegroundService.class);
        context.stopService(stopIntent);
    }
}
