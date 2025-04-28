package it.uniba.dib.sms2324.ecowatering.management;

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

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;

public class ManageEWHubActivity extends AppCompatActivity implements
        ManageEWHubManualControlFragment.OnUserActionCallback,
        ManageRemoteEWDevicesConnectedFragment.OnRemoteDeviceConnectedActionCallback {
    protected static final int ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_IT_SELF_REMOVED = 1031;
    private static EcoWateringHub selectedEWHub;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Common.unlockLayout(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eco_watering_hub);
        Bundle b = getIntent().getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
        selectedEWHub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
        Log.i(Common.THIS_LOG, "selectedHub: " + selectedEWHub);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        if((result == Common.ACTION_REMOTE_DEVICES_CONNECTED_RESTART_FRAGMENT) || (result == Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED)) {
            fragmentManager.popBackStack();
            changeFragment(new ManageRemoteEWDevicesConnectedFragment(), true);
        }
        else if(result == Common.ACTION_BACK_PRESSED) {
            fragmentManager.popBackStack();
        }
        else if(result == ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_IT_SELF_REMOVED) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void changeFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.manageEWHubFrameLayout, fragment);
        if(addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    protected static EcoWateringHub getSelectedEWHub() {
        return selectedEWHub;
    }

    protected static void setSelectedEWHub(@NonNull EcoWateringHub ecoWateringHub) {
        selectedEWHub = ecoWateringHub;
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    private void showHttpErrorFaultDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
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
