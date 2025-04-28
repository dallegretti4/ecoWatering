package it.uniba.dib.sms2324.ecowateringhub.connection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.wifi.WiFiConnectionFragment;

public class ManageRemoteEWDevicesConnectedActivity extends AppCompatActivity implements
        ManageRemoteEWDevicesConnectedFragment.OnRemoteDeviceActionSelectedCallback,
        ConnectionChooserFragment.OnConnectionModeSelectedCallback,
        OnConnectionFinishCallback {
    protected static final int ACTION_ADD_REMOTE_DEVICE = 1027;
    protected static final int BT_PERMISSION_REQUEST = 2001;
    private static FragmentManager fragmentManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_remote_ew_devices_connected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Common.lockLayout(this);
        // NO INTERNET CONNECTION CASE
        if(!HttpHelper.isDeviceConnectedToInternet(this)) {
            showInternetFaultDialog();
        }
        else {
            fragmentManager = getSupportFragmentManager();
            changeFragment(new ManageRemoteEWDevicesConnectedFragment(), false);
        }
    }

    @Override
    public void onRemoteDeviceActionSelected(int action) {
        if(action == Common.REFRESH_FRAGMENT) {
            changeFragment(new ManageRemoteEWDevicesConnectedFragment(), false);
        }
        else if(action == ACTION_ADD_REMOTE_DEVICE) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.LOCATION_PERMISSION_REQUEST);
                }
                else {
                    showUserMustGrantLocationPermissionManuallyDialog();
                }
            }
            else {
                changeFragment(new ConnectionChooserFragment(), true);
            }
        }
        else if(action == Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED) {
            fragmentManager.popBackStack();
            changeFragment(new ManageRemoteEWDevicesConnectedFragment(), true);
        }
    }

    @Override
    public void onModeSelected(Fragment fragment) {
        if(fragment == null) {
            fragmentManager.popBackStack();
        }
        else {
            changeFragment(fragment, true);
        }
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
        // CALLED IN ManageRemoteEWDevicesConnectedActivity IN onRemoteDeviceActionSelected()
        if(requestCode == Common.LOCATION_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new ConnectionChooserFragment(), false);
            }
        }
        // REQUESTED IN ConnectionChooserFragment IN onViewCreated()
        else if(requestCode == BT_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new BtConnectionFragment(), false);
            }
        }
        // REQUESTED IN ConnectionChooserFragment IN onViewCreated()
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

    private void showUserMustGrantLocationPermissionManuallyDialog() {
        String message = getString(R.string.why_use_location_dialog_message) + ".\n\n" + getString(R.string.user_must_enable_location_permission_message);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.user_must_enable_location_permission_title))
                .setMessage(message)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.go_to_settings_button),
                        (dialogInterface, i) -> Common.openAppDetailsSetting(this))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    /**
     * Notify the user, device is already connected.
     * Positive button dismiss dialog.
     */
    private void showDeviceAlreadyConnectedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_already_exists_title))
                .setMessage(getString(R.string.remote_device_already_exists_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss())
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
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_added_title))
                .setMessage(getString(R.string.remote_device_added_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            fragmentManager.popBackStack();
                            fragmentManager.popBackStack();
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
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> EcoWateringHub.getEcoWateringHubJsonString(
                                Common.getThisDeviceID(this),
                                (jsonResponse) -> {
                                    Common.restartApp(this);
                                    finish();
                                }
                        ))
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
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            Common.restartApp(this);
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
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
