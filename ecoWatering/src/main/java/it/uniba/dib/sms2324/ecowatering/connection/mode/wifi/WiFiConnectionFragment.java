package it.uniba.dib.sms2324.ecowatering.connection.mode.wifi;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;

public class WiFiConnectionFragment extends Fragment {
    private static final int WIFI_GROUP_CREATED_SUCCESS_RESULT = 1013;
    private static final int WIFI_GROUP_CREATED_FAILURE_RESULT = 1014;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private WiFiConnectionRequestThread wifiConnectionThread;
    private OnConnectionFinishCallback onConnectionFinishCallback;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                onConnectionFinishCallback.closeConnection();
            }
            else if(itemId == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_WIFI);
            }
            return false;
        }
    };
    private final WifiP2pManager.ActionListener wifiP2pActionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(Common.LOG_NORMAL, "group removed");
        }
        @Override
        public void onFailure(int i) {
            Log.i(Common.LOG_NORMAL, "group not removed");
        }
    };

    protected final BroadcastReceiver wifiP2pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                if((intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) != WifiManager.WIFI_STATE_ENABLING) &&
                        (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) != WifiManager.WIFI_STATE_ENABLED))
                    requireActivity().runOnUiThread(() -> showWifiDisabledDialog());
            }
        }
    };

    public WiFiConnectionFragment() {
        super(R.layout.fragment_wifi_connection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(Common.LOG_NORMAL, ".................> on Attach");
        if (context instanceof OnConnectionFinishCallback) {
            onConnectionFinishCallback = (OnConnectionFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(Common.LOG_NORMAL, ".................> on Detach");
        onConnectionFinishCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Common.lockLayout(requireActivity());
        toolbarSetup(view);
        // WIFI P2P MANAGER & CHANNEL SETUP
        wifiP2pManager = (WifiP2pManager) requireActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(requireContext(), requireContext().getMainLooper(), null);
        // START TO LISTEN FOR A REQUEST
        startToListenForRequest();
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.unlockLayout(requireActivity());
        wifiP2pManager.removeGroup(channel, null);
        requireActivity().unregisterReceiver(wifiP2pReceiver);
        wifiConnectionThread.closeSocket();
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            ((AppCompatActivity) requireActivity()).addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    public void startToListenForRequest() {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // WIFI NOT ENABLED CASE
        if (!wifiManager.isWifiEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                wifiManager.setWifiEnabled(true);
            else
                showEnableWiFiDialog();
        }
        // WIFI ENABLED CASE
        else {
            enableGPS((result) -> {
                if (result == Common.GPS_ENABLED_RESULT) {
                    wifiP2pManager.removeGroup(channel, this.wifiP2pActionListener);    // MAKE SURE THERE AREN'T WIFI DIRECT GROUPS
                    createGroup((resultCode) -> {   // CREATE NEW WIFI DIRECT GROUP
                        if(resultCode == WIFI_GROUP_CREATED_SUCCESS_RESULT) {
                            // RECEIVER SETUP
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                            requireActivity().registerReceiver(wifiP2pReceiver, intentFilter);
                            wifiConnectionThread = new WiFiConnectionRequestThread(requireContext(), (response) -> {
                                if(response != null)
                                    manageResponse(response);
                            });
                            wifiConnectionThread.start();
                            // SET TIME LIMIT TO CONNECTION THREAD
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    (() -> onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ERROR_RESULT)),
                                    OnConnectionFinishCallback.MAX_TIME_CONNECTION
                            );
                        }
                        else
                            onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_WIFI);
                    });
                }
            });
        }
    }

    /**
     * {@code @param:}
     *  OnGpsEnabledCallback callback;
     * If GPS is not enabled, requests to enable GPS
     */
    private void enableGPS(Common.OnIntegerResultGivenCallback callback) {
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
                    Log.i(Common.LOG_NORMAL, "GPS already enabled");
                    callback.getResult(Common.GPS_ENABLED_RESULT);
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
    private void createGroup(Common.OnIntegerResultGivenCallback callback) {
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.getResult(WIFI_GROUP_CREATED_SUCCESS_RESULT);
            }
            @Override
            public void onFailure(int i) {
                Log.i(Common.LOG_NORMAL, "Error: group not created. i: " + i);
                callback.getResult(WIFI_GROUP_CREATED_FAILURE_RESULT);
            }
        });
    }

    private void manageResponse(@NonNull String response) {
        switch (response) {
            case OnConnectionFinishCallback.WIFI_ERROR_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ERROR_RESULT);
                break;
            case OnConnectionFinishCallback.WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT);
                break;
            case OnConnectionFinishCallback.WIFI_CONNECTED_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_CONNECTED_DEVICE_RESULT);
                break;
            default:
                break;
        }
    }

    private void showWifiDisabledDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_disabled_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_disabled_dialog_msg))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    onConnectionFinishCallback.closeConnection();
                })
                .setCancelable(false)
                .show();
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
                        ((dialogInterface, i) -> onConnectionFinishCallback.closeConnection()))
                .setCancelable(false)
                .show();
    }
}
