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
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowateringhub.connection.mode.wifi.WiFiConnectionFragment;

public class ManageConnectedRemoteEWDevicesActivity extends AppCompatActivity implements
        it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback,
        it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment.OnConnectionChooserActionCallback,
        OnConnectionFinishCallback {
    protected static final int BT_PERMISSION_REQUEST = 2001;
    private static FragmentManager fragmentManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_connected_remote_ew_devices);
        fragmentManager = getSupportFragmentManager();

        if(savedInstanceState == null) {    // NOT CONFIGURATION CHANGED CASE
            changeFragment(new ManageConnectedRemoteEWDevicesFragment(Common.getThisDeviceID(this), Common.CALLED_FROM_HUB), false);
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
        startActivity(new Intent(this, ManageConnectedRemoteEWDevicesActivity.class));
        finish();
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
                        BT_PERMISSION_REQUEST
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
        startActivity(new Intent(this, ManageConnectedRemoteEWDevicesActivity.class));
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
        // CALLED IN ManageRemoteEWDevicesConnectedActivity IN onRemoteDeviceActionSelected()
        if(requestCode == Common.LOCATION_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment(Common.CALLED_FROM_HUB, false), false);
            }
        }
        // REQUESTED IN ManageRemoteEWDevicesConnectedActivity IN onModeSelected()
        else if(requestCode == BT_PERMISSION_REQUEST) {
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
