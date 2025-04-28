package it.uniba.dib.sms2324.ecowatering;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import it.uniba.dib.sms2324.ecowatering.connection.ConnectToEWHubActivity;
import it.uniba.dib.sms2324.ecowatering.entry.MainFragment;
import it.uniba.dib.sms2324.ecowatering.entry.UserProfileFragment;
import it.uniba.dib.sms2324.ecowatering.setup.StartFirstFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;

public class MainActivity extends AppCompatActivity implements
        StartFirstFragment.OnFirstStartFinishCallback,
        MainFragment.OnMainFragmentActionCallback,
        UserProfileFragment.OnUserProfileActionCallback {
    public static EcoWateringDevice thisEcoWateringDevice;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Common.lockLayout(this);
        fragmentManager = getSupportFragmentManager();
        // LOADING FRAGMENT
        changeFragment(new LoadingFragment(), false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // NO INTERNET CONNECTION CASE
        if(!HttpHelper.isDeviceConnectedToInternet(this)) {
            showInternetFaultDialog();
        }
        else {
            Fragment tmpFragment = fragmentManager.findFragmentById(R.id.mainFrameLayout);
            // CONFIGURATION CHANGE FROM MainFragment CASE
            if(tmpFragment instanceof MainFragment) {
                changeFragment(tmpFragment, false);
            }
            else {
                EcoWateringDevice.exists(Common.getThisDeviceID(this), (existsResponse) -> {
                    // NOT FIRST START CASE
                    if(existsResponse.equals(HttpHelper.HTTP_RESPONSE_EXISTS_TRUE)) {
                        EcoWateringDevice.getEcoWateringDeviceJsonString(Common.getThisDeviceID(this), (jsonResponse) -> {
                            if(jsonResponse != null) {
                                thisEcoWateringDevice = new EcoWateringDevice(jsonResponse);
                                // NEVER CONNECTED TO A ECO WATERING HUB CASE
                                if(thisEcoWateringDevice.getEcoWateringHubList() == null || thisEcoWateringDevice.getEcoWateringHubList().isEmpty()) {
                                    ConnectToEWHubActivity.setIsFirstActivity(true);
                                    startActivity(new Intent(this, ConnectToEWHubActivity.class));
                                    finish();
                                }
                                // ALREADY CONNECTED TO ONE HUB AT LEAST CASE
                                else {
                                    changeFragment(new MainFragment(), false);
                                }
                            }
                            else {
                                showHttpErrorFaultDialog();
                            }
                        });
                    }
                    // FIRST START CASE
                    else {
                        changeFragment(new StartFirstFragment(), false);
                    }
                });
            }
        }
    }

    // FROM StartFirstFragment
    /**
     * {@code @param:}
     *  String deviceName -> to name the ecoWateringDevice;
     * To create the ecoWatering Device.
     */
    @Override
    public void onFinish(String deviceName) {
        if(deviceName == null || deviceName.equals("")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        else {
            EcoWateringDevice.addNewEcoWateringDevice(this, deviceName, () -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });

        }
    }

    // FROM MainFragment
    @Override
    public void onMainFragmentUserProfileChosen() {
        changeFragment(new UserProfileFragment(), true);
    }

    // FROM UserProfileFragment
    @Override
    public void onUserProfileGoBack() {
        popToBackStackFragment();
    }

    @Override
    public void onUserProfileRefresh() {
        popToBackStackFragment();
        changeFragment(new UserProfileFragment(), true);
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Fragment fragment;
     *  boolean addToBackStackFlag -> true, to add the transaction to the backstack.
     *  To change the fragment.
     */
    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStackFlag) {
        if(fragment instanceof MainFragment) {
            Common.unlockLayout(this);
        }
        else {
            Common.lockLayout(this);
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        if(addToBackStackFlag) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    private void popToBackStackFragment() {
        fragmentManager.popBackStack();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user there isn't internet connection.
     * Positive button restarts the app.
     */
    protected void showInternetFaultDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.internet_connection_fault_msg))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }))
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) finish();
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
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
                    if(keyCode == KeyEvent.KEYCODE_BACK) finish();
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}