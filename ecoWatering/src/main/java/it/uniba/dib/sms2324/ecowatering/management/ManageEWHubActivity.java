package it.uniba.dib.sms2324.ecowatering.management;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Calendar;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlanPreview;
import it.uniba.dib.sms2324.ecowateringcommon.ui.AutomateSystemFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.hub.ManageHubAutomaticControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.hub.ManageHubManualControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.SensorConfigurationFragment;

public class ManageEWHubActivity extends AppCompatActivity implements
        ManageHubManualControlFragment.OnHubManualActionChosenCallback,
        ManageHubAutomaticControlFragment.OnManageHubAutomaticControlActionCallback,
        ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback,
        SensorConfigurationFragment.OnSensorConfigurationActionCallback,
        AutomateSystemFragment.OnAutomateSystemActionCallback {

    private static final long WAITING_TIME_AFTER_AUTOMATE_SYSTEM_DEVICE_REQUEST = 2 * 1000;
    private static EcoWateringHub selectedEWHub;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(it.uniba.dib.sms2324.ecowateringcommon.R.layout.activity_frame_layout);
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            Bundle b = getIntent().getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
            selectedEWHub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
            if(selectedEWHub != null) {
                if(selectedEWHub.isAutomated()) {
                    changeFragment(new ManageHubAutomaticControlFragment(Common.CALLED_FROM_DEVICE, selectedEWHub), true);
                }
                else {
                    changeFragment(new ManageHubManualControlFragment(Common.CALLED_FROM_DEVICE, selectedEWHub), true);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(selectedEWHub == null) { // ERROR CASE
            showHttpErrorFaultDialog();
        }
        if(!HttpHelper.isDeviceConnectedToInternet(this)) { // NO INTERNET CONNECTION CASE
            showInternetFaultDialog(this);
        }
    }


    // FROM ManageHubFragment.OnHubActionChosenCallback
    @Override
    public void onManageHubBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onManageHubRefreshFragment() {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            Bundle b = new Bundle();
            b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
            Intent refreshIntent = new Intent(this, ManageEWHubActivity.class);
            refreshIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
            startActivity(refreshIntent);
            finish();
        }));
    }

    @Override
    public void onManualSecondToolbarFunctionChosen() {
        // GO BACK
        onManageHubBackPressed();
    }

    @Override
    public void refreshDataObject(EcoWateringHub.OnEcoWateringHubGivenCallback callback) {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            callback.getEcoWateringHub(selectedEWHub);
        }));
    }

    @Override
    public void manageConnectedRemoteDevices() {
        fragmentManager.popBackStack();
        changeFragment(new ManageConnectedRemoteEWDevicesFragment(selectedEWHub.getDeviceID(), Common.CALLED_FROM_DEVICE), true);
    }

    @Override
    public void configureSensor(String sensorType) {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(selectedEWHub, Common.CALLED_FROM_DEVICE, sensorType), true);
        }));
    }

    @Override   // CALLED FROM AutomateSystemFragment.OnAutomateSystemActionCallback TOO
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    //  CALLED FROM ManageHubManualControlFragment.OnHubManualActionChosenCallback

    @Override
    public void automateEcoWateringSystem() {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new AutomateSystemFragment(selectedEWHub, Common.CALLED_FROM_DEVICE), true);
        }));
    }

    @Override
    public void setIrrigationSystemState(boolean value) {
        if(value) DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM);
        else DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM);
    }

    @Override
    public void setDataObjectRefreshing(boolean value) {
        if(value) DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_START_DATA_OBJECT_REFRESHING);
        else DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_STOP_DATA_OBJECT_REFRESHING);
    }

    @Override
    public void scheduleIrrSys(Calendar calendar, int[] irrigationDuration) {
        StringBuilder requestStringBuilder = new StringBuilder(DeviceRequest.REQUEST_SCHEDULE_IRR_SYS);
        requestStringBuilder.append(DeviceRequest.REQUEST_PARAMETER_DIVISOR).append("{\\\"").append(DeviceRequest.STARTING_DATE_PARAMETER).append("\\\":[");
        if(calendar != null)
            requestStringBuilder.append(calendar.get(Calendar.YEAR)).append(",").append(calendar.get(Calendar.MONTH)).append(",").append(calendar.get(Calendar.DAY_OF_MONTH)).append("],\\\"")
                    .append(DeviceRequest.STARTING_TIME_PARAMETER).append("\\\":[")
                    .append(calendar.get(Calendar.HOUR_OF_DAY)).append(",").append(calendar.get(Calendar.MINUTE)).append("],\\\"")
                    .append(DeviceRequest.IRRIGATION_DURATION_PARAMETER).append("\\\":[")
                    .append(irrigationDuration[0]).append(",").append(irrigationDuration[1]).append("]}");
        else
            requestStringBuilder.append(0).append(",").append(0).append(",").append(0).append("],\\\"")
                    .append(DeviceRequest.STARTING_TIME_PARAMETER).append("\\\":[")
                    .append(0).append(",").append(0).append("],\\\"")
                    .append(DeviceRequest.IRRIGATION_DURATION_PARAMETER).append("\\\":[")
                    .append(0).append(",").append(0).append("]}");
        DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), requestStringBuilder.toString());
    }

    //  CALLED FROM ManageHubAutomaticControlFragment.OnManageHubAutomaticControlActionCallback
    @Override
    public void controlSystemManually() {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_DISABLE_AUTOMATE_SYSTEM);
        }));
    }


    //  CALLED FROM ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback

    @Override
    public void onManageConnectedDevicesGoBack() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent goBackIntent = new Intent(this, ManageEWHubActivity.class);
        goBackIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, bundle);
        startActivity(goBackIntent);
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onManageConnectedDevicesRefresh() {
        fragmentManager.popBackStack();
        changeFragment(new ManageConnectedRemoteEWDevicesFragment(selectedEWHub.getDeviceID(), Common.CALLED_FROM_DEVICE), true);
    }

    @Override
    public void addNewRemoteDevice() {} // NOT USED BY ECO WATERING DEVICE

    // FROM SensorConfigurationFragment.OnSensorConfigurationActionCallback

    @Override
    public void onSensorConfigurationGoBack() {
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent startIntent = new Intent(this, ManageEWHubActivity.class);
        startIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(startIntent);
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }
    @Override
    public void onSensorConfigurationRefreshFragment(String sensorType) {
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(selectedEWHub, Common.CALLED_FROM_DEVICE, sensorType), true);
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
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent restartIntent = new Intent(this, ManageEWHubActivity.class);
        restartIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(restartIntent);
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onAutomateSystemAgreeIrrigationPlan(@NonNull IrrigationPlanPreview irrigationPlanPreview) {
        irrigationPlanPreview.updateIrrigationPlanOnServer(this, selectedEWHub.getDeviceID());
        DeviceRequest.sendRequest(this, selectedEWHub.getDeviceID(), DeviceRequest.REQUEST_ENABLE_AUTOMATE_SYSTEM);
        changeFragment(new LoadingFragment(), false);
        try { Thread.sleep(WAITING_TIME_AFTER_AUTOMATE_SYSTEM_DEVICE_REQUEST); }
        catch (InterruptedException ignored) {}
        EcoWateringHub.getEcoWateringHub(selectedEWHub.getDeviceID(), (jsonResponse -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            Bundle b = new Bundle();
            b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
            Intent restartIntent = new Intent(this, ManageEWHubActivity.class);
            restartIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
            startActivity(restartIntent);
            finish();
        }));
    }

    private void changeFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // INSERT SLIDE ANIMATION ON SensorConfigurationFragment
        if(fragment instanceof SensorConfigurationFragment) { // FOR SensorConfigurationFragment
            if(!SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY))
                fragmentTransaction.setCustomAnimations(
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left,
                        it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right
                );
                // SENSOR CONFIGURATION FRAGMENT IS REFRESHING CASE
            else SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY, false);
        }   // INSERT SLIDE ANIMATION ON ManageConnectedRemoteEWDevicesFragment
        else if (fragment instanceof ManageConnectedRemoteEWDevicesFragment)
            fragmentTransaction.setCustomAnimations(
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right
            );
        if(addToBackStack)
            fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        fragmentTransaction.commit();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    private void showHttpErrorFaultDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user there isn't internet connection.
     * Positive button restarts the app.
     */
    private void showInternetFaultDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
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
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
