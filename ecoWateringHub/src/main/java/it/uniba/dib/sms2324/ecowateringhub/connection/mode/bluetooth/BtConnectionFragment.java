package it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringhub.R;

public class BtConnectionFragment extends Fragment {
    private static final String BT_CONNECTED_RESPONSE = "remoteDeviceAdded";
    private static final int BT_ENABLED_RESULT = 1009;
    private static final int MAX_TIME_CONNECTION = 30 * 1000;
    private TextView titleTextView;
    private TextView titleConnectingTextView;
    private ListView deviceListView;
    private ProgressBar btConnectionRequestProgressBar;
    private static List<BluetoothDevice> btDeviceList;
    private static ArrayAdapter<String> deviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice deviceToAdd;
    private BtDiscoveryThread btDiscoveryThread;
    private BtConnectionRequestThread btConnectionRequestThread;
    private OnConnectionFinishCallback onConnectionFinishCallback;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), ((resultCode) -> {
                if(resultCode.getResultCode() == Activity.RESULT_OK) {
                    onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH);
                }
                else {
                    onConnectionFinishCallback.closeConnection();
                }
            }));

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
                onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH);
            }
            return false;
        }
    };

    public BtConnectionFragment() {
        super(R.layout.fragment_bt_connection);
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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Common.lockLayout(requireActivity());
        // TOOLBAR SETUP
        toolbarSetup(view);
        // FRAGMENT LAYOUT SETUP
        fragmentLayoutSetup(view);
        // CHECK DEVICE SUPPORT BLUETOOTH
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            // ENABLE GPS
            enableGPS((result) -> {
                if(result == Common.GPS_ENABLED_RESULT) {
                    startBluetoothDeviceDiscovery();
                }
            });
        }
        // DEVICE NOT SUPPORTS BLUETOOTH CASE
        else {
            showDeviceNotSupportsBluetoothDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothAdapter = null;
        if(btDiscoveryThread != null && btDiscoveryThread.isAlive()) {
            btDiscoveryThread.interrupt();
        }
        if(btConnectionRequestThread != null && btConnectionRequestThread.isAlive()) {
            btConnectionRequestThread.interrupt();
        }
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void fragmentLayoutSetup(@NonNull View view) {
        titleTextView = view.findViewById(R.id.btConnectionTitleTextView);
        titleConnectingTextView = view.findViewById(R.id.btConnectionTitleConnectingTextView);
        btConnectionRequestProgressBar = view.findViewById(R.id.connectionRequestProgressBar);

        btDeviceList = new ArrayList<>();
        List<String> deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, deviceList);
        deviceListView = view.findViewById(R.id.btFoundedDevicesListView);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener((adapterView, v, position, l) -> showConnectPopUpMenu(v, position));
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
                    Log.i(Common.THIS_LOG, "GPS already enabled");
                    callback.getResult(Common.GPS_ENABLED_RESULT);
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
        enableBluetooth((resultCode) -> {
            // BLUETOOTH ENABLED OR ENABLING REQUEST ACCEPTED CASE
            if (resultCode == BT_ENABLED_RESULT) {
                // START DISCOVERY THREAD
                btDiscoveryThread = new BtDiscoveryThread(requireContext(), bluetoothAdapter);
                requireActivity().runOnUiThread(btDiscoveryThread);
            }
            // BLUETOOTH ENABLING REQUEST REJECTED CASE
            else {
                onConnectionFinishCallback.closeConnection();
            }
        });
    }

    /**
     * {@code @param:}
     *  OnBtEnableResponseCallback callback -> to manage the response.
     * To check and enable the bluetooth.
     */
    private void enableBluetooth(Common.OnIntegerResultGivenCallback callback) {
        if(!bluetoothAdapter.isEnabled()) {
            enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
        else {
            callback.getResult(BT_ENABLED_RESULT);
        }
    }

    /**
     * To show the pop up menu
     * which avoid user to send a bluetooth connection.
     */
    @SuppressLint("MissingPermission")
    private void showConnectPopUpMenu(@NonNull View view, int position) {
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

    private void sendBtConnectionRequest(@NonNull Context context, Common.OnStringResponseGivenCallback callback) {
        titleTextView.setVisibility(View.GONE);
        deviceListView.setVisibility(View.GONE);
        titleConnectingTextView.setVisibility(View.VISIBLE);
        btConnectionRequestProgressBar.setVisibility(View.VISIBLE);
        btConnectionRequestThread = new BtConnectionRequestThread(context, deviceToAdd, callback);
        btConnectionRequestThread.start();
        // SET TIME LIMIT TO CONNECTION THREAD
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if(btConnectionRequestThread.isAlive()) {
                btConnectionRequestThread.cancel();
                requireActivity().runOnUiThread(this::showDeviceNotAvailableDialog);
            }
        }, MAX_TIME_CONNECTION);
    }

    private void manageResponse(String response) {
        switch (response) {
            case OnConnectionFinishCallback.BT_ERROR_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ERROR_RESULT);
                break;
            case OnConnectionFinishCallback.BT_ALREADY_CONNECTED_DEVICE_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT);
                break;
            case BT_CONNECTED_RESPONSE:
                onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_CONNECTED_DEVICE_RESULT);
                break;
            default:
                break;
        }
    }

    protected static List<BluetoothDevice> getBtDeviceList() {
        return btDeviceList;
    }

    protected static void addToBtDeviceList(@NonNull BluetoothDevice device) {
        btDeviceList.add(device);
    }

    protected static void addToDeviceListAdapter(String deviceName) {
        deviceListAdapter.add(deviceName);
        deviceListAdapter.notifyDataSetChanged();
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
                        ((dialogInterface, i) -> onConnectionFinishCallback.closeConnection())
                )
                .setCancelable(false);
        dialog.show();
    }

    private void showDeviceNotAvailableDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.remote_device_not_available_title))
                .setMessage(getString(R.string.remote_device_not_available_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> requireActivity().runOnUiThread(() ->
                                onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH))
                ).setCancelable(false)
                .show();
    }
}
