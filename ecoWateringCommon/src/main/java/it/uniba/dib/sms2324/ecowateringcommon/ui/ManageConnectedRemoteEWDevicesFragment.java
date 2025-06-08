package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDeviceAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDeviceComparator;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class ManageConnectedRemoteEWDevicesFragment extends Fragment {
    private static final String CONNECTED_DEVICES_EWD_LIST_OUT_STATE = "CONNECTED_DEVICES_EWD_LIST_OUT_STATE";
    private static final String DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE = "DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE";
    private OnConnectedRemoteEWDeviceActionCallback onConnectedRemoteEWDeviceActionCallback;
    public interface OnConnectedRemoteEWDeviceActionCallback {
        void onManageConnectedDevicesGoBack();
        void onManageConnectedDevicesRefresh();
        void addNewRemoteDevice(); // ONLY FOR HUB
    }
    private static String hubID;
    private static String calledFrom;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onConnectedRemoteEWDeviceActionCallback.onManageConnectedDevicesGoBack();
        }
    };
    private ArrayList<String> remoteDeviceStringList;
    private ArrayList<EcoWateringDevice> remoteEWDeviceList;
    private EcoWateringDeviceAdapter ecoWateringDeviceAdapter;
    private int outStatePosition = -1;
    private static boolean isRemoveRemoteDeviceConfirmDialogVisible;
    private static boolean isRemoveRemoteDeviceSuccessfulDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;


    // CONSTRUCTORS

    public ManageConnectedRemoteEWDevicesFragment() {
        super(R.layout.fragment_manage_connected_remote_eco_watering_devices);
    }

    public ManageConnectedRemoteEWDevicesFragment(String hubIDString, String calledFromString) {
        super(R.layout.fragment_manage_connected_remote_eco_watering_devices);
        hubID = hubIDString;
        calledFrom = calledFromString;
    }


    // METHODS

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectedRemoteEWDeviceActionCallback) {
            onConnectedRemoteEWDeviceActionCallback = (OnConnectedRemoteEWDeviceActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onConnectedRemoteEWDeviceActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        Common.showLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment);
        toolbarSetup(view);
        Common.unlockLayout(requireActivity());

        if(savedInstanceState == null) {
            EcoWateringHub.getEcoWateringHubJsonString(hubID, (jsonResponse) -> {   // HUB OBJECT RECOVERING
                EcoWateringHub hub = new EcoWateringHub(jsonResponse);
                this.remoteDeviceStringList = (ArrayList<String>) hub.getRemoteDeviceList();
                if(this.remoteDeviceStringList != null && !this.remoteDeviceStringList.isEmpty()) { // THERE IS AT LEAST ONE CONNECTED REMOTE DEVICE CASE
                    // TITLE BUILDING
                    String titleString = this.remoteDeviceStringList.size() + " " + getResources().getQuantityString(
                            it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                            this.remoteDeviceStringList.size(),
                            this.remoteDeviceStringList.size()
                    );
                    ((TextView) view.findViewById(R.id.remoteDevicesConnectedTextView)).setText(titleString);
                    // LIST SETUP
                    this.remoteEWDeviceList = new ArrayList<>();
                    listSetup(view);
                    fillEWDList(() -> requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment)));
                }
                else {
                    noConnectedRemoteDeviceCaseSetup(view); // NO CONNECTED REMOTE DEVICES CASE
                    requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment));
                }
            });
        }
        // CONFIGURATION CHANGED CASE
        else {
            requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manageRemoteEWDevicesConnectedFragmentContainer, R.id.includeLoadingFragment));
            this.remoteEWDeviceList = savedInstanceState.getParcelableArrayList(CONNECTED_DEVICES_EWD_LIST_OUT_STATE); // ECO WATERING DEVICE LIST RECOVERING
            // NO CONNECTED REMOTE DEVICES CASE
            if(this.remoteEWDeviceList == null || this.remoteEWDeviceList.isEmpty()) {
                noConnectedRemoteDeviceCaseSetup(view);
            }
            else {  // THERE IS AT LEAST ONE CONNECTED REMOTE DEVICE CASE
                // TITLE BUILDING
                String titleString = this.remoteEWDeviceList.size() + " " + getResources().getQuantityString(
                        it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                        this.remoteEWDeviceList.size(),
                        this.remoteEWDeviceList.size()
                );
                ((TextView) view.findViewById(R.id.remoteDevicesConnectedTextView)).setText(titleString);
                // LIST SETUP
                listSetup(view);
                requireActivity().runOnUiThread(() -> this.ecoWateringDeviceAdapter.notifyDataSetChanged());
            }
            // DISCONNECT DEVICE CARD ENABLED RECOVERING CASE
            if(savedInstanceState.getInt(DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE) != -1) {
                if(isRemoveRemoteDeviceConfirmDialogVisible) showRemoveRemoteDeviceConfirmDialog(view, savedInstanceState.getInt(DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE));
                else showDisconnectDeviceCard(view, savedInstanceState.getInt(DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE));
            }
            // DIALOGS
            else if(isRemoveRemoteDeviceSuccessfulDialogVisible) showRemoveRemoteDeviceSuccessfulDialog();
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(CONNECTED_DEVICES_EWD_LIST_OUT_STATE, this.remoteEWDeviceList);
        outState.putInt(DISCONNECT_DEVICE_CARD_POSITION_OUT_STATE, this.outStatePosition);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            if(calledFrom.equals(Common.CALLED_FROM_HUB))
                toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ew_primary_color_from_hub, requireActivity().getTheme()));
            else
                toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ew_primary_color_from_device, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.manage_remote_device_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
                        menuInflater.inflate(R.menu.menu_manage_connected_remote_devices_for_hub, menu);
                    }
                    else {
                        menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
                    }
                }
                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemId = menuItem.getItemId();
                    if(itemId == android.R.id.home) {
                        onConnectedRemoteEWDeviceActionCallback.onManageConnectedDevicesGoBack();
                    }
                    else if(itemId == R.id.refreshItem) {
                        onConnectedRemoteEWDeviceActionCallback.onManageConnectedDevicesRefresh();
                    }
                    else if(calledFrom.equals(Common.CALLED_FROM_HUB) && itemId == R.id.addNewRemoteEWDeviceItem) {     // ONLY FOR HUB
                        onConnectedRemoteEWDeviceActionCallback.addNewRemoteDevice();
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void listSetup(@NonNull View view) {
        this.ecoWateringDeviceAdapter = new EcoWateringDeviceAdapter(requireContext(), this.remoteEWDeviceList, calledFrom);
        // LIST SETUP
        AbsListView absListView = view.findViewById(R.id.remoteDevicesConnectedAbsView);
        absListView.setAdapter(this.ecoWateringDeviceAdapter);
        absListView.setAdapter(this.ecoWateringDeviceAdapter);
        absListView.setOnItemClickListener((adapterView, v, position, l) -> showDisconnectDeviceCard(view, position));
    }

    private void fillEWDList(Common.OnMethodFinishCallback callback) {
        ArrayList<EcoWateringDevice> remoteEWDeviceHelperList = new ArrayList<>();
        for(String deviceID : this.remoteDeviceStringList) {
            EcoWateringDevice.getEcoWateringDeviceJsonString(deviceID, (jsonResponse) -> {  // DEVICE OBJECT BUILDING
                remoteEWDeviceHelperList.add(new EcoWateringDevice(jsonResponse));
                if(remoteEWDeviceHelperList.size() == this.remoteDeviceStringList.size()) { // IS LAST OBJECT
                    remoteEWDeviceHelperList.sort(new EcoWateringDeviceComparator());   // SORT HELPER LIST
                    for(EcoWateringDevice device : remoteEWDeviceHelperList) {  // FILL LIST / GRID VIEW
                        this.remoteEWDeviceList.add(device);
                        requireActivity().runOnUiThread(() -> this.ecoWateringDeviceAdapter.notifyDataSetChanged());
                        callback.canContinue();
                    }
                }
            });
        }
    }

    private void showDisconnectDeviceCard(@NonNull View view, int position) {
        this.outStatePosition = position;
        view.findViewById(R.id.disconnectDeviceCard).setVisibility(View.VISIBLE);   // MAKE CARD VISIBLE
        view.findViewById(R.id.disconnectDeviceBackgroundCard).setVisibility(View.VISIBLE);    // MAKE CARD BACKGROUND VISIBLE
        view.findViewById(R.id.cancelButton).setOnClickListener((v) -> hideDisconnectDeviceCard(view)); // CANCEL BUTTON SETUP
        // DISCONNECT BUTTON SETUP
        view.findViewById(R.id.disconnectRemoteDeviceButton).setOnClickListener((v) -> showRemoveRemoteDeviceConfirmDialog(view, position));
    }

    private void hideDisconnectDeviceCard(@NonNull View view) {
        this.outStatePosition = -1;
        view.findViewById(R.id.disconnectDeviceCard).setVisibility(View.GONE);  // MAKE CARD NOT VISIBLE
        view.findViewById(R.id.disconnectDeviceBackgroundCard).setVisibility(View.GONE);    // MAKE CARD BACKGROUND NOT VISIBLE
    }

    private void noConnectedRemoteDeviceCaseSetup(@NonNull View view) {
        view.findViewById(R.id.noRemoteDevicesConnectedCaseTextView).setVisibility(View.VISIBLE);
        view.findViewById(R.id.remoteDevicesConnectedAbsView).setVisibility(View.GONE);
    }

    private void showRemoveRemoteDeviceConfirmDialog(@NonNull View view, int position) {
        isRemoveRemoteDeviceConfirmDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.remove_remote_device_confirm_title))
                .setMessage(getString(R.string.remove_remote_device_confirm_message))
                .setPositiveButton(
                        getString(R.string.confirm_button),
                        ((dialogInterface, i) -> {
                            isRemoveRemoteDeviceConfirmDialogVisible = false;
                            EcoWateringHub.removeRemoteDevice(
                                    hubID,
                                    this.remoteEWDeviceList.get(position),
                                    (response) ->
                                            requireActivity().runOnUiThread(() -> {
                                                hideDisconnectDeviceCard(view);
                                                if(response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) showRemoveRemoteDeviceSuccessfulDialog();
                                                else showHttpErrorFaultDialog();
                                            }));
                        }))
                .setNegativeButton(
                        getString(R.string.close_button),
                        (dialogInterface, i) -> {
                            isRemoveRemoteDeviceConfirmDialogVisible = false;
                            dialogInterface.dismiss();
                        }
                )
                .setCancelable(false)
                .show();

    }

    private void showRemoveRemoteDeviceSuccessfulDialog() {
        isRemoveRemoteDeviceSuccessfulDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remove_remote_device_successfully_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isRemoveRemoteDeviceSuccessfulDialogVisible = false;
                            onConnectedRemoteEWDeviceActionCallback.onManageConnectedDevicesRefresh();
                        }))
                .setCancelable(false)
                .show();
    }
    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            Common.restartApp(requireContext());
                            requireActivity().finish();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
