package it.uniba.dib.sms2324.ecowatering;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.HttpHelper;

public class ManageEWHubActivity extends AppCompatActivity implements
        ManageEWHubManualControlFragment.OnUserActionCallback,
        CreateNewEWHubConfigurationFragment.OnEWHubConfigurationAgreedCallback,
        ManageRemoteEWDevicesConnectedFragment.OnRemoteDeviceConnectedActionCallback,
        ManageEWHubManualConfigurationFragment.OnConfigurationUserActionCallback {
    protected static EcoWateringHub selectedEWHub;
    private static FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Common.unlockLayout(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eco_watering_hub);
        Bundle b = getIntent().getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
        selectedEWHub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
        Log.i(Common.THIS_LOG, "selectedHub: " + selectedEWHub);
        if(selectedEWHub != null) {
            // NO INTERNET CONNECTION CASE
            if(!HttpHelper.isDeviceConnectedToInternet(this)) {
                showInternetFaultDialog(this);
            }
            else {
                fragmentManager = getSupportFragmentManager();
                Common.lockLayout(this);
                if(selectedEWHub.getEcoWateringHubConfiguration().isAutomated()) {
                    changeFragment(new ManageEWHubAutomatedControlFragment(), false);
                }
                else {
                    changeFragment(new ManageEWHubManualControlFragment(), false);
                }
            }
        }
        // ERROR CASE
        else {
            showHttpErrorFaultDialog(this);
        }
    }

    @Override
    public void onAutomateIrrigationSystem() {
        changeFragment(new CreateNewEWHubConfigurationFragment(), true);
    }

    @Override
    public void onEWHubConfigurationAgreed(EcoWateringHub hub) {
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, hub);
        Intent automatedControlRestartIntent = new Intent(this, ManageEWHubActivity.class);
        automatedControlRestartIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(automatedControlRestartIntent);
        finish();
    }

    @Override
    public void onRemoteEWDevicesConnectedCardListener() {
        changeFragment(new ManageRemoteEWDevicesConnectedFragment(), true);
    }

    @Override
    public void onRefreshMenuItem() {
        EcoWateringHub.getEcoWateringHubJsonString(selectedEWHub.getDeviceID(), (jsonResponse) -> {
            selectedEWHub = new EcoWateringHub(jsonResponse);
            if(selectedEWHub.getEcoWateringHubConfiguration().isAutomated()) {
                changeFragment(new ManageEWHubAutomatedControlFragment(), false);
            }
            else {
                changeFragment(new ManageEWHubManualControlFragment(), false);
            }
        });
    }

    @Override
    public void onRemoteDeviceConnectedAction(int result) {
        if(result == Common.ACTION_REMOTE_DEVICES_CONNECTED_RESTART_FRAGMENT) {
            popBackStackFragment();
            changeFragment(new ManageRemoteEWDevicesConnectedFragment(), true);
        }
        else if(result == Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED) {
            popBackStackFragment();
        }
        else if(result == Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_IT_SELF_REMOVED) {
            Common.restartApp(this);
            finish();
        }
    }

    @Override
    public void onRefreshConfigurationFragment() {
        EcoWateringHub.getEcoWateringHubJsonString(ManageEWHubActivity.selectedEWHub.getDeviceID(), (jsonResponse) -> {
            ManageEWHubActivity.selectedEWHub = new EcoWateringHub(jsonResponse);
            popBackStackFragment();
            changeFragment(new ManageEWHubManualConfigurationFragment(), true);
        });
    }

    protected static void popBackStackFragment() {
        fragmentManager.popBackStack();
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
    protected void showHttpErrorFaultDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
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
