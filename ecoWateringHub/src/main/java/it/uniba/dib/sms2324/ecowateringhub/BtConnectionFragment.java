package it.uniba.dib.sms2324.ecowateringhub;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads.BtConnectionRequestThread;
import it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads.BtDiscoveryThread;

public class BtConnectionFragment extends Fragment {
    protected static TextView titleTextView;
    protected static TextView titleConnectingTextView;
    protected static ListView deviceListView;
    protected static ProgressBar btConnectionRequestProgressBar;
    public static List<BluetoothDevice> btDeviceList;
    public static List<String> deviceList;
    public static ArrayAdapter<String> deviceListAdapter;
    protected static BluetoothAdapter bluetoothAdapter;
    protected static BluetoothDevice deviceToAdd;
    private BtDiscoveryThread btDiscoveryThread;
    private BtConnectionRequestThread btConnectionRequestThread;
    private OnBtEnableResponseCallback onBtEnableResponseCallback;
    private interface OnBtEnableResponseCallback {
        void getResult(int resultCode);
    }
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), ((resultCode) -> {
                if(resultCode.getResultCode() == Activity.RESULT_OK) {
                    onBtEnableResponseCallback.getResult(Common.BT_ENABLED_RESULT);
                }
                else {
                    onBtEnableResponseCallback.getResult(Common.BT_NOT_ENABLED_RESULT);
                }
            }));

    public interface OnBtConnectionResponseGivenCallback {
        void getResponse(String response);
    }

    protected static OnGpsEnabledCallback onGpsEnabledCallback;
    protected interface OnGpsEnabledCallback {
        void onGpsEnabled(int resultCode);
    }

    private static OnBtConnectionFinishCallback onBtConnectionFinishCallback;
    public interface OnBtConnectionFinishCallback {
        void onBtConnectionFinish(int btResultCode);
    }

    public BtConnectionFragment() {
        super(R.layout.fragment_bt_connection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBtConnectionFinishCallback) {
            onBtConnectionFinishCallback = (OnBtConnectionFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onBtConnectionFinishCallback = null;
        bluetoothAdapter = null;
        if(btDiscoveryThread != null && btDiscoveryThread.isAlive()) {
            btDiscoveryThread.interrupt();
        }
        if(btConnectionRequestThread != null && btConnectionRequestThread.isAlive()) {
            btConnectionRequestThread.interrupt();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_connection_toolbar_title));
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
                        if(btConnectionRequestProgressBar.getVisibility() == View.VISIBLE) {
                            titleTextView.setVisibility(View.VISIBLE);
                            deviceListView.setVisibility(View.VISIBLE);
                            titleConnectingTextView.setVisibility(View.GONE);
                            btConnectionRequestProgressBar.setVisibility(View.GONE);
                        }
                        btDeviceList.clear();
                        deviceList.clear();
                        deviceListAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, deviceList);
                        deviceListView.setAdapter(deviceListAdapter);
                        startBluetoothDeviceDiscovery();
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        titleTextView = view.findViewById(R.id.btConnectionTitleTextView);
        titleConnectingTextView = view.findViewById(R.id.btConnectionTitleConnectingTextView);
        btConnectionRequestProgressBar = view.findViewById(R.id.connectionRequestProgressBar);

        btDeviceList = new ArrayList<>();
        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, deviceList);
        deviceListView = view.findViewById(R.id.btFoundedDevicesListView);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener((adapterView, v, position, l) -> showConnectPopUpMenu(v, position));

        // CHECK DEVICE SUPPORT BLUETOOTH
        if (bluetoothAdapter != null) {
            enableGPS(onGpsEnabledCallback = ((result) -> {
                if(result == Common.GPS_ENABLED_RESULT) {
                    startBluetoothDeviceDiscovery();
                }
            }));
        }
        // DEVICE NOT SUPPORTS BLUETOOTH CASE
        else {
            showDeviceNotSupportsBluetoothDialog();
        }
    }

    /**
     * {@code @param:}
     *  OnGpsEnabledCallback callback;
     * If GPS is not enabled, requests to enable GPS
     */
    private void enableGPS(OnGpsEnabledCallback callback) {
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
                            resolvableApiException.startResolutionForResult(requireActivity(), Common.GPS_BT_ENABLE_REQUEST);
                        } catch (IntentSender.SendIntentException sendEx) {
                            sendEx.printStackTrace();
                        }
                    }
                });
    }

    /**
     * To start the bluetooth discovery
     */
    private void startBluetoothDeviceDiscovery() {
        // CHECK AND ENABLE BLUETOOTH
        enableBluetooth(onBtEnableResponseCallback = ((resultCode) -> {
            // BLUETOOTH ENABLED OR ENABLING REQUEST ACCEPTED CASE
            if (resultCode == Common.BT_ENABLED_RESULT) {
                // START DISCOVERY THREAD
                btDiscoveryThread = new BtDiscoveryThread(requireContext(), bluetoothAdapter);
                requireActivity().runOnUiThread(btDiscoveryThread);
            }
            // BLUETOOTH ENABLING REQUEST REJECTED CASE
            else {
                ManageRemoteEWDevicesConnectedActivity.popBackStackFragment();
            }
        }));
    }

    /**
     * {@code @param:}
     *  OnBtEnableResponseCallback callback -> to manage the response.
     * To check and enable the bluetooth.
     */
    private void enableBluetooth(OnBtEnableResponseCallback callback) {
        if(!bluetoothAdapter.isEnabled()) {
            enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
        else {
            callback.getResult(Common.BT_ENABLED_RESULT);
        }
    }

    /**
     * To show the pop up menu
     * which avoid user to send a bluetooth connection.
     */
    @SuppressLint("MissingPermission")
    private void showConnectPopUpMenu(@NonNull View view, int position) {
        Log.i(Common.THIS_LOG, "popup menu");
        deviceToAdd = btDeviceList.get(position);
        PopupMenu connectPopUpMenu = new PopupMenu(requireContext(), view);
        connectPopUpMenu.getMenu().add(getString(R.string.connect));
        connectPopUpMenu.setOnMenuItemClickListener((menuItem) -> {
            if(menuItem.getTitle() != null && menuItem.getTitle().equals(getString(R.string.connect))) {
                if(bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                sendBtConnectionRequest(requireContext(), (response) ->
                    requireActivity().runOnUiThread(() -> {
                        titleTextView.setVisibility(View.VISIBLE);
                        deviceListView.setVisibility(View.VISIBLE);
                        titleConnectingTextView.setVisibility(View.GONE);
                        btConnectionRequestProgressBar.setVisibility(View.GONE);
                        manageResponse(response);
                    })
                );
            }
            return true;
        });
        requireActivity().runOnUiThread(connectPopUpMenu::show);
    }

    protected void sendBtConnectionRequest(@NonNull Context context, OnBtConnectionResponseGivenCallback callback) {
        titleTextView.setVisibility(View.GONE);
        deviceListView.setVisibility(View.GONE);
        titleConnectingTextView.setVisibility(View.VISIBLE);
        btConnectionRequestProgressBar.setVisibility(View.VISIBLE);
        btConnectionRequestThread = new BtConnectionRequestThread(context, deviceToAdd, callback);
        btConnectionRequestThread.start();
    }

    protected static void manageResponse(String response) {
        switch (response) {
            case Common.BT_ERROR_RESPONSE:
                onBtConnectionFinishCallback.onBtConnectionFinish(Common.BT_ERROR_RESULT);
                break;
            case Common.BT_ALREADY_CONNECTED_DEVICE_RESPONSE:
                onBtConnectionFinishCallback.onBtConnectionFinish(Common.BT_ALREADY_CONNECTED_RESULT);
                break;
            case Common.BT_CONNECTED_RESPONSE:
                onBtConnectionFinishCallback.onBtConnectionFinish(Common.BT_CONNECTED_RESULT);
                break;
            default:
                break;
        }
    }

    /**
     * Notify the user, device not supports bluetooth.
     * Positive button to return to the ConnectionChooserFragment.
     */
    private void showDeviceNotSupportsBluetoothDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> ManageRemoteEWDevicesConnectedActivity.popBackStackFragment())
                )
                .setCancelable(false);
        dialog.show();
    }
}
