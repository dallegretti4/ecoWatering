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

import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.AutomateSystemFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageHubAutomaticControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageHubManualControlFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.SensorConfigurationFragment;

public class ManageEWHubActivity extends AppCompatActivity implements
        ManageHubManualControlFragment.OnHubManualActionChosenCallback,
        ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback,
        SensorConfigurationFragment.OnSensorConfigurationActionCallback,
        AutomateSystemFragment.OnAutomateSystemActionCallback {
    private static EcoWateringHub selectedEWHub;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eco_watering_hub);
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            Bundle b = getIntent().getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
            selectedEWHub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
            if(selectedEWHub != null) {
                if(selectedEWHub.isAutomated()) {
                    changeFragment(new ManageHubAutomaticControlFragment(), false);
                }
                else {
                    changeFragment(new ManageHubManualControlFragment(Common.CALLED_FROM_DEVICE, selectedEWHub), false);
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


    // FROM ManageHubManualControlFragment.OnHubActionChosenCallback
    @Override
    public void onBackPressedFromManageHubManual() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void refreshManageHubManualFragment() {
        EcoWateringHub.getEcoWateringHubJsonString(selectedEWHub.getDeviceID(), (jsonResponse) -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            if(selectedEWHub.isAutomated()) {
                changeFragment(new ManageHubAutomaticControlFragment(), false);
            }
            else {
                changeFragment(new ManageHubManualControlFragment(Common.CALLED_FROM_DEVICE, selectedEWHub), false);
            }
        });
    }

    @Override
    public void onManualSecondToolbarFunctionChosen() {
        // GO BACK
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void manageConnectedRemoteDevices() {
        changeFragment(new ManageConnectedRemoteEWDevicesFragment(selectedEWHub.getDeviceID(), Common.CALLED_FROM_DEVICE), true);
    }

    @Override
    public void automateEcoWateringSystem() {
        fragmentManager.popBackStack();
        EcoWateringHub.getEcoWateringHubJsonString(selectedEWHub.getDeviceID(), (jsonResponse) -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            changeFragment(new AutomateSystemFragment(selectedEWHub, Common.CALLED_FROM_DEVICE), true);
        });
    }

    @Override
    public void setDataObjectRefreshing(boolean value) {
        if(value) DeviceRequest.sendRequest(selectedEWHub.getDeviceID(), Common.getThisDeviceID(this), DeviceRequest.REQUEST_START_DATA_OBJECT_REFRESHING);
        else DeviceRequest.sendRequest(selectedEWHub.getDeviceID(), Common.getThisDeviceID(this), DeviceRequest.REQUEST_STOP_DATA_OBJECT_REFRESHING);
    }

    @Override
    public void setIrrigationSystemState(boolean value) {
        if(value) DeviceRequest.sendRequest(selectedEWHub.getDeviceID(), Common.getThisDeviceID(this), DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM);
        else DeviceRequest.sendRequest(selectedEWHub.getDeviceID(), Common.getThisDeviceID(this), DeviceRequest.REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM);
    }

    @Override
    public void configureSensor(String sensorType) {
        EcoWateringHub.getEcoWateringHubJsonString(selectedEWHub.getDeviceID(), (jsonResponse) -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            changeFragment(new SensorConfigurationFragment(selectedEWHub, Common.CALLED_FROM_DEVICE, sensorType), true);
        });
    }

    @Override
    public void forceSensorsUpdate() {}

    @Override
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onManageConnectedDevicesGoBack() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent goBackIntent = new Intent(this, ManageEWHubActivity.class);
        goBackIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, bundle);
        startActivity(goBackIntent);
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
        finish();
    }
    @Override
    public void onSensorConfigurationRefreshFragment(String sensorType) {
        EcoWateringHub.getEcoWateringHubJsonString(selectedEWHub.getDeviceID(), (jsonResponse) -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(selectedEWHub, Common.CALLED_FROM_DEVICE, sensorType), true);
        });
    }
    @Override
    public void onSensorConfigurationRestartApp() {
        startActivity(new Intent(this, MainActivity.class));
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
        finish();
    }

    @Override
    public void onAutomateSystemFinish() {
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent restartIntent = new Intent(this, ManageEWHubActivity.class);
        restartIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(restartIntent);
        finish();
    }

    private void changeFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.manageEWHubFrameLayout, fragment);
        if(addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
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
