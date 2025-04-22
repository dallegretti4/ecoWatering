package it.uniba.dib.sms2324.ecowatering;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringDeviceAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;

public class ManageRemoteEWDevicesConnectedFragment extends Fragment {
    private ArrayList<EcoWateringDevice> ecoWateringDeviceList;
    private EcoWateringDeviceAdapter ecoWateringDeviceAdapter;
    private OnRemoteDeviceConnectedActionCallback onRemoteDeviceConnectedActionCallback;
    protected interface OnRemoteDeviceConnectedActionCallback {
        void onRemoteDeviceConnectedAction(int result);
    }

    public ManageRemoteEWDevicesConnectedFragment() {
        super(R.layout.fragment_manage_remote_eco_watering_devices_connected);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnRemoteDeviceConnectedActionCallback) {
            onRemoteDeviceConnectedActionCallback = (OnRemoteDeviceConnectedActionCallback) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remote_device_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.menu_manage_eco_watering_hub, menu);
                }

                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemID = menuItem.getItemId();
                    if(itemID == android.R.id.home) {
                        ManageEWHubActivity.popBackStackFragment();
                    }
                    else if(itemID == R.id.refreshEWHubItem) {
                        EcoWateringHub.getEcoWateringHubJsonString(ManageEWHubActivity.selectedEWHub.getDeviceID(), (jsonResponse) -> {
                            ManageEWHubActivity.selectedEWHub = new EcoWateringHub(jsonResponse);
                            onRemoteDeviceConnectedActionCallback.onRemoteDeviceConnectedAction(Common.ACTION_REMOTE_DEVICES_CONNECTED_RESTART_FRAGMENT);
                        });
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

            TextView remoteDevicesConnectedTextView = view.findViewById(R.id.remoteDevicesConnectedTextView);
            remoteDevicesConnectedTextView.setText(getResources().getQuantityString(
                    it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                    ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().size(),
                    ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().size()
            ));

            ecoWateringDeviceList = new ArrayList<>();
            ecoWateringDeviceAdapter = new EcoWateringDeviceAdapter(requireContext(), ecoWateringDeviceList, Common.CALLED_FROM_DEVICE);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ListView ewDevicesListView = view.findViewById(R.id.remoteDevicesConnectedListView);
                ewDevicesListView.setAdapter(ecoWateringDeviceAdapter);
                ewDevicesListView.setOnItemClickListener((adapterView, v, position, l) -> showRemoteDeviceInfoDialog(position));
            }
            else {
                GridView ewDevicesGridView = view.findViewById(R.id.remoteDevicesConnectedGridView);
                ewDevicesGridView.setAdapter(ecoWateringDeviceAdapter);
                ewDevicesGridView.setOnItemClickListener((adapterView, v, position, l) -> showRemoteDeviceInfoDialog(position));
            }
            requireActivity().runOnUiThread(ecoWateringDeviceAdapter::notifyDataSetChanged);
            for(String deviceID : ManageEWHubActivity.selectedEWHub.getRemoteDeviceList()) {
                EcoWateringDevice.getEcoWateringDeviceJsonString(deviceID, (jsonResponse) -> {
                    ecoWateringDeviceList.add(new EcoWateringDevice(jsonResponse));
                    requireActivity().runOnUiThread(ecoWateringDeviceAdapter::notifyDataSetChanged);
                });
            }
        }
    }

    private void showRemoteDeviceInfoDialog(int position) {
        EcoWateringDevice device = ecoWateringDeviceList.get(position);
        String infoMessage = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.device_id_label) + " " + device.getDeviceID();
        new AlertDialog.Builder(requireContext())
                .setTitle(device.getName())
                .setMessage(infoMessage)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.disconnect),
                        ((dialogInterface, i) -> {
                            // REMOTE DEVICE DISCONNECTED IT SELF FROM HUB CASE
                            if(Common.getThisDeviceID(requireContext()).equals(device.getDeviceID())) {
                                showRemoteDeviceRemoveItSelfFromHubDialog(device);
                            }
                            // OTHER CASES
                            else {
                                showDisconnectRemoteDeviceConfirmDialog(device);
                            }
                        }))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setCancelable(false)
                .show();
    }

    private void showRemoteDeviceRemoveItSelfFromHubDialog(EcoWateringDevice device) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remote_device_remove_it_self_from_hub_confirm_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> EcoWateringHub.removeRemoteDevice(
                                ManageEWHubActivity.selectedEWHub.getDeviceID(),
                                device,
                                (response) -> {
                                    if(response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) {
                                        requireActivity().runOnUiThread(() -> showRemoveRemoteDeviceSuccessfulDialog(true));
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                    }
                                }
                        )
                )
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void showDisconnectRemoteDeviceConfirmDialog(EcoWateringDevice device) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_confirm_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_confirm_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        ((dialogInterface, i) -> EcoWateringHub.removeRemoteDevice(
                                ManageEWHubActivity.selectedEWHub.getDeviceID(),
                                device,
                                (response) -> {
                                    if(response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) {
                                        requireActivity().runOnUiThread(() -> showRemoveRemoteDeviceSuccessfulDialog(false));
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                    }
                                })))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss())
                )
                .setCancelable(false)
                .show();
    }

    private void showRemoveRemoteDeviceSuccessfulDialog(boolean remoteDeviceRemoveItSelf) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_successfully_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) ->
                                EcoWateringHub.getEcoWateringHubJsonString(ManageEWHubActivity.selectedEWHub.getDeviceID(), (jsonResponse) -> {
                                    ManageEWHubActivity.selectedEWHub = new EcoWateringHub(jsonResponse);
                                    if(remoteDeviceRemoveItSelf) {
                                        onRemoteDeviceConnectedActionCallback.onRemoteDeviceConnectedAction(Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_IT_SELF_REMOVED);
                                    }
                                    else {
                                        onRemoteDeviceConnectedActionCallback.onRemoteDeviceConnectedAction(Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED);
                                    }
                                })))
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            Common.restartApp(requireContext());
                            requireActivity().finish();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
