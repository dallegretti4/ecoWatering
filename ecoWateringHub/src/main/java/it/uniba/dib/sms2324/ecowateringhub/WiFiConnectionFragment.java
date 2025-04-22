package it.uniba.dib.sms2324.ecowateringhub;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads.WiFiConnectionRequestThread;

public class WiFiConnectionFragment extends Fragment {
    protected static ConstraintLayout constraintLayout;
    protected static ConstraintSet initialConstraintSet;
    private static ProgressBar progressBar;
    private static TextView titleTextView;
    private static ListView wifiConnectionListView;
    protected static WifiP2pManager wifiP2pManager;
    protected static WifiP2pManager.Channel channel;
    private static ArrayList<WifiP2pDevice> deviceList;
    protected static WifiP2pDevice tmpDevice;
    private WiFiConnectionRequestThread wiFiConnectionRequestThread;
    private final BroadcastReceiver peersReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                    wifiP2pManager.requestPeers(channel, ((peerList) -> {
                        deviceList = new ArrayList<>(peerList.getDeviceList());
                        ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                deviceList.stream().map(device -> device.deviceName).collect(Collectors.toList()));
                        wifiConnectionListView.setAdapter(deviceAdapter);
                        deviceAdapter.notifyDataSetChanged();
                        Log.i(Common.THIS_LOG, "peers size: " + deviceList.size());
                    }));
                }
                else if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if(networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                        wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener);
                    }
                }
            }
        }
    };
    protected static OnGpsEnabledCallback onGpsEnabledCallback;

    protected interface OnGpsEnabledCallback {
        void onGpsEnabled(int resultCode);
    }

    private static OnWiFiConnectionFinishCallback onWiFiConnectionFinishCallback;

    protected interface OnWiFiConnectionFinishCallback {
        void onWiFiConnectionFinish(int resultCode);
    }

    public interface OnWiFiConnectionResponseGivenCallback {
        void getResponse(String response);
    }
    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener = info -> {
        if(info.groupFormed && !info.isGroupOwner && info.groupOwnerAddress.getHostAddress() != null) {
            String peerAddress = info.groupOwnerAddress.getHostAddress();
            wiFiConnectionRequestThread = new WiFiConnectionRequestThread(requireContext(), peerAddress, ((response) -> {
                requireActivity().runOnUiThread(()->{
                    titleTextView.setText(R.string.wifi_connection_title);
                    wifiConnectionListView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    initialConstraintSet.applyTo(constraintLayout);
                });
                switch (response) {
                    case Common.WIFI_ERROR_RESPONSE:
                        onWiFiConnectionFinishCallback.onWiFiConnectionFinish(Common.WIFI_ERROR_RESULT);
                        break;
                    case Common.WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE:
                        onWiFiConnectionFinishCallback.onWiFiConnectionFinish(Common.WIFI_ALREADY_CONNECTED_DEVICE_RESULT);
                        break;
                    case Common.WIFI_CONNECTED_RESPONSE:
                        onWiFiConnectionFinishCallback.onWiFiConnectionFinish(Common.WIFI_CONNECTED_RESULT);
                        break;
                    default:
                        break;
                }
            }));
            wiFiConnectionRequestThread.start();
        }
    };

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
        // onWiFiConnectionFinishCallback = null;
        requireActivity().unregisterReceiver(peersReceiver);
        if(wiFiConnectionRequestThread != null && wiFiConnectionRequestThread.isAlive()) {
            wiFiConnectionRequestThread.interrupt();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
                }

                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemID = menuItem.getItemId();
                    if(itemID == android.R.id.home) {
                        ManageRemoteEWDevicesConnectedActivity.popBackStackFragment();
                    }
                    else if(itemID == R.id.refreshItem) {
                        onWiFiConnectionFinishCallback.onWiFiConnectionFinish(Common.WIFI_RESTART_FRAGMENT_RESULT);
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        progressBar = view.findViewById(R.id.wifiConnectionProgressBar);
        titleTextView = view.findViewById(R.id.wifiConnectionTitleTextView);
        wifiConnectionListView = view.findViewById(R.id.wifiConnectionListView);

        constraintLayout = view.findViewById(R.id.wifiConstraintLayout);
        initialConstraintSet = new ConstraintSet();
        initialConstraintSet.clone(constraintLayout);

        wifiP2pManager = (WifiP2pManager) requireActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(requireContext(), requireContext().getMainLooper(), null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        requireContext().registerReceiver(peersReceiver, intentFilter);

        wifiConnectionListView.setOnItemClickListener((adapterView, v, position, l) -> showConnectPopUpMenu(v, position));

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
            enableGPS(onGpsEnabledCallback = ((resultCode) -> {
                if (resultCode == Common.GPS_ENABLED_RESULT) {
                    wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(Common.THIS_LOG, "peers discovery started");
                        }
                        @Override
                        public void onFailure(int i) {
                            Log.i(Common.THIS_LOG, "peers discovery not started, i: " + i);
                            showErrorDialog();
                        }
                    });
                } else {
                    showWhyUseLocationDialog();
                }
            }));
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
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(requireActivity(), Common.GPS_WIFI_ENABLE_REQUEST);
                        } catch (IntentSender.SendIntentException sendEx) {
                            sendEx.printStackTrace();
                        }
                    }
                });
    }

    /**
     * To show the pop up menu
     * which avoid user to send a WiFi connection.
     */
    @SuppressLint("MissingPermission")
    private void showConnectPopUpMenu(@NonNull View view, int position) {
        PopupMenu menu = new PopupMenu(requireContext(), view);
        menu.getMenu().add(getString(R.string.connect));
        tmpDevice = deviceList.get(position);
        menu.setOnMenuItemClickListener((menuItem) -> {
            if (menuItem.getTitle() != null && menuItem.getTitle().equals(getString(R.string.connect))) {
                requireActivity().runOnUiThread(()->{
                    titleTextView.setText(getString(R.string.connecting));
                    wifiConnectionListView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(constraintLayout);
                    constraintSet.connect(R.id.wifiConnectionTitleTextView, ConstraintSet.BOTTOM, R.id.wifiConnectionProgressBar, ConstraintSet.TOP);
                    constraintSet.applyTo(constraintLayout);
                });
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = tmpDevice.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(Common.THIS_LOG, "connection started");
                    }
                    @Override
                    public void onFailure(int i) {
                        Log.i(Common.THIS_LOG, "connection not started");
                    }
                });
            }
            return true;
        });
        menu.show();
    }

    /**
     * To ask to user, to enable WiFi.
     * Positive button to go to setting.
     * Negative button to restart the fragment.
     */
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
                        ((dialogInterface, i) -> ManageRemoteEWDevicesConnectedActivity.popBackStackFragment()))

                .setCancelable(false)
                .show();
    }

    /**
     * Explains to user, why app uses user location.
     * Positive button to restart the fragment.
     */
    private void showWhyUseLocationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.why_use_location_dialog_title))
                .setMessage(getString(R.string.why_use_location_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> EcoWateringHub.getEcoWateringHubJsonString(
                                Common.getThisDeviceID(requireContext()),
                                (jsonResponse) -> {
                                    MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
                                    requireActivity().finish();
                                }))
                )
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user that something went wrong.
     * Positive button dismiss dialog.
     */
    private void showErrorDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> EcoWateringHub.getEcoWateringHubJsonString(
                                Common.getThisDeviceID(requireContext()),
                                (jsonResponse) -> {
                                    Common.restartApp(requireContext());
                                    requireActivity().finish();
                                }
                        ))
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
