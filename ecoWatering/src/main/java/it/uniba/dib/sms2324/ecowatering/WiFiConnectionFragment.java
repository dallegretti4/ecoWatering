package it.uniba.dib.sms2324.ecowatering;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.hubconnectionthreads.WiFiConnectionRequestThread;
import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class WiFiConnectionFragment extends Fragment {
    protected static WifiP2pManager wifiP2pManager;
    protected static WifiP2pManager.Channel channel;
    protected static OnGpsEnabledCallback onGpsEnabledCallback;

    protected interface OnGpsEnabledCallback {
        void onGpsEnabled(int resultCode);
    }

    private interface OnGroupCreatedCallback {
        void onGroupCreated(int resultCode);
    }

    protected static OnWiFiConnectionFinishCallback onWiFiConnectionFinishCallback;

    protected interface OnWiFiConnectionFinishCallback {
        void onWiFiFinish(int wifiResultCode);
    }

    public interface OnWiFiConnectionResponseGivenCallback {
        void getResponse(String response);
    }

    public WiFiConnectionFragment() {
        super(R.layout.fragment_wifi_connection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnWiFiConnectionFinishCallback) {
            onWiFiConnectionFinishCallback = (OnWiFiConnectionFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onWiFiConnectionFinishCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            ((AppCompatActivity) requireActivity()).addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.menu_manage_eco_watering_hub, menu);
                }

                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemId = menuItem.getItemId();
                    if(itemId == android.R.id.home) {
                        ConnectToEWHubActivity.popBackStatFragment();
                    }
                    else if(itemId == R.id.refreshEWHubItem) {
                        onWiFiConnectionFinishCallback.onWiFiFinish(Common.WIFI_RESTART_FRAGMENT_RESULT);
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        wifiP2pManager = (WifiP2pManager) requireActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(requireContext(), requireContext().getMainLooper(), null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // WIFI NOT ENABLED CASE
        if (!wifiManager.isWifiEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                wifiManager.setWifiEnabled(true);
            } else {
                showEnableWiFiDialog();
            }
        }
        // WIFI ENABLED CASE
        else {
            enableGPS(onGpsEnabledCallback = (result) -> {
                if (result == Common.GPS_ENABLED_RESULT) {
                    // MAKE SURE THERE AREN'T WIFI DIRECT GROUPS
                    wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(Common.THIS_LOG, "group removed");
                        }
                        @Override
                        public void onFailure(int i) {
                            Log.i(Common.THIS_LOG, "group not removed");
                        }
                    });
                    // CREATE WIFI DIRECT GROUP
                    createGroup((resultCode) -> {
                        if(resultCode == Common.WIFI_GROUP_CREATED_SUCCESS_RESULT) {
                            new WiFiConnectionRequestThread(requireContext(), (response) -> {
                                if(response != null) {
                                    manageResponse(response);
                                }
                            }).start();
                        }
                    });
                }
                else {
                    Log.i(Common.THIS_LOG, "gps permission not granted");
                    ConnectToEWHubActivity.popBackStatFragment();
                }
            });
        }
    }

    /**
     * {@code @param:}
     *  OnGpsEnabledCallback callback;
     * If GPS is not enabled, requests to enable GPS
     */
    private void enableGPS(WiFiConnectionFragment.OnGpsEnabledCallback callback) {
        com.google.android.gms.location.LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(requireActivity());
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    Log.i(Common.THIS_LOG, "GPS already enabled");
                    callback.onGpsEnabled(Common.GPS_ENABLED_RESULT);
                })
                .addOnFailureListener(e -> {
                    if(e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(requireActivity(), Common.GPS_ENABLE_REQUEST);
                        }
                        catch (IntentSender.SendIntentException sendEx) {
                            sendEx.printStackTrace();
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void createGroup(OnGroupCreatedCallback callback) {
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onGroupCreated(Common.WIFI_GROUP_CREATED_SUCCESS_RESULT);
            }
            @Override
            public void onFailure(int i) {
                Log.i(Common.THIS_LOG, "Error: group not created. i: " + i);
                callback.onGroupCreated(Common.WIFI_GROUP_CREATED_FAILURE_RESULT);
            }
        });
    }

    private void manageResponse(@NonNull String response) {
        switch (response) {
            case Common.WIFI_ERROR_RESPONSE:
                onWiFiConnectionFinishCallback.onWiFiFinish(Common.WIFI_ERROR_RESULT);
                break;
            case Common.WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE:
                onWiFiConnectionFinishCallback.onWiFiFinish(Common.WIFI_ALREADY_CONNECTED_DEVICE_RESULT);
                break;
            case Common.WIFI_CONNECTED_RESPONSE:
                onWiFiConnectionFinishCallback.onWiFiFinish(Common.WIFI_CONNECTED_RESULT);
                break;
            default:
                break;
        }
    }

    private void showEnableWiFiDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.user_must_enable_wifi_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.user_must_enable_wifi_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.go_to_settings_button),
                        ((dialogInterface, i) -> {
                            Intent wifiSettingIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            startActivity(wifiSettingIntent);
                        }))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> ConnectToEWHubActivity.popBackStatFragment()))
                .setCancelable(false)
                .show();
    }
}
