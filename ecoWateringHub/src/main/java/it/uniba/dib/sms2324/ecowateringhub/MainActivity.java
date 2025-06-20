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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Calendar;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystemScheduling;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlanPreview;
import it.uniba.dib.sms2324.ecowateringcommon.ui.AutomateSystemFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.hub.ManageHubAutomaticControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.hub.ManageHubFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.hub.ManageHubManualControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.SensorConfigurationFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment;
import it.uniba.dib.sms2324.ecowateringhub.data.DataObjectRefreshingRunnable;
import it.uniba.dib.sms2324.ecowateringhub.data.EcoWateringSensor;
import it.uniba.dib.sms2324.ecowateringhub.connection.ManageEcoWateringDevicesConnectionActivity;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundHubService;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartFirstFragment;
import it.uniba.dib.sms2324.ecowateringhub.setup.StartSecondFragment;

public class MainActivity extends AppCompatActivity implements
        StartFirstFragment.OnFirstStartFinishCallback,
        StartSecondFragment.OnSecondStartFinishCallback,
        ManageHubManualControlFragment.OnHubManualActionChosenCallback,
        ManageHubAutomaticControlFragment.OnManageHubAutomaticControlActionCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment.OnUserProfileActionCallback,
        AutomateSystemFragment.OnAutomateSystemActionCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.SensorConfigurationFragment.OnSensorConfigurationActionCallback {
    public static boolean isSimulation = true;
    private static EcoWateringHub thisEcoWateringHub;
    private FragmentManager fragmentManager;
    private static String tempHubName;
    private static Address tempAddress;
    private static boolean isWhyGrantLocationPermissionDialogVisible;
    private static boolean isEnableGpsDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;
    private static boolean isInternetFaultDialog;

    @Override
    protected void onCreate(Bundle activitySavedInstanceState) {
        super.onCreate(activitySavedInstanceState);
        setContentView(it.uniba.dib.sms2324.ecowateringcommon.R.layout.activity_frame_layout);
        fragmentManager = getSupportFragmentManager();
        if(HttpHelper.isDeviceConnectedToInternet(this)) {
            if(activitySavedInstanceState == null) {
                changeFragment(new LoadingFragment(), false);    // SHOW LOADING FRAGMENT
                EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), ((existsResponse) -> {  // CHECK DEVICE IS REGISTERED
                    // NOT FIRST START CASE
                    if(!existsResponse.equals(HttpHelper.HTTP_RESPONSE_ERROR)) {
                        thisEcoWateringHub = new EcoWateringHub(existsResponse);    // INSTANCE OF THIS HUB
                        new Thread(new DataObjectRefreshingRunnable(this, thisEcoWateringHub, DataObjectRefreshingRunnable.FORCE_SENSORS_DURATION)).start(); // FORCE SENSORS AND WEATHER INFO REFRESHING
                        EcoWateringForegroundHubService.checkEcoWateringSystemNeedToBeAutomated(this, thisEcoWateringHub);
                        if(thisEcoWateringHub.isAutomated()) changeFragment(new ManageHubAutomaticControlFragment(Common.CALLED_FROM_HUB, thisEcoWateringHub), true);
                        else changeFragment(new ManageHubManualControlFragment(Common.CALLED_FROM_HUB, thisEcoWateringHub), true);
                    }
                    else {  // FIRST START CASE
                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) changeFragment(new StartFirstFragment(), false);
                        else runOnUiThread(this::showWhyGrantLocationPermissionDialog);
                    }
                }));
            }
            else {
                if(isWhyGrantLocationPermissionDialogVisible) runOnUiThread(this::showWhyGrantLocationPermissionDialog);
                if(isEnableGpsDialogVisible) runOnUiThread(this::showEnableGpsDialog);
                else if(isHttpErrorFaultDialogVisible) runOnUiThread(this::showHttpErrorFaultDialog);
                else if(isInternetFaultDialog) runOnUiThread(this::showInternetFaultDialog);
            }
        } else showInternetFaultDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // USER IS RETURNED FROM SETTING CASE
        if(SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY)) {
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
                finish();
            }
            else
                runOnUiThread(this::showEnableGpsDialog);
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
        EcoWateringHub.addNewEcoWateringHub(this, tempHubName, tempAddress, irrigationSystem, (() -> {
            updateSensorsList();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }));
    }

    // FROM ManageHubFragment.OnManageHubActionCallback
    @Override
    public void onManageHubBackPressed() {
        finish();
    }

    @Override
    public void onManageHubRefreshFragment() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onManualSecondToolbarFunctionChosen() {
        fragmentManager.popBackStack();
        changeFragment(new UserProfileFragment(Common.CALLED_FROM_HUB), true);
    }

    @Override
    public void refreshDataObject(EcoWateringHub.OnEcoWateringHubGivenCallback callback) {
        new Thread(new DataObjectRefreshingRunnable(this, thisEcoWateringHub, ManageHubFragment.DATA_OBJECT_REFRESHING_DURATION)).start();
        EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
            thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            callback.getEcoWateringHub(thisEcoWateringHub);
        }));
    }

    @Override
    public void manageConnectedRemoteDevices() {
        startActivity(new Intent(this, ManageEcoWateringDevicesConnectionActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left);
        finish();
    }

    @Override
    public void configureSensor(String sensorType) {
        SensorsInfo.updateSensorList(this, Common.getThisDeviceID(this), sensorType, EcoWateringSensor.getConnectedSensorList(this, sensorType));
        EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
            thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            changeFragment(new SensorConfigurationFragment(thisEcoWateringHub, Common.CALLED_FROM_HUB, sensorType), true);
        }));
    }

    // FROM ManageHubAutomaticControlFragment.OnHubAutomaticActionChosenCallback
    @Override
    public void controlSystemManually() {
        thisEcoWateringHub.setIsAutomated(false, (response) -> {
            if(!response.equals(EcoWateringHub.SET_IS_AUTOMATED_SUCCESS_RESPONSE)) {
                runOnUiThread(this::showHttpErrorFaultDialog);
                restartApp();
            }
        });
    }

    // FROM ManageHubManualControlFragment.OnHubManualActionChosenCallback

    @Override
    public void automateEcoWateringSystem() {
        EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
            thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new AutomateSystemFragment(thisEcoWateringHub, Common.CALLED_FROM_HUB), true);
        }));
    }

    @Override
    public void setIrrigationSystemState(boolean value) {
        EcoWateringForegroundHubService.cancelIrrSysManualSchedulingWorker(this);
        IrrigationSystem.setScheduling(this, null, null, (response -> {
            thisEcoWateringHub.getIrrigationSystem().setState(Common.getThisDeviceID(this), Common.getThisDeviceID(this), value);
        }));
    }

    @Override
    public void setDataObjectRefreshing(boolean value) {
        thisEcoWateringHub.setIsDataObjectRefreshing(this, value, (response) -> {
            if(response.equals(EcoWateringHub.SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE)) {
                EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
                    thisEcoWateringHub = new EcoWateringHub(jsonResponse);
                    EcoWateringForegroundHubService.checkEcoWateringForegroundServiceNeedToBeStarted(this, thisEcoWateringHub);
                }));
            }
            else runOnUiThread(this::showHttpErrorFaultDialog);
        });
    }

    @Override
    public void scheduleIrrSys(Calendar calendar, int[] irrigationDuration) {
        if(calendar != null) {
            //  SCHEDULE CASE
            IrrigationSystem.setScheduling(this, calendar, irrigationDuration, (response -> {
                if(response.equals(IrrigationSystem.IRRIGATION_SYSTEM_SET_SCHEDULING_RESPONSE)) {
                    EcoWateringForegroundHubService.scheduleManualIrrSysWorker(this, calendar, irrigationDuration);
                    thisEcoWateringHub.getIrrigationSystem().setState(Common.getThisDeviceID(this), Common.getThisDeviceID(this), false);
                } else
                    runOnUiThread(this::showHttpErrorFaultDialog);
            }));
        }
        else {  // DELETE SCHEDULING CASE
            EcoWateringForegroundHubService.cancelIrrSysManualSchedulingWorker(this);
            IrrigationSystem.setScheduling(this, null, null, (response ->
                thisEcoWateringHub.getIrrigationSystem().setState(Common.getThisDeviceID(this), Common.getThisDeviceID(this), false))
            );
        }
    }

    @Override   // CALLED FROM UserProfileFragment.OnUserProfileActionCallback AND AutomateSystemFragment.OnAutomateSystemActionCallback TOO
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // FROM UserProfileFragment
    @Override
    public void onUserProfileGoBack() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onUserProfileRefresh() {
        fragmentManager.popBackStack();
        changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment(Common.CALLED_FROM_HUB), true);
    }

    // FROM SensorConfigurationFragment.OnSensorConfigurationActionCallback
    @Override
    public void onSensorConfigurationGoBack() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }
    @Override
    public void onSensorConfigurationRefreshFragment(String sensorType) {
        EcoWateringHub.getEcoWateringHub(thisEcoWateringHub.getDeviceID(), (jsonResponse -> {
            thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(thisEcoWateringHub, Common.CALLED_FROM_HUB, sensorType), true);
        }));
    }
    @Override
    public void onSensorConfigurationRestartApp() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    // FROM AutomateSystemFragment.OnAutomateSystemActionCallback
    @Override
    public void onAutomateSystemGoBack() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onAutomateSystemAgreeIrrigationPlan(@NonNull IrrigationPlanPreview irrigationPlanPreview) {
        irrigationPlanPreview.updateIrrigationPlanOnServer(this, Common.getThisDeviceID(this)); // UPDATE IRRIGATION PLAN ON SERVER
        thisEcoWateringHub.setIsAutomated(true, (response) -> {
            if(response.equals(EcoWateringHub.SET_IS_AUTOMATED_SUCCESS_RESPONSE)) {
                EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this), (jsonResponse -> {
                    thisEcoWateringHub = new EcoWateringHub(jsonResponse);
                    EcoWateringForegroundHubService.checkEcoWateringSystemNeedToBeAutomated(this, thisEcoWateringHub);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }));
            }
            else runOnUiThread(this::showHttpErrorFaultDialog);
        });
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Fragment fragment -> to replace the fragment;
     * To replace the fragment.
     */
    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        runOnUiThread(() -> {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // INSERT SLIDE ANIMATION ON SensorConfigurationFragment
            if((fragment instanceof SensorConfigurationFragment)) { // FOR SensorConfigurationFragment
                if(!SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY))
                    fragmentTransaction.setCustomAnimations(
                            it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right,
                            it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left,
                            it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left,
                            it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right
                    );
                    // SENSOR CONFIGURATION FRAGMENT IS REFRESHING CASE
                else SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY, false);
            }
            else if(fragment instanceof StartSecondFragment)
                fragmentTransaction.setCustomAnimations(
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right
                );

            fragmentTransaction.replace(it.uniba.dib.sms2324.ecowateringcommon.R.id.mainFrameLayout, fragment);
            if(addToBackStack)
                fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
    }

    private void updateSensorsList() {
        SensorsInfo.updateSensorList(   // AMBIENT TEMPERATURE SENSOR LIST UPDATE
                this, Common.getThisDeviceID(this),
                SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE,
                EcoWateringSensor.getConnectedSensorList(this, SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE)
        );
        SensorsInfo.updateSensorList(   // LIGHT SENSOR LIST UPDATE
                this, Common.getThisDeviceID(this),
                SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT,
                EcoWateringSensor.getConnectedSensorList(this, SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT)
        );
        SensorsInfo.updateSensorList(   // RELATIVE HUMIDITY SENSOR LIST UPDATE
                this, Common.getThisDeviceID(this),
                SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY,
                EcoWateringSensor.getConnectedSensorList(this, SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)
        );
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
        boolean firstStartDialogFlag = SharedPreferencesHelper.readStringFromSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY).equals(Common.NULL_STRING_VALUE);
        // USER MUST GRANT LOCATION PERMISSION MANUALLY CASE
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) &&
                (!firstStartDialogFlag)) {
            dialog.setMessage(getString(R.string.why_grant_location_permission_dialog_message))
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
                    finish();
                }
        ).setCancelable(false);
        dialog.show();
    }

    private void showEnableGpsDialog() {
        isEnableGpsDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.why_use_gps_dialog_title))
                .setMessage(getString(R.string.why_use_gps_dialog_msg))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button), (dialogInterface, i) -> {
                    isEnableGpsDialogVisible = false;
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }).setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    isEnableGpsDialogVisible = false;
                    finish();
                }).setCancelable(false)
                .show();
    }

    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            startActivity(new Intent(this, MainActivity.class));
                            overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        startActivity(new Intent(this, MainActivity.class));
                        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
                        finish();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
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