package it.uniba.dib.sms2324.ecowateringhub.connection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.wifi.WiFiConnectionFragment;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;

public class ManageEcoWateringDevicesConnectionActivity extends AppCompatActivity implements
        it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment.OnConnectionChooserActionCallback,
        OnConnectionFinishCallback {
    private static FragmentManager fragmentManager;
    private static boolean isDeviceAlreadyConnectedDialogVisible;
    private static boolean isDeviceConnectedSuccessfullyVisible;
    private static boolean isInternetFaultDialog;
    private static boolean isErrorDialogVisible;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_connected_remote_ew_devices);
        fragmentManager = getSupportFragmentManager();

        if(savedInstanceState == null) {    // NOT CONFIGURATION CHANGED CASE
            changeFragment(new ManageConnectedRemoteEWDevicesFragment(Common.getThisDeviceID(this), Common.CALLED_FROM_HUB), false);
        }
        else {  // CONFIGURATION CHANGED CASE
            if(isInternetFaultDialog) runOnUiThread(this::showInternetFaultDialog);
            else if(isErrorDialogVisible) runOnUiThread(this::showErrorDialog);
            else if(isDeviceConnectedSuccessfullyVisible) runOnUiThread(this::showDeviceConnectedSuccessfully);
            else if(isDeviceAlreadyConnectedDialogVisible) runOnUiThread(this::showDeviceAlreadyConnectedDialog);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!HttpHelper.isDeviceConnectedToInternet(this)) { // CHECK INTERNET CONNECTION
            showInternetFaultDialog();
        }
    }

    @Override
    public void onManageConnectedDevicesGoBack() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onManageConnectedDevicesRefresh() {
        // CALLED ALSO AFTER LAST DEVICE HAS BEEN REMOVED
        EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(this), (jsonResponse) -> {
            MainActivity.setThisEcoWateringHub(new EcoWateringHub(jsonResponse));
            // ECO WATERING FOREGROUND SERVICE NEED TO BE STOPPED
            if(((MainActivity.getThisEcoWateringHub().getRemoteDeviceList() == null) || MainActivity.getThisEcoWateringHub().getRemoteDeviceList().isEmpty()) &&
                    (!MainActivity.getThisEcoWateringHub().isDataObjectRefreshing())) {
                EcoWateringForegroundService.stopEcoWateringForegroundService(this);
            }
            // REFRESH FRAGMENT
            startActivity(new Intent(this, ManageEcoWateringDevicesConnectionActivity.class));
            finish();
        });
    }

    @Override
    public void addNewRemoteDevice() {
        changeFragment(new ConnectionChooserFragment(Common.CALLED_FROM_HUB, false), true);
    }

    @Override
    public void onModeSelected(String mode) {
        // BLUETOOTH MODE CASE
        if(mode.equals(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH)) {
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
                    ((ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        Common.BT_PERMISSION_REQUEST
                );
            }
            else {
                changeFragment(new BtConnectionFragment(), true);
            }
        }
        // WIFI MODE CASE
        else if(mode.equals(OnConnectionFinishCallback.CONNECTION_MODE_WIFI)) {
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.NEARBY_WIFI_DEVICES}, Common.WIFI_PERMISSION_REQUEST);
            }
            else {
                changeFragment(new WiFiConnectionFragment(), true);
            }
        }
    }

    @Override
    public void onConnectionChooserBackPressed() {
        startActivity(new Intent(this, ManageEcoWateringDevicesConnectionActivity.class));
        finish();
    }

    @Override
    public void onConnectionFinish(int resultCode) {
        switch (resultCode) {
            case Common.ACTION_BACK_PRESSED:
                fragmentManager.popBackStack();
                break;
            case OnConnectionFinishCallback.CONNECTION_ERROR_RESULT:
                runOnUiThread(this::showErrorDialog);
                break;
            case OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT:
                runOnUiThread(this::showDeviceAlreadyConnectedDialog);
                break;
            case OnConnectionFinishCallback.CONNECTION_CONNECTED_DEVICE_RESULT:
                runOnUiThread(this::showDeviceConnectedSuccessfully);
                break;
            default:
                break;
        }
    }

    @Override
    public void restartFragment(String connectionMode) {
        fragmentManager.popBackStack();
        if(connectionMode.equals(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH)) {
            changeFragment(new BtConnectionFragment(), true);
        }
        else if(connectionMode.equals(OnConnectionFinishCallback.CONNECTION_MODE_WIFI)) {
            changeFragment(new WiFiConnectionFragment(), true);
        }
        else {
            runOnUiThread(this::showErrorDialog);
        }
    }

    @Override
    public void closeConnection() {
        fragmentManager.popBackStack();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // REQUESTED IN ConnectionChooserFragment IN onViewCreated()
        if(requestCode == Common.LOCATION_PERMISSION_REQUEST) {
            if(!SharedPreferencesHelper.readBooleanOnSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY)) {
                SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.FIRST_START_FLAG_FILE_NAME, SharedPreferencesHelper.FIRST_START_FLAG_VALUE_KEY, true);
            }
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                fragmentManager.popBackStack();
            }
        }
        // REQUESTED IN ManageRemoteEWDevicesConnectedActivity IN onModeSelected()
        else if(requestCode == Common.BT_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new BtConnectionFragment(), false);
            }
        }
        // REQUESTED IN ManageRemoteEWDevicesConnectedActivity IN onModeSelected()
        else if(requestCode == Common.WIFI_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new WiFiConnectionFragment(), false);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Common.GPS_WIFI_ENABLE_REQUEST) {
            fragmentManager.popBackStack();
            if(resultCode == RESULT_OK) {
                changeFragment(new WiFiConnectionFragment(), true);
            }
        }
        else if(requestCode == Common.GPS_BT_ENABLE_REQUEST) {
            fragmentManager.popBackStack();
            if(resultCode == RESULT_OK) {
                changeFragment(new BtConnectionFragment(), true);
            }
        }
    }

    private void changeFragment(Fragment fragment, boolean addToBackStackFlag) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        if(addToBackStackFlag) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    /**
     * Notify the user, device is already connected.
     * Positive button dismiss dialog.
     */
    private void showDeviceAlreadyConnectedDialog() {
        isDeviceAlreadyConnectedDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_already_exists_title))
                .setMessage(getString(R.string.remote_device_already_exists_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDeviceAlreadyConnectedDialogVisible = false;
                            dialogInterface.dismiss();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * Notify the user, device is successfully connected.
     * Positive button restarts the MainFragment.
     */
    private void showDeviceConnectedSuccessfully() {
        isDeviceConnectedSuccessfullyVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_added_title))
                .setMessage(getString(R.string.remote_device_added_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDeviceConnectedSuccessfullyVisible = false;
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                )
                .setCancelable(false)
                .create().show();
    }

    /**
     * Notify the user that something went wrong.
     * Positive button dismiss dialog.
     */
    private void showErrorDialog() {
        isErrorDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isErrorDialogVisible = false;
                            EcoWateringHub.getEcoWateringHubJsonString(
                                    Common.getThisDeviceID(this),
                                    (jsonResponse) -> {
                                        Common.restartApp(this);
                                        finish();
                                    });
                        })
                )
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
                            Common.restartApp(this);
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isInternetFaultDialog = false;
                        Common.restartApp(this);
                        finish();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
