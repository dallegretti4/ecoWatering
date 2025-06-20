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
import it.uniba.dib.sms2324.ecowatering.service.EcoWateringForegroundDeviceService;
import it.uniba.dib.sms2324.ecowateringcommon.ui.UserProfileFragment;
import it.uniba.dib.sms2324.ecowatering.setup.StartFirstFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.ui.LoadingFragment;

public class MainActivity extends AppCompatActivity implements
        StartFirstFragment.OnFirstStartFinishCallback,
        MainFragment.OnMainFragmentActionCallback,
        UserProfileFragment.OnUserProfileActionCallback {
    public static String IS_FIRST_ACTIVITY_INTENT_KEY = "IS_FIRST_ACTIVITY";
    private static EcoWateringDevice thisEcoWateringDevice;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(it.uniba.dib.sms2324.ecowateringcommon.R.layout.activity_frame_layout);
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            changeFragment(new LoadingFragment(), false);   // LOADING FRAGMENT
            EcoWateringDevice.exists(Common.getThisDeviceID(this), (existsResponse) -> { // CHECK DEVICE IS REGISTERED
                if(!existsResponse.equals(HttpHelper.HTTP_RESPONSE_ERROR)) {
                    // DEVICE ALREADY REGISTERED CASE
                    thisEcoWateringDevice = new EcoWateringDevice(existsResponse);
                    EcoWateringForegroundDeviceService.checkIfServiceNeedToBeStarted(this, thisEcoWateringDevice);
                    if(thisEcoWateringDevice.getEcoWateringHubList() == null || thisEcoWateringDevice.getEcoWateringHubList().isEmpty()) {
                        // NEVER CONNECTED TO A ECO WATERING HUB CASE
                        Intent firstStartIntent = new Intent(this, ConnectToEWHubActivity.class);
                        firstStartIntent.putExtra(IS_FIRST_ACTIVITY_INTENT_KEY, true);
                        startActivity(firstStartIntent);
                        finish();
                    }
                    else    // ALREADY CONNECTED TO ONE HUB AT LEAST CASE
                        changeFragment(new MainFragment(), false);
                }
                else    // DEVICE NEED TO BE REGISTERED CASE
                    changeFragment(new StartFirstFragment(), false);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NO INTERNET CONNECTION CASE
        if(!HttpHelper.isDeviceConnectedToInternet(this))
            showInternetFaultDialog();
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

    // FROM MainFragment.OnMainFragmentActionCallback
    @Override
    public void onMainFragmentUserProfileChosen() {
        changeFragment(new UserProfileFragment(Common.CALLED_FROM_DEVICE), true);
    }

    // FROM UserProfileFragment
    @Override
    public void onUserProfileGoBack() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_in_left, it.uniba.dib.sms2324.ecowateringcommon.R.anim.fragment_transaction_slide_out_right);
        finish();
    }

    @Override
    public void onUserProfileRefresh() {
        fragmentManager.popBackStack();
        changeFragment(new UserProfileFragment(Common.CALLED_FROM_DEVICE), true);
    }

    @Override   // CALLED FROM MainFragment.OnMainFragmentActionCallback TO
    public void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Fragment fragment;
     *  boolean addToBackStackFlag -> true, to add the transaction to the backstack.
     *  To change the fragment.
     */
    private void changeFragment(@NonNull Fragment fragment, boolean addToBackStackFlag) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        if(addToBackStackFlag) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    public static EcoWateringDevice getThisEcoWateringDevice() {
        return thisEcoWateringDevice;
    }

    public static void setThisEcoWateringDevice(@NonNull EcoWateringDevice ecoWateringDevice) {
        thisEcoWateringDevice = ecoWateringDevice;
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
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
}