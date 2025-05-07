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
import it.uniba.dib.sms2324.ecowateringcommon.ui.ManageConnectedRemoteEWDevicesFragment;

public class ManageEWHubActivity extends AppCompatActivity implements
        ManageEWHubManualControlFragment.OnUserActionCallback,
        ManageConnectedRemoteEWDevicesFragment.OnConnectedRemoteEWDeviceActionCallback {
    private static EcoWateringHub selectedEWHub;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eco_watering_hub);
        Bundle b = getIntent().getBundleExtra(Common.MANAGE_EWH_INTENT_OBJ);
        selectedEWHub = Objects.requireNonNull(b).getParcelable(Common.MANAGE_EWH_INTENT_OBJ);
        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            if(selectedEWHub.getEcoWateringHubConfiguration().isAutomated()) {
                changeFragment(new ManageEWHubAutomatedControlFragment(), false);
            }
            else {
                changeFragment(new ManageEWHubManualControlFragment(), false);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(selectedEWHub == null) { // ERROR CASE
            showHttpErrorFaultDialog(this);
        }
        if(!HttpHelper.isDeviceConnectedToInternet(this)) { // NO INTERNET CONNECTION CASE
            showInternetFaultDialog(this);
        }
    }

    @Override
    public void onAutomateIrrigationSystem() {

    }

    @Override
    public void onRemoteEWDevicesConnectedCardListener() {
        changeFragment(new ManageConnectedRemoteEWDevicesFragment(selectedEWHub.getDeviceID(), Common.CALLED_FROM_DEVICE), true);
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
    public void onManageConnectedDevicesGoBack() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, selectedEWHub);
        Intent goBackIntent = new Intent(this, ManageEWHubActivity.class);
        goBackIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, bundle);
        startActivity(goBackIntent);
        finish();
    }

    @Override
    public void onManageConnectedDevicesRefresh() {
        fragmentManager.popBackStack();
        changeFragment(new ManageConnectedRemoteEWDevicesFragment(selectedEWHub.getDeviceID(), Common.CALLED_FROM_DEVICE), true);
    }

    @Override
    public void addNewRemoteDevice() {} // NOT USED BY ECO WATERING DEVICE

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
