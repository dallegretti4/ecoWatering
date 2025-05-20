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
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlanPreview;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageHubAutomaticControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageHubManualControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment;
import it.uniba.dib.sms2324.ecowateringhub.configuration.SensorConfigurationFragment;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.AmbientTemperatureSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.LightSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.RelativeHumiditySensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.connection.ManageConnectedRemoteEWDevicesActivity;
import it.uniba.dib.sms2324.ecowateringhub.runnable.weather.WeatherInfoRunnable;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartFirstFragment;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartSecondFragment;

public class MainActivity extends AppCompatActivity implements
        StartFirstFragment.OnFirstStartFinishCallback,
        StartSecondFragment.OnSecondStartFinishCallback,
        ManageHubManualControlFragment.OnHubActionChosenCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment.OnUserProfileActionCallback,
        SensorConfigurationFragment.OnSensorConfiguredCallback {
    private static final int FORCE_SENSORS_INTERVAL_DURATION = 500; // millis
    public static boolean isSimulation = true;
    private static EcoWateringHub thisEcoWateringHub;
    private FragmentManager fragmentManager;
    private static String tempHubName;
    private static Address tempAddress;
    private static boolean isWhyGrantLocationPermissionDialogVisible;
    private static boolean isInternetFaultDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(Common.THIS_LOG, "MainActivity -> onCreate()");
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {    // CHECK IS NOT CONFIGURATION CHANGED
            changeFragment(new LoadingFragment(), false);    // SHOW LOADING FRAGMENT
            EcoWateringHub.exists(Common.getThisDeviceID(this), ((existsResponse) -> {  // CHECK DEVICE IS REGISTERED
                // NOT FIRST START CASE
                if(!existsResponse.equals(HttpHelper.HTTP_RESPONSE_ERROR)) {
                    thisEcoWateringHub = new EcoWateringHub(existsResponse);
                    IrrigationPlanPreview.getIrrigationPlanPreviewJsonString(thisEcoWateringHub, Common.getThisDeviceID(this), (response) -> {
                        new IrrigationPlanPreview(response).updateIrrigationPlanOnServer(Common.getThisDeviceID(this), Common.getThisDeviceID(this));
                    });

                    checkEcoWateringServiceNeedToStart();
                    forceSensorsUpdate(this, () -> {
                        if(thisEcoWateringHub.getEcoWateringHubConfiguration().isAutomated()) changeFragment(new ManageHubAutomaticControlFragment(), true);
                        else changeFragment(new ManageHubManualControlFragment(Common.CALLED_FROM_HUB, thisEcoWateringHub), true);
                    });
                }
                else {  // FIRST START CASE
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) changeFragment(new StartFirstFragment(), false);
                    else runOnUiThread(this::showWhyGrantLocationPermissionDialog);
                }
            }));
        }
        else {
            if(isWhyGrantLocationPermissionDialogVisible) runOnUiThread(this::showWhyGrantLocationPermissionDialog);
            else if(isInternetFaultDialog) runOnUiThread(this::showInternetFaultDialog);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!HttpHelper.isDeviceConnectedToInternet(this)) runOnUiThread(this::showInternetFaultDialog);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // USER IS RETURNED FROM SETTING CASE
        if(SharedPreferencesHelper.readBooleanOnSharedPreferences(this, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY)) {
            SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY, false);
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) changeFragment(new StartFirstFragment(), false);
            else runOnUiThread(this::showWhyGrantLocationPermissionDialog);
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
            if(resultCode == RESULT_OK) {
                startActivity(new Intent(this, MainActivity.class));
            }
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

    // FROM ManageHubManualControlFragment
    @Override
    public void onBackPressedFromManageHub() {
        finish();
    }

    @Override
    public void refreshFragment() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onSecondToolbarFunctionChosen() {
        changeFragment(new UserProfileFragment(Common.CALLED_FROM_HUB), true);
    }

    @Override
    public void manageConnectedRemoteDevices() {
        startActivity(new Intent(this, ManageConnectedRemoteEWDevicesActivity.class));
        finish();
    }

    @Override
    public void automateEcoWateringSystem() {}

    @Override
    public void setDataObjectRefreshing(boolean value) {
        if(value) EcoWateringForegroundService.startEcoWateringService(thisEcoWateringHub, this, EcoWateringForegroundService.DATA_OBJECT_REFRESHING_SERVICE_TYPE);
        else EcoWateringForegroundService.stopEcoWateringService(thisEcoWateringHub, this, EcoWateringForegroundService.DATA_OBJECT_REFRESHING_SERVICE_TYPE);
    }

    @Override
    public void setIrrigationSystemState(boolean value) {
        thisEcoWateringHub.getEcoWateringHubConfiguration().getIrrigationSystem().setState(Common.getThisDeviceID(this), Common.getThisDeviceID(this), value);
    }

    @Override
    public void configureSensor(int sensorType) {
        changeFragment(new SensorConfigurationFragment(sensorType), true);
    }

    @Override
    public void forceSensorsUpdate(Common.OnMethodFinishCallback callback) {
        forceSensorsUpdate(this, callback);
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

    @Override   // CALLED FROM ManageHubManualControlFragment TOO
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // FROM SensorConfigurationFragment.OnSensorConfiguredCallback
    @Override
    public void onSensorConfigured(int sensorResult) {
        if(sensorResult == Common.ACTION_BACK_PRESSED) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        else if(sensorResult == Common.REFRESH_FRAGMENT) {
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(), true);
        }
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

    private void checkEcoWateringServiceNeedToStart() {
        if(thisEcoWateringHub.getEcoWateringHubConfiguration().isDataObjectRefreshing()) EcoWateringForegroundService.startEcoWateringService(thisEcoWateringHub, this, EcoWateringForegroundService.DATA_OBJECT_REFRESHING_SERVICE_TYPE);
        else if((thisEcoWateringHub.getRemoteDeviceList() != null) && (!thisEcoWateringHub.getRemoteDeviceList().isEmpty())) EcoWateringForegroundService.startEcoWateringService(thisEcoWateringHub, this, EcoWateringForegroundService.DEVICE_REQUEST_REFRESHING_SERVICE_TYPE);
        else EcoWateringForegroundService.stopEcoWateringService(thisEcoWateringHub, this, EcoWateringForegroundService.DEVICE_REQUEST_REFRESHING_SERVICE_TYPE);
    }

    public static void forceSensorsUpdate(@NonNull Context context, Common.OnMethodFinishCallback callback) {
        Thread ambientTemperatureSensorForcedThread = new Thread(), lightSensorForcedThread = new Thread(), relativeHumiditySensorForcedThread = new Thread();
        new Thread(new WeatherInfoRunnable(context)).start();
        if((thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
            ambientTemperatureSensorForcedThread = new Thread(new AmbientTemperatureSensorRunnable(context, thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
            ambientTemperatureSensorForcedThread.start();
        }
        if((thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
            lightSensorForcedThread = new Thread(new LightSensorRunnable(context, thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
            lightSensorForcedThread.start();
        }
        if((thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
            relativeHumiditySensorForcedThread = new Thread(new RelativeHumiditySensorRunnable(context, thisEcoWateringHub, FORCE_SENSORS_INTERVAL_DURATION));
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
        isWhyGrantLocationPermissionDialogVisible = true;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.why_grant_location_permission_dialog_title));
        // CHECK IF IS FIRST START
        boolean firstStartDialogFlag = SharedPreferencesHelper.readStringOnSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY).equals(Common.NULL_STRING_VALUE);
        // USER MUST GRANT LOCATION PERMISSION MANUALLY CASE
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) && (!firstStartDialogFlag)) {
            dialog.setMessage(getString(R.string.why_grant_location_permission_manually_dialog_message))
                    .setPositiveButton(
                            getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.setting_button),
                            (dialogInterface, i) -> {
                                isWhyGrantLocationPermissionDialogVisible = false;
                                SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY, true);
                                Common.openAppDetailsSetting(this);
                            }
                    ).setCancelable(false);
        }
        else {  // APP CAN REQUEST PERMISSION CASE
            dialog.setMessage(getString(R.string.why_grant_location_permission_dialog_message))
                    .setPositiveButton(
                            getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.ok_button),
                            (dialogInterface, i) -> {
                                isWhyGrantLocationPermissionDialogVisible = false;
                                SharedPreferencesHelper.writeStringOnSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY, SharedPreferencesHelper.FIRST_START_VALUE_FLAG);
                                changeFragment(new StartFirstFragment(), false);
                            }
                    );
        }
        dialog.setNegativeButton(
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                (dialogInterface, i) -> {
                    isWhyGrantLocationPermissionDialogVisible = false;
                    SharedPreferencesHelper.writeStringOnSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY, SharedPreferencesHelper.FIRST_START_VALUE_FLAG);
                    finish();
                }
        ).setCancelable(false);
        dialog.show();
    }

    /**
     * Notify the user there isn't internet connection.
     * Positive button restarts the app.
     */
    private void showInternetFaultDialog() {
        isInternetFaultDialog = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isInternetFaultDialog = false;
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    isInternetFaultDialog = false;
                    if(keyCode == KeyEvent.KEYCODE_BACK) finish();
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}