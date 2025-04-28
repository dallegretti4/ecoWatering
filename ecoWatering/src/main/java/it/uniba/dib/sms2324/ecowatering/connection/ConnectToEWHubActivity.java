package it.uniba.dib.sms2324.ecowatering.connection;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowatering.connection.ui.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowatering.connection.ui.ConnectionChooserFragment;
import it.uniba.dib.sms2324.ecowatering.connection.ui.WiFiConnectionFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;

public class ConnectToEWHubActivity extends AppCompatActivity implements
        ConnectionChooserFragment.OnConnectionModeSelectedCallback,
        OnConnectionFinishCallback {
    public static final int FIRST_BT_CONNECT_PERMISSION_REQUEST = 1016;
    private static final int FIRST_LOCATION_PERMISSION_REQUEST = 1015;
    public static final int FIRST_WIFI_PERMISSION_REQUEST = 1017;
    private static FragmentManager fragmentManager;
    private static boolean isFirstActivity = false;

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
    public void onConnectionFinish(int resultCode) {
        if(resultCode == OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT) {
            runOnUiThread(this::showDeviceAlreadyConnectedDialog);
        }
        else if(resultCode == OnConnectionFinishCallback.CONNECTION_CONNECTED_DEVICE_RESULT) {
            runOnUiThread(this::showDeviceConnectedDialog);
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
    }

    @Override
    public void closeConnection() {
        fragmentManager.popBackStack();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // CALLED IN ConnectToEWHubActivity IN onStart()
        if(requestCode == FIRST_LOCATION_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new ConnectionChooserFragment(), false);
            }
            else {
                showWhyUseLocationDialog();
            }
        }
        // CALLED IN ConnectionChooserFragment
        else if(requestCode == FIRST_BT_CONNECT_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onModeSelected(new BtConnectionFragment());
            }
        }
        // CALLED IN ConnectionChooserFragment
        else if (requestCode == FIRST_WIFI_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onModeSelected(new WiFiConnectionFragment());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Common.GPS_ENABLE_REQUEST) {
            fragmentManager.popBackStack();
            if(resultCode == Activity.RESULT_OK) {
                changeFragment(new WiFiConnectionFragment(), true);
            }
        }
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

    public static boolean isFirstActivity() {
        return isFirstActivity;
    }

    public static void setIsFirstActivity(boolean value) {
        isFirstActivity = value;
    }

    private void showWhyUseLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.next_button),
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(
                                this,
                                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                FIRST_LOCATION_PERMISSION_REQUEST
                        ))
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
                            startActivity(new Intent(this, MainActivity.class));
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
