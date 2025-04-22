package it.uniba.dib.sms2324.ecowatering;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHubComparator;
import it.uniba.dib.sms2324.ecowateringcommon.HttpHelper;

public class ConnectToEWHubActivity extends AppCompatActivity implements
        ConnectionChooserFragment.OnConnectionModeSelectedCallback,
        BtConnectionFragment.OnBtConnectionFinishCallback,
        WiFiConnectionFragment.OnWiFiConnectionFinishCallback {
    private static FragmentManager fragmentManager;
    protected static boolean isFirstActivity = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Common.unlockLayout(this);
        setContentView(R.layout.activity_connect_to_eco_watering_hub);
    }

    @Override
    public void onStart() {
        super.onStart();
        // NO INTERNET CONNECTION CASE
        if(!HttpHelper.isDeviceConnectedToInternet(this)) {
            showInternetFaultDialog(this);
        }
        else {
            fragmentManager = getSupportFragmentManager();
            // CHECK FIRST REQUEST PERMISSION
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showWhyUseLocationPermissionDialog();
            }
            else {
                changeFragment(new ConnectionChooserFragment(), false);
                Common.lockLayout(this);
            }
        }
    }

    @Override
    public void onModeSelected(Fragment fragment) {
        changeFragment(fragment, true);
    }

    @Override
    public void onBtFinish(int btResultCode) {
        if(btResultCode == Common.BT_RESTART_FRAGMENT_RESULT) {
            popBackStatFragment();
            changeFragment(new BtConnectionFragment(), true);
        }
        else if((btResultCode == Common.BT_ALREADY_CONNECTED_RESULT)) {
            runOnUiThread(this::showDeviceAlreadyConnectedDialog);
        }
        else if(btResultCode == Common.BT_CONNECTED_RESULT) {
            runOnUiThread(this::showDeviceConnectedDialog);
        }
    }

    @Override
    public void onWiFiFinish(int wifiResultCode) {
        if(wifiResultCode == Common.WIFI_RESTART_FRAGMENT_RESULT) {
            popBackStatFragment();
            changeFragment(new WiFiConnectionFragment(), true);
        }
        else if(wifiResultCode == Common.WIFI_ALREADY_CONNECTED_DEVICE_RESULT) {
            runOnUiThread(this::showDeviceAlreadyConnectedDialog);
        }
        else if(wifiResultCode == Common.WIFI_CONNECTED_RESULT) {
            runOnUiThread(this::showDeviceConnectedDialog);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // CALLED IN ConnectToEWHubActivity IN onStart()
        if(requestCode == Common.FIRST_LOCATION_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new ConnectionChooserFragment(), false);
            }
            else {
                showWhyUseLocationDialog();
            }
        }
        // CALLED IN ConnectionChooserFragment
        else if(requestCode == Common.FIRST_BT_CONNECT_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onModeSelected(new BtConnectionFragment());
            }
        }
        // CALLED IN ConnectionChooserFragment
        else if (requestCode == Common.FIRST_WIFI_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onModeSelected(new WiFiConnectionFragment());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Common.GPS_ENABLE_REQUEST) {
            if(resultCode == Activity.RESULT_OK) {
                WiFiConnectionFragment.onGpsEnabledCallback.onGpsEnabled(Common.GPS_ENABLED_RESULT);
            }
            else {
                WiFiConnectionFragment.onGpsEnabledCallback.onGpsEnabled(Common.WIFI_ERROR_RESULT);
            }
        }
    }

    protected static void popBackStatFragment() {
        fragmentManager.popBackStack();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Fragment fragment;
     *  boolean addToBackStackFlag -> true, to add the transaction to the backstack.
     *  To change the fragment.
     */
    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStackFlag) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.connectToEcoWateringHubFrameLayout, fragment);
        if(addToBackStackFlag) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    private void showWhyUseLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.next_button),
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.FIRST_LOCATION_PERMISSION_REQUEST))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Explains why app use user location.
     * Positive button to go to setting.
     * Negative button to restart app.
     */
    private void showWhyUseLocationDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_message))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.go_to_settings_button),
                        ((dialogInterface, i) -> {
                            Intent goToSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            goToSettingIntent.setData(uri);
                            startActivity(goToSettingIntent);
                        }))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> finish()))
                .setCancelable(false);
        dialog.show();
    }

    /**
     * Notify the user, device is already connected.
     * Positive button to callback with result.
     */
    private void showDeviceAlreadyConnectedDialog() {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_already_added_title))
                .setMessage(getString(R.string.remote_device_already_added_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(this, ConnectToEWHubActivity.class));
                            finish();
                        }))
                .setCancelable(false);
        dialog.show();
    }

    /**
     * Notify the user, device is successfully connected.
     * Positive button to callback with result.
     */
    private void showDeviceConnectedDialog() {
        android.app.AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_added_success_title))
                .setMessage(getString(R.string.remote_device_added_success_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            if(isFirstActivity) {
                                isFirstActivity = false;
                            }
                            Common.restartApp(this);
                            finish();
                        }))
                .setCancelable(false);
        dialog.show();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user there isn't internet connection.
     * Positive button restarts the app.
     */
    protected void showInternetFaultDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> finish())
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
