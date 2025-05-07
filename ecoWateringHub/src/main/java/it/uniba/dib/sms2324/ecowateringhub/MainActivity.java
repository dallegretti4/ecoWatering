package it.uniba.dib.sms2324.ecowateringhub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.AmbientTemperatureSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.LightSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.RelativeHumiditySensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.entry.AutomaticControlFragment;
import it.uniba.dib.sms2324.ecowateringhub.connection.ManageConnectedRemoteEWDevicesActivity;
import it.uniba.dib.sms2324.ecowateringhub.entry.ManualControlFragment;
import it.uniba.dib.sms2324.ecowateringhub.configuration.EcoWateringConfigurationActivity;
import it.uniba.dib.sms2324.ecowateringhub.runnable.weather.WeatherInfoRunnable;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartFirstFragment;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartSecondFragment;

public class MainActivity extends AppCompatActivity implements
        StartFirstFragment.OnFirstStartFinishCallback,
        StartSecondFragment.OnSecondStartFinishCallback,
        ManualControlFragment.OnUserActionCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment.OnUserProfileActionCallback {
    private static final String FIRST_START_FILENAME_FLAG = "isFirstStartCheck";
    private static final String FIRST_START_KEY_FLAG = "IS_FIRST_START";
    private static final String FIRST_START_VALUE_FLAG = "true";
    private static final String FIRST_START_KEY_NOT_FOUND_FLAG = "keyNotFound";
    private static final int FORCE_SENSORS_INTERVAL_DURATION = 500; // millis
    public static boolean isSimulation = true;
    private static EcoWateringHub thisEcoWateringHub;
    private FragmentManager fragmentManager;
    private static String tempHubName;
    private static Address tempAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();
        if(HttpHelper.isDeviceConnectedToInternet(this)) {    // CHECK INTERNET CONNECTION
            if(savedInstanceState == null) {    // CHECK IS NOT CONFIGURATION CHANGED
                changeFragment(new LoadingFragment(), false);    // SHOW LOADING FRAGMENT
                EcoWateringHub.exists(Common.getThisDeviceID(this), ((existsResponse) -> {  // CHECK DEVICE IS REGISTERED
                    // NOT FIRST START CASE
                    if(!existsResponse.equals(HttpHelper.HTTP_RESPONSE_ERROR)) {
                        thisEcoWateringHub = new EcoWateringHub(existsResponse);
                        forceSensorsUpdate(this, () -> {
                            if(thisEcoWateringHub.getEcoWateringHubConfiguration().isAutomated()) {
                                changeFragment(new AutomaticControlFragment(), true);
                            }
                            else {
                                changeFragment(new ManualControlFragment(), true);
                            }
                        });
                    }
                    // FIRST START CASE
                    else {
                        runOnUiThread(this::showWhyGrantLocationPermissionDialog);
                    }
                }));
            }
        }
        else {
            showInternetFaultDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // CALLED FROM StartFirstFragment -> startFindLocation
        if(requestCode == Common.LOCATION_PERMISSION_REQUEST) {
            if((grantResults.length > 0) && (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            else if(grantResults.length > 0) {
                // accepted case
                changeFragment(new StartFirstFragment(), false);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Common.GPS_ENABLE_REQUEST) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onFirstStartFinish(@NonNull String hubName, @NonNull Address address) {
        tempHubName = hubName;
        tempAddress = address;
        changeFragment(new StartSecondFragment(), true);
    }

    @Override
    public void onSecondStartFinish(@NonNull IrrigationSystem irrigationSystem) {
        EcoWateringHub.addNewEcoWateringHub(
                this,
                tempHubName,
                tempAddress,
                irrigationSystem,
                (() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
        );
    }

    @Override
    public void onCardSelected(Class<?> cardActivityClass) {
        if(cardActivityClass == ManageConnectedRemoteEWDevicesActivity.class) {
            startActivity(new Intent(this, ManageConnectedRemoteEWDevicesActivity.class));
        }
        else if(cardActivityClass.equals(EcoWateringConfigurationActivity.class)) {
            startActivity(new Intent(this, EcoWateringConfigurationActivity.class));
        }
        finish();
    }

    @Override
    public void onAutomateIrrigationSystem() {
    }

    @Override
    public void refreshManualControlFragment() {
        fragmentManager.popBackStack();
        changeFragment(new ManualControlFragment(), true);
    }

    @Override
    public void onUserProfileSelected() {
        changeFragment(new UserProfileFragment(Common.CALLED_FROM_HUB), true);
    }

    // FROM UserProfileFragment
    @Override
    public void onUserProfileGoBack() {
        fragmentManager.popBackStack();
    }

    @Override
    public void onUserProfileRefresh() {
        fragmentManager.popBackStack();
        changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment(Common.CALLED_FROM_HUB), true);
    }

    @Override
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Fragment fragment -> to replace the fragment;
     * To replace the fragment.
     */
    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        if(addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    public static void forceSensorsUpdate(@NonNull Context context, Common.OnMethodFinishCallback callback) {
        Thread ambientTemperatureSensorForcedThread = new Thread(), lightSensorForcedThread = new Thread(), relativeHumiditySensorForcedThread = new Thread();
        new Thread(new WeatherInfoRunnable(context)).start();
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
            ambientTemperatureSensorForcedThread = new Thread(new AmbientTemperatureSensorRunnable(context, MainActivity.thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
            ambientTemperatureSensorForcedThread.start();
        }
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
            lightSensorForcedThread = new Thread(new LightSensorRunnable(context, MainActivity.thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
            lightSensorForcedThread.start();
        }
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
            relativeHumiditySensorForcedThread = new Thread(new RelativeHumiditySensorRunnable(context, MainActivity.thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
            relativeHumiditySensorForcedThread.start();
        }
        while(ambientTemperatureSensorForcedThread.isAlive() || lightSensorForcedThread.isAlive() || relativeHumiditySensorForcedThread.isAlive()) {
            // WAIT UNTIL SENSORS THREAD ARE INTERRUPTED
            Log.i(Common.THIS_LOG, "threads are alive");
            try{
                Thread.sleep(300);
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(context), (jsonResponse) -> {
            thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            callback.canContinue();
        });
    }

    public static EcoWateringHub getThisEcoWateringHub() {
        return thisEcoWateringHub;
    }

    public static void setThisEcoWateringHub(@NonNull EcoWateringHub ecoWateringHub) {
        thisEcoWateringHub = ecoWateringHub;
    }

    /**
     * Notify the user, why app needs to know device's location.
     * Positive button to go to the FirstStartFragment.
     * If app cannot requests permissions directly, Positive Button open the app details setting.
     */
    private void showWhyGrantLocationPermissionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.why_grant_location_permission_dialog_title));

        // CHECK IF IS FIRST START
        boolean firstStartDialogFlag = false;
        SharedPreferences sharedPreferences = getSharedPreferences(FIRST_START_FILENAME_FLAG, Context.MODE_PRIVATE);
        if(sharedPreferences.getString(FIRST_START_KEY_FLAG, FIRST_START_KEY_NOT_FOUND_FLAG).equals(FIRST_START_KEY_NOT_FOUND_FLAG)) {
            firstStartDialogFlag = true;
        }

        // USER MUST GRANT LOCATION PERMISSION MANUALLY CASE
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) && (!firstStartDialogFlag)) {
            dialog.setMessage(getString(R.string.why_grant_location_permission_manually_dialog_message))
                    .setPositiveButton(
                            getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.setting_button),
                            (dialogInterface, i) -> Common.openAppDetailsSetting(this)
                    )
                    .setCancelable(false);
        }
        // APP CAN REQUEST PERMISSION CASE
        else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FIRST_START_KEY_FLAG, FIRST_START_VALUE_FLAG);
            editor.apply();
            dialog.setMessage(getString(R.string.why_grant_location_permission_dialog_message))
                    .setPositiveButton(
                            getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ok_button),
                            (dialogInterface, i) -> changeFragment(new StartFirstFragment(), false)
                    )
                    .setCancelable(false);
        }
        dialog.show();
    }

    /**
     * Notify the user there isn't internet connection.
     * Positive button restarts the app.
     */
    private void showInternetFaultDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) finish();
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}