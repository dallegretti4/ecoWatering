package it.uniba.dib.sms2324.ecowateringhub.connection.mode.wifi;

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
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class WiFiConnectionFragment extends Fragment {
    private ConstraintLayout constraintLayout;
    private ConstraintSet initialConstraintSet;
    private ProgressBar progressBar;
    private TextView titleTextView;
    private ListView wifiConnectionListView;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private ArrayList<WifiP2pDevice> deviceList;
    private WiFiConnectionRequestThread wiFiConnectionRequestThread;
    private OnConnectionFinishCallback onConnectionFinishCallback;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemID = menuItem.getItemId();
            if(itemID == android.R.id.home) {
                onConnectionFinishCallback.closeConnection();
            }
            else if(itemID == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_WIFI);
            }
            return false;
        }
    };
    private final BroadcastReceiver peersReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                    // FILL LIST VIEW
                    wifiP2pManager.requestPeers(channel, ((peerList) -> {
                        deviceList = new ArrayList<>(peerList.getDeviceList());
                        ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                deviceList.stream().map(device -> device.deviceName).collect(Collectors.toList()));
                        wifiConnectionListView.setAdapter(deviceAdapter);
                        deviceAdapter.notifyDataSetChanged();
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
        if (context instanceof OnConnectionFinishCallback) {
            onConnectionFinishCallback = (OnConnectionFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onConnectionFinishCallback = null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // SETUP
        Common.lockLayout(requireActivity());
        toolbarSetup(view);
        fragmentLayoutSetup(view);
        wifiP2pManager = (WifiP2pManager) requireActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(requireContext(), requireContext().getMainLooper(), null);
        peersReceiverSetup();
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // LOGIC
        if (!wifiManager.isWifiEnabled()) { // WIFI NOT ENABLED CASE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) wifiManager.setWifiEnabled(true);
            else showEnableWiFiDialog();
        }
        else {  // WIFI ENABLED CASE
            enableGPS((resultCode) -> {
                if (resultCode == Common.GPS_ENABLED_RESULT) {
                    wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(Common.LOG_NORMAL, "peers discovery started");
                        }
                        @Override
                        public void onFailure(int i) {
                            Log.i(Common.LOG_NORMAL, "peers discovery not started, i: " + i);
                            showErrorDialog();
                        }
                    });
                } else {
                    showWhyUseLocationDialog();
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(peersReceiver);
        if(wiFiConnectionRequestThread != null && wiFiConnectionRequestThread.isAlive()) {
            wiFiConnectionRequestThread.interrupt();
        }
        Common.unlockLayout(requireActivity());
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.wifi_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void fragmentLayoutSetup(@NonNull View view) {
        progressBar = view.findViewById(R.id.wifiConnectionProgressBar);
        titleTextView = view.findViewById(R.id.wifiConnectionTitleTextView);
        wifiConnectionListView = view.findViewById(R.id.wifiConnectionListView);
        constraintLayout = view.findViewById(R.id.wifiConstraintLayout);
        initialConstraintSet = new ConstraintSet();
        initialConstraintSet.clone(constraintLayout);
        wifiConnectionListView.setOnItemClickListener((adapterView, v, position, l) -> showConnectPopUpMenu(v, position));
    }

    private void peersReceiverSetup() {
        IntentFilter peersIntentFilter = new IntentFilter();
        peersIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        peersIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        peersIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        peersIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        requireActivity().registerReceiver(peersReceiver, peersIntentFilter);
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
        WifiP2pDevice tmpDevice = deviceList.get(position);
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
                        Log.i(Common.LOG_NORMAL, "connection started");
                    }
                    @Override
                    public void onFailure(int i) {
                        Log.i(Common.LOG_NORMAL, "connection not started");
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
                        ((dialogInterface, i) -> onConnectionFinishCallback.closeConnection()))

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
                        ((dialogInterface, i) -> onConnectionFinishCallback.closeConnection())
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
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
