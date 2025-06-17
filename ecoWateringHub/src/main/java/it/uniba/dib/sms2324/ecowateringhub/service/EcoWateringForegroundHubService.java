package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
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
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystemScheduling;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.DeviceRequestRefreshingRunnable;
import it.uniba.dib.sms2324.ecowateringhub.data.DataObjectRefreshingRunnable;

public class EcoWateringForegroundHubService extends Service {
    private static final String SET_BATTERY_PERCENT_SUCCESS_RESPONSE = "batteryPercentSet";
    private static final String TAG_IRRIGATION_SYSTEM_START = "START_IRRIGATION_SYSTEM";
    private static final String NAME_IRRIGATION_SYSTEM_START = "startIrrigationSystem";
    public static final String TAG_IRRIGATION_SYSTEM_MANUAL_START = "START_MANUAL_IRRIGATION_SYSTEM";
    public static final String NAME_IRRIGATION_SYSTEM_MANUAL_START = "startManualIrrigationSystem";
    protected static final String TAG_IRRIGATION_SYSTEM_MANUAL_STOP = "STOP_MANUAL_IRRIGATION_SYSTEM";
    public static final String NAME_IRRIGATION_SYSTEM_MANUAL_STOP = "stOPManualIrrigationSystem";
    private static final String TAG_IRRIGATION_SYSTEM_STOP = "STOP_IRRIGATION_SYSTEM";
    private static final String NAME_IRRIGATION_SYSTEM_STOP = "stopIrrigationSystem";
    private static final String TAG_IRRIGATION_PLAN_REFRESH = "REFRESH_IRRIGATION_PLAN";
    private static final String NAME_IRRIGATION_PLAN_REFRESH = "refreshIrrigationPlan";
    private static final int NOTIFICATION_ID = 1;
    private static final String WAKE_LOCK_TAG = "ecoWateringHub:wakeLockEcoWateringHubService";
    private static final String CHANNEL_ID = "EcoWateringHubServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "ecoWateringHub Service";
    private static final long DEVICE_REQUESTS_REFRESHING_FREQUENCY = 3 * 1000; // MILLISECONDS
    private static final long DATA_OBJECT_REFRESHING_FREQUENCY = 60 * 1000;
    private static final long HUB_REFRESHING_FREQUENCY = 20 * 1000;
    private EcoWateringHub hub;
    private PowerManager.WakeLock wakeLock;
    private Thread deviceRequestRefreshingThread;
    private Thread dataObjectRefreshingThread;
    private Thread hubRefreshingThread;

    public EcoWateringForegroundHubService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification(this, this.hub));
        Log.i(Common.LOG_SERVICE, "EcoWateringForegroundHubService -> onCreate()");
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
                    // CHECK BATTERY STATE
                    this.hub.setBatteryPercent(this, getBatteryPercentage(this), (response -> {
                        if(response.equals(SET_BATTERY_PERCENT_SUCCESS_RESPONSE)) {
                            // REFRESH HUB AND UPDATE NOTIFICATION
                            EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
                                this.hub = new EcoWateringHub(jsonResponse);
                                MainActivity.setThisEcoWateringHub(this.hub);
                                updateNotification(this, this.hub);
                            }));
                        }
                    }));
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
        if ((this.wakeLock != null) && this.wakeLock.isHeld())
            this.wakeLock.release();
        if(this.deviceRequestRefreshingThread != null)
            this.deviceRequestRefreshingThread.interrupt();
        if(this.dataObjectRefreshingThread != null)
            this.dataObjectRefreshingThread.interrupt();
        if(this.hubRefreshingThread != null)
            this.hubRefreshingThread.interrupt();
        stopForeground(true);
        stopSelf();
        Log.i(Common.LOG_SERVICE, "ecoWateringForegroundHubService -> onDestroy");
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
        if(HttpHelper.isDeviceConnectedToInternet(context)) {
            if(hub != null) text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ambient_temperature_label) + ": " + hub.getAmbientTemperature() + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.relative_humidity_label) + ": " + hub.getRelativeHumidity() + "%\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.light_uv_index_label) + ": " + hub.getIndexUV() + "\n" +
                    context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.precipitation_label) + ": " + hub.getWeatherInfo().getPrecipitation() + context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.precipitation_measurement_unit);
            else text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.starting_label);
            // OVERRIDE NOTIFICATION FOR BATTERY LOW CASE
            if((getBatteryPercentage(context) >= 0) && (getBatteryPercentage(context) <= 20))
                text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.low_battery_notification);
        }
        else
            text = context.getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setSmallIcon(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.small_simple_logo)
                .build();
    }

    private static void updateNotification(@NonNull Context context, @NonNull EcoWateringHub hub) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(context, hub));
    }

    private static int getBatteryPercentage(@NonNull Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0)
                return Math.round((level * 100f) / scale);
        }
        return -1;
    }

    public static void checkEcoWateringForegroundServiceNeedToBeStarted(@NonNull Context context, @NonNull EcoWateringHub hub) {
        if((hub.isDataObjectRefreshing()) ||
                ((hub.getRemoteDeviceList() != null) && !hub.getRemoteDeviceList().isEmpty())) {
            startEcoWateringForegroundHubService(context, hub);
        }
        else {  // TO BE SURE ECO WATERING FOREGROUND SERVICE IS NO ALIVE
            stopEcoWateringForegroundHubService(context);
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

    public static void scheduleManualIrrSysWorker(@NonNull Context context, @NonNull Bundle b) {
        int[] startingDate = b.getIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_DATE);
        int[] startingTime = b.getIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_TIME);
        int[] irrigationDuration = b.getIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION);
        if((startingDate != null) && (startingTime != null) && (irrigationDuration != null)) {
            SharedPreferencesHelper.writeIntOnSharedPreferences(context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.HOUR_OF_DAY), irrigationDuration[0]);
            SharedPreferencesHelper.writeIntOnSharedPreferences(context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.MINUTE), irrigationDuration[1]);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, startingDate[0]);
            calendar.set(Calendar.MONTH, startingDate[1] - 1);
            calendar.set(Calendar.DAY_OF_MONTH, startingDate[2]);
            calendar.set(Calendar.HOUR_OF_DAY, startingTime[0]);
            calendar.set(Calendar.MINUTE, startingTime[1]);

            long currentTime = System.currentTimeMillis();
            long startDelay = calendar.getTimeInMillis() - currentTime;
            // TO START IRR SYS
            if(startDelay < 0) {
                sendOneTimeWorkRequest(context, (10 * 1000), TAG_IRRIGATION_SYSTEM_MANUAL_START, NAME_IRRIGATION_SYSTEM_MANUAL_START);
                Log.i(Common.LOG_SERVICE, "IrrSysStartManualWorker now");
                calendar = Calendar.getInstance();
                calendar.setTimeInMillis(currentTime);
                calendar.add(Calendar.HOUR_OF_DAY, irrigationDuration[0]);
                calendar.add(Calendar.MINUTE, irrigationDuration[1]);
                long stopDelay = ((long)irrigationDuration[0] * 60 * 60 * 1000) + ((long)irrigationDuration[1] * 60 * 1000);
                sendOneTimeWorkRequest(context, stopDelay, TAG_IRRIGATION_SYSTEM_MANUAL_STOP, NAME_IRRIGATION_SYSTEM_MANUAL_STOP);
                // UPDATE SCHEDULING ON DATABASE
                startingDate[0] = calendar.get(Calendar.YEAR);
                startingDate[1] = calendar.get(Calendar.MONTH);
                startingDate[2] = calendar.get(Calendar.DAY_OF_MONTH);
                startingTime[0] = calendar.get(Calendar.HOUR_OF_DAY);
                startingTime[1] = calendar.get(Calendar.MINUTE);
                Bundle b1 = new Bundle();
                b1.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_DATE, startingDate);
                b1.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_TIME, startingTime);
                b1.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION, irrigationDuration);
                IrrigationSystem.setScheduling(context, b1, null);
            }
            else {
                sendOneTimeWorkRequest(context, startDelay, TAG_IRRIGATION_SYSTEM_MANUAL_START, NAME_IRRIGATION_SYSTEM_MANUAL_START);
                Log.i(Common.LOG_SERVICE, "IrrSysStartManualWorker at " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
                calendar.add(Calendar.HOUR_OF_DAY, irrigationDuration[0]);
                calendar.add(Calendar.MINUTE, irrigationDuration[1]);
                long stopDelay = calendar.getTimeInMillis() - currentTime;
                sendOneTimeWorkRequest(context, stopDelay, TAG_IRRIGATION_SYSTEM_MANUAL_STOP, NAME_IRRIGATION_SYSTEM_MANUAL_STOP);
            }
            Log.i(Common.LOG_SERVICE, "IrrSysStopManualWorker at " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
        }
    }

    public static void cancelIrrSysManualSchedulingWorker(@NonNull Context context, EcoWateringHub hub) {
        WorkManager.getInstance(context)
                .cancelUniqueWork(NAME_IRRIGATION_SYSTEM_MANUAL_START);
        WorkManager.getInstance(context)
                .cancelUniqueWork(NAME_IRRIGATION_SYSTEM_MANUAL_STOP);
        IrrigationSystem.setScheduling(context, null, null);
        hub.getIrrigationSystem().setState(Common.getThisDeviceID(context), Common.getThisDeviceID(context), false);
    }

    private static void sendPeriodicWorkRequest(@NonNull Context context, Class<? extends ListenableWorker> worker, long initialDelay, String tag, String name) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(worker, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build();

        WorkManager.getInstance(context)
                .cancelUniqueWork(name)
                .getResult()
                .addListener(
                        () -> WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request),
                        Executors.newSingleThreadExecutor()
                );
    }

    protected static void sendOneTimeWorkRequest(@NonNull Context context, long initialDelay, String tag, String name) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(IrrigationSystemManualSetWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request);
    }

    private static Calendar getCalendarPlusXMinutes(int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, minute);
        return calendar;
    }

    private static void startEcoWateringForegroundHubService(@NonNull Context context, @NonNull EcoWateringHub hub) {
        stopEcoWateringForegroundHubService(context);  // TO BE SURE ECO WATERING FOREGROUND SERVICE IS UNIQUE
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
        Intent startIntent = new Intent(context, EcoWateringForegroundHubService.class);
        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        ContextCompat.startForegroundService(context, startIntent);
    }

    private static void stopEcoWateringForegroundHubService(@NonNull Context context) {
        Intent stopIntent = new Intent(context, EcoWateringForegroundHubService.class);
        context.stopService(stopIntent);
    }
}
