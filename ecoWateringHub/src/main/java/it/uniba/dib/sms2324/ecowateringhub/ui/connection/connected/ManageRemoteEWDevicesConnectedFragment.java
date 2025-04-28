package it.uniba.dib.sms2324.ecowateringhub.ui.connection.connected;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDeviceAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.ui.connection.ManageRemoteEWDevicesConnectedActivity;

public class ManageRemoteEWDevicesConnectedFragment extends Fragment {
    private ArrayList<String> remoteDeviceList;
    private List<EcoWateringDevice> remoteDevicesConnectedList;
    private EcoWateringDeviceAdapter ecoWateringDeviceAdapter;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(requireContext(), MainActivity.class));
            requireActivity().finish();
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_manage_remote_ew_devices_connected_fragment, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else if(itemId == R.id.addNewRemoteEWDeviceItem) {
                onRemoteDeviceActionSelectedCallback.onRemoteDeviceActionSelected(ManageRemoteEWDevicesConnectedActivity.ACTION_ADD_REMOTE_DEVICE);
            }
            else if(itemId == R.id.refreshItem) {
                onRemoteDeviceActionSelectedCallback.onRemoteDeviceActionSelected(Common.REFRESH_FRAGMENT);
            }
            return false;
        }
    };
    private OnRemoteDeviceActionSelectedCallback onRemoteDeviceActionSelectedCallback;

    public interface OnRemoteDeviceActionSelectedCallback {
        void onRemoteDeviceActionSelected(int action);
    }
    public ManageRemoteEWDevicesConnectedFragment() {
        super(R.layout.fragment_manage_remote_ew_devices_connected);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnRemoteDeviceActionSelectedCallback) {
            onRemoteDeviceActionSelectedCallback = (OnRemoteDeviceActionSelectedCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onRemoteDeviceActionSelectedCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // ON BACK PRESSED CALLBACK SETUP
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        // TOOLBAR SETUP
        toolbarSetup(view);

        Common.showLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment);

        // REFRESH THIS_ECO_WATERING_HUB
        EcoWateringHub.getEcoWateringHubJsonString(MainActivity.thisEcoWateringHub.getDeviceID(), (jsonResponse) -> {
            MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            this.remoteDeviceList = (ArrayList<String>) MainActivity.thisEcoWateringHub.getRemoteDeviceList();
            requireActivity().runOnUiThread(() -> {
                // NO REMOTE DEVICES CONNECTED CASE
                if(this.remoteDeviceList == null || this.remoteDeviceList.isEmpty()) {
                    noRemoteDeviceConnectedCaseSetup(view);
                }
                // ALMOST ONE REMOTE DEVICE CONNECTED CASE
                else {
                    almostOneRemoteDeviceConnectedCaseSetup(view);
                }
                Common.hideLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment);
            });
        });
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.manage_remote_device_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void noRemoteDeviceConnectedCaseSetup(@NonNull View view) {
        TextView noRemoteDevicesConnectedCaseTextView = view.findViewById(R.id.noRemoteDevicesConnectedCaseTextView);
        noRemoteDevicesConnectedCaseTextView.setVisibility(View.VISIBLE);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            view.findViewById(R.id.remoteDevicesConnectedListView).setVisibility(View.GONE);
        }
        else {
            view.findViewById(R.id.remoteDevicesConnectedGridView).setVisibility(View.GONE);
        }
    }

    private void almostOneRemoteDeviceConnectedCaseSetup(@NonNull View view) {
        TextView remoteDevicesConnectedTextView = view.findViewById(R.id.remoteDevicesConnectedTextView);
        String titleString = this.remoteDeviceList.size() + " " + getResources().getQuantityString(
                it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                remoteDeviceList.size(),
                remoteDeviceList.size()
        );
        remoteDevicesConnectedTextView.setText(titleString);

        this.remoteDevicesConnectedList = new ArrayList<>();
        this.ecoWateringDeviceAdapter = new EcoWateringDeviceAdapter(requireContext(), this.remoteDevicesConnectedList, EcoWateringDeviceAdapter.CALLED_FROM_HUB);
        requireActivity().runOnUiThread(() -> this.ecoWateringDeviceAdapter.notifyDataSetChanged());
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ListView remoteDevicesListView = view.findViewById(R.id.remoteDevicesConnectedListView);
            remoteDevicesListView.setAdapter(this.ecoWateringDeviceAdapter);
            remoteDevicesListView.setOnItemClickListener((adapterView, v, position, l) -> showRemoteDeviceInfoDialog(position));
        }
        else {
            GridView remoteDevicesGridView = view.findViewById(R.id.remoteDevicesConnectedGridView);
            remoteDevicesGridView.setAdapter(this.ecoWateringDeviceAdapter);
            remoteDevicesGridView.setOnItemClickListener((adapterView, v, position, l) -> showRemoteDeviceInfoDialog(position));
        }

        // FILL REMOTE DEVICE LIST
        for(String deviceID : MainActivity.thisEcoWateringHub.getRemoteDeviceList()) {
            EcoWateringDevice.getEcoWateringDeviceJsonString(deviceID, (jsonResponse1) -> {
                this.remoteDevicesConnectedList.add(new EcoWateringDevice(jsonResponse1));
                requireActivity().runOnUiThread(() -> this.ecoWateringDeviceAdapter.notifyDataSetChanged());
            });
        }
    }

    private void disconnectDeviceFromHub(EcoWateringDevice device) {
        EcoWateringHub.removeRemoteDevice(
                MainActivity.thisEcoWateringHub.getDeviceID(),
                device,
                (response) -> {
                    if(response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) {
                        requireActivity().runOnUiThread(this::showRemoveRemoteDeviceSuccessfulDialog);
                    }
                    else {
                        requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                    }
                });
    }

    private void showRemoteDeviceInfoDialog(int position) {
        EcoWateringDevice device = remoteDevicesConnectedList.get(position);
        String infoMessage = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.device_id_label) + " " + device.getDeviceID();
        new AlertDialog.Builder(requireContext())
                .setTitle(device.getName())
                .setMessage(infoMessage)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.disconnect),
                        ((dialogInterface, i) -> showDisconnectRemoteDeviceConfirmDialog(device)))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }

    private void showDisconnectRemoteDeviceConfirmDialog(EcoWateringDevice device) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_confirm_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_confirm_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        ((dialogInterface, i) -> disconnectDeviceFromHub(device))
                )
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss())
                )
                .setCancelable(false)
                .show();
    }

    private void showRemoveRemoteDeviceSuccessfulDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_successfully_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> onRemoteDeviceActionSelectedCallback.onRemoteDeviceActionSelected(Common.ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED)))
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
