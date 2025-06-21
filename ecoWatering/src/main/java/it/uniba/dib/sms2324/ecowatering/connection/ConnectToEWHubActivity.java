package it.uniba.dib.sms2324.ecowatering.connection;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowatering.connection.mode.bluetooth.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowatering.connection.mode.wifi.WiFiConnectionFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment;

public class ConnectToEWHubActivity extends AppCompatActivity implements
        it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment.OnConnectionChooserActionCallback,
        OnConnectionFinishCallback {
    private static final String MUST_SHOW_MESSAGE_INTENT_KEY = "MUST_SHOW_MESSAGE";
    private FragmentManager fragmentManager;
    private static boolean isFirstActivity = false;
    private static boolean isWhyUseLocationFirstDialogVisible;
    private static boolean isWhyUseLocationDialogVisible;
    private static boolean isDeviceAlreadyConnectedDialogVisible;
    private static boolean isDeviceConnectedDialogVisible;
    private static boolean isInternetFaultDialogVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(it.uniba.dib.sms2324.ecowateringcommon.R.layout.activity_frame_layout);
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            Log.i(Common.LOG_NORMAL, "ConnectToEWHubActivity -> onCreate()");
            if(getIntent().getBooleanExtra(MainActivity.IS_FIRST_ACTIVITY_INTENT_KEY, false)) {
                isFirstActivity = true;
                if(!SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.LOCATION_PERMISSION_FIRST_DIALOG_FILE_NAME, SharedPreferencesHelper.LOCATION_PERMISSION_FIRST_DIALOG_VALUE_KEY)) {
                    SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.LOCATION_PERMISSION_FIRST_DIALOG_FILE_NAME, SharedPreferencesHelper.LOCATION_PERMISSION_FIRST_DIALOG_VALUE_KEY, true);
                    if(getIntent().getBooleanExtra(MUST_SHOW_MESSAGE_INTENT_KEY, false))
                        showWhyUseLocationFirstDialog();
                    else
                        changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment(Common.CALLED_FROM_DEVICE, isFirstActivity), false);
                }
                else
                    changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment(Common.CALLED_FROM_DEVICE, isFirstActivity), false);
            }
            else
                changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment(Common.CALLED_FROM_DEVICE, isFirstActivity), false);
        }
        else {
            if(isWhyUseLocationFirstDialogVisible) runOnUiThread(this::showWhyUseLocationFirstDialog);
            else if(isWhyUseLocationDialogVisible) runOnUiThread(this::showWhyUseLocationDialog);
            else if(isDeviceAlreadyConnectedDialogVisible) runOnUiThread(this::showDeviceAlreadyConnectedDialog);
            else if(isDeviceConnectedDialogVisible) runOnUiThread(this::showDeviceConnectedDialog);
            else if(isInternetFaultDialogVisible) runOnUiThread(this::showInternetFaultDialog);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NO INTERNET CONNECTION CASE
        if(!HttpHelper.isDeviceConnectedToInternet(this)) {
            showInternetFaultDialog();
        }
    }

    @Override
    public void onModeSelected(String mode) {
        // BLUETOOTH MODE CASE
        if(mode.equals(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH)) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH_CONNECT}, Common.BT_PERMISSION_REQUEST);
            }
            else {
                changeFragment(new BtConnectionFragment(), true);
            }
        }
        // WIFI MODE CASE
        else if(mode.equals(OnConnectionFinishCallback.CONNECTION_MODE_WIFI)) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.NEARBY_WIFI_DEVICES}, Common.WIFI_PERMISSION_REQUEST);
            }
            else {
                changeFragment(new WiFiConnectionFragment(), true);
            }
        }
    }

    @Override
    public void onConnectionChooserBackPressed() {
        if(isFirstActivity) {
            finish();
        }
        else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onConnectionFinish(int resultCode) {
        if(resultCode == OnConnectionFinishCallback.CONNECTION_ERROR_RESULT) {
            runOnUiThread(this::showDeviceNotAvailableDialog);
        }
        else if(resultCode == OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT) {
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
            SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_KEY, true);
            changeFragment(new BtConnectionFragment(), true);
        }
        else if(connectionMode.equals(OnConnectionFinishCallback.CONNECTION_MODE_WIFI)) {
            SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_KEY, true);
            changeFragment(new WiFiConnectionFragment(), true);
        }
    }

    @Override
    public void closeConnection() {
        startActivity(new Intent(this, ConnectToEWHubActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // CALLED IN ConnectionChooserFragment IN onViewCreated()
        if(requestCode == Common.LOCATION_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Intent restartIntent = new Intent(this, MainActivity.class);
                restartIntent.putExtra(MUST_SHOW_MESSAGE_INTENT_KEY, true);
                startActivity(restartIntent);
                finish();
            }
            else {
                startActivity(new Intent(this, ConnectToEWHubActivity.class));
                finish();
            }
        }
        // CALLED IN onModeSelected()
        else if(requestCode == Common.BT_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new BtConnectionFragment(), true);
            }
        }
        // CALLED IN onModeSelected()
        else if (requestCode == Common.WIFI_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                changeFragment(new WiFiConnectionFragment(), true);
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

    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStackFlag) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        boolean needToBeAnimated = false;
        if(fragment instanceof BtConnectionFragment) {
            if(SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_KEY))
                SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.BT_CONNECTION_FRAGMENT_IS_REFRESHING_KEY, false);
            else needToBeAnimated = true;
        }
        else if(fragment instanceof WiFiConnectionFragment) {
            if(SharedPreferencesHelper.readBooleanFromSharedPreferences(this, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_KEY))
                SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME, SharedPreferencesHelper.WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_KEY, false);
            else needToBeAnimated = true;
        }
        else if(fragment instanceof ConnectionChooserFragment) needToBeAnimated = true;
        if(needToBeAnimated)
            fragmentTransaction.setCustomAnimations(
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_right,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_left,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left,
                    it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right
            );

        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        if(addToBackStackFlag) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    private void showWhyUseLocationFirstDialog() {
        isWhyUseLocationFirstDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.next_button),
                        (dialogInterface, i) -> {
                            isWhyUseLocationFirstDialogVisible = false;
                            changeFragment(new it.uniba.dib.sms2324.ecowateringcommon.ui.ConnectionChooserFragment(Common.CALLED_FROM_DEVICE, isFirstActivity), false);
                        })
                .setCancelable(false)
                .show();
    }

    /**
     * Explains why app use user location.
     * Positive button to go to setting.
     * Negative button to restart app.
     */
    private void showWhyUseLocationDialog() {
        isWhyUseLocationDialogVisible = true;
        String title = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_title);
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_dialog_message);
        String buttonName = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button);
        DialogInterface.OnClickListener onClickListener = (dialog, i) -> {
            isWhyUseLocationDialogVisible = false;
            startActivity(new Intent(ConnectToEWHubActivity.this, MainActivity.class));
            finish();
        };
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            title = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_address_dialog_post_never_show_title);
            message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.why_use_location_address_dialog_post_never_show_message);
            buttonName = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.go_to_settings_button);
            onClickListener = (dialogInterface, i) -> {
                isWhyUseLocationDialogVisible = false;
                SharedPreferencesHelper.writeBooleanOnSharedPreferences(this, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY, true);
                Intent goToSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                goToSettingIntent.setData(uri);
                startActivity(goToSettingIntent);
            };
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonName, onClickListener)
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isWhyUseLocationDialogVisible = false;
                            if(!isFirstActivity) {
                                startActivity(new Intent(this, MainActivity.class));
                            }
                            finish();
                        }))
                .setCancelable(false);
        dialog.show();
    }

    private void showDeviceNotAvailableDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> runOnUiThread(this::closeConnection))
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user, device is already connected.
     * Positive button to callback with result.
     */
    private void showDeviceAlreadyConnectedDialog() {
        isDeviceAlreadyConnectedDialogVisible = true;
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_already_added_title))
                .setMessage(getString(R.string.remote_device_already_added_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDeviceAlreadyConnectedDialogVisible = false;
                            startActivity(new Intent(this, ConnectToEWHubActivity.class));
                            finish();
                        }))
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user, device is successfully connected.
     * Positive button to callback with result.
     */
    private void showDeviceConnectedDialog() {
        isDeviceConnectedDialogVisible = true;
        android.app.AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_device_added_success_title))
                .setMessage(getString(R.string.remote_device_added_success_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDeviceConnectedDialogVisible = false;
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
    private void showInternetFaultDialog() {
        isInternetFaultDialogVisible = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isInternetFaultDialogVisible = false;
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isInternetFaultDialogVisible = false;
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
