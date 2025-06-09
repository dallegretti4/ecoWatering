package it.uniba.dib.sms2324.ecowatering.entry;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

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

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowatering.connection.ConnectToEWHubActivity;
import it.uniba.dib.sms2324.ecowatering.management.ManageEWHubActivity;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHubAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHubComparator;

public class MainFragment extends Fragment {
    private static final String BUNDLE_EWH_LIST_KEY = "EWH_LIST";
    private static final String TO_DISCONNECT_HUB_ID_OUT_STATE = "TO_DISCONNECT_HUB_ID_OUT_STATE";
    private List<EcoWateringHub> ecoWateringHubList;
    private ArrayAdapter<EcoWateringHub> ecoWateringHubAdapter;
    private OnMainFragmentActionCallback onMainFragmentActionCallback;
    public interface OnMainFragmentActionCallback {
        void restartApp();
        void onMainFragmentUserProfileChosen();
    }
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_main_fragment, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == R.id.addNewEcoWateringHubItem) {
                startActivity(new Intent(requireContext(), ConnectToEWHubActivity.class));
                requireActivity().finish();
            }
            else if(itemId == R.id.refreshMainFragmentItem)
                onMainFragmentActionCallback.restartApp();
            else if(itemId == R.id.userProfileItem)
                onMainFragmentActionCallback.onMainFragmentUserProfileChosen();
            return false;
        }
    };
    private static String toDisconnectHubID = Common.NULL_STRING_VALUE;
    private static boolean isDisconnectedHubDialogVisible = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnMainFragmentActionCallback) {
            onMainFragmentActionCallback = (OnMainFragmentActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onMainFragmentActionCallback = null;
    }

    public MainFragment() {
        super(R.layout.fragment_main);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Common.showLoadingFragment(view, R.id.mainFrameLayout, R.id.includeLoadingFragment);
        toolbarSetup(view);
        hubListSetup(view);
        // CONFIGURATION CHANGED CASE
        if((savedInstanceState != null) && (savedInstanceState.getParcelableArrayList(BUNDLE_EWH_LIST_KEY) != null)) {
            refillEcoWateringHubList(Objects.requireNonNull(savedInstanceState.getParcelableArrayList(BUNDLE_EWH_LIST_KEY)));   // FILL HUB LIST
            Common.hideLoadingFragment(view, R.id.mainFrameLayout, R.id.includeLoadingFragment);
            if(savedInstanceState.getString(TO_DISCONNECT_HUB_ID_OUT_STATE) != null) {
                showDisconnectFromEWHubDialog(Objects.requireNonNull(savedInstanceState.getString(TO_DISCONNECT_HUB_ID_OUT_STATE)));    // DIALOG RECOVERING
            }
            else if (isDisconnectedHubDialogVisible) {  // DIALOG RECOVERING
                showDeviceDisconnectedDialog();
            }
        }
        else {  // NORMAL LAUNCH
            uploadEcoWateringHubList(() -> requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.mainFrameLayout, R.id.includeLoadingFragment)));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if((this.ecoWateringHubList != null) && (!this.ecoWateringHubList.isEmpty())) outState.putParcelableArrayList(BUNDLE_EWH_LIST_KEY, (new ArrayList<>(this.ecoWateringHubList)));
        if(!toDisconnectHubID.equals(Common.NULL_STRING_VALUE)) outState.putString(TO_DISCONNECT_HUB_ID_OUT_STATE, toDisconnectHubID);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void hubListSetup(@NonNull View view) {
        this.ecoWateringHubList = new ArrayList<>();
        this.ecoWateringHubAdapter = new EcoWateringHubAdapter(requireContext(), this.ecoWateringHubList);
        // SET ADAPTER
        AbsListView absListView = view.findViewById(R.id.hubAbsView);
        absListView.setAdapter(this.ecoWateringHubAdapter);
        absListView.setOnItemClickListener((adapterView, v, position, l) -> manageEWHub(position));
        absListView.setOnItemLongClickListener(
                (adapterView, v, position, l) -> {
                    showDisconnectPopUpMenu(v, position);
                    return true;
                });
    }

    private void refillEcoWateringHubList(ArrayList<EcoWateringHub> helpEcoWateringHubList) {
        for(EcoWateringHub hub : helpEcoWateringHubList) {
            this.ecoWateringHubList.add(hub);
            requireActivity().runOnUiThread(() -> this.ecoWateringHubAdapter.notifyDataSetChanged());
        }
    }

    private void uploadEcoWateringHubList(Common.OnMethodFinishCallback callback) {
        ArrayList<EcoWateringHub> hubListHelper = new ArrayList<>(); // AUXILIARY ARRAY LIST
        EcoWateringDevice.getEcoWateringDeviceJsonString(Common.getThisDeviceID(requireContext()), (jsonDeviceResponse) -> {
            MainActivity.setThisEcoWateringDevice(new EcoWateringDevice(jsonDeviceResponse));
            // NO HUB CONNECTED CASE
            if(MainActivity.getThisEcoWateringDevice().getEcoWateringHubList().isEmpty() || (MainActivity.getThisEcoWateringDevice().getEcoWateringHubList() == null)) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else {  // AT LEAN ONE HUB CONNECTED CASE
                for(String hubID : MainActivity.getThisEcoWateringDevice().getEcoWateringHubList()) {
                    EcoWateringHub.getEcoWateringHubJsonString(hubID, (jsonHubResponse) -> {
                        hubListHelper.add(new EcoWateringHub(jsonHubResponse));
                        if(hubListHelper.size() == MainActivity.getThisEcoWateringDevice().getEcoWateringHubList().size()) {
                            // LAST HUB CASE
                            hubListHelper.sort(new EcoWateringHubComparator());
                            for(int i=0; i<hubListHelper.size(); i++) { // LIST FILLING
                                this.ecoWateringHubList.add(hubListHelper.get(i));
                                requireActivity().runOnUiThread(this.ecoWateringHubAdapter::notifyDataSetChanged);
                            }
                            callback.canContinue();
                        }
                    });
                }
            }
        });
    }

    private void manageEWHub(int position) {
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, this.ecoWateringHubList.get(position));
        Intent manageEWHIntent = new Intent(requireContext(), ManageEWHubActivity.class);
        manageEWHIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(manageEWHIntent);
        requireActivity().finish();
    }

    private void showDisconnectPopUpMenu(@NonNull View view, int position) {
        String hubID = this.ecoWateringHubList.get(position).getDeviceID();
        PopupMenu disconnectMenu = new PopupMenu(requireContext(), view);
        disconnectMenu.getMenu().add(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.disconnect));
        disconnectMenu.setOnMenuItemClickListener((menuItem) -> {
            if(menuItem.getTitle() != null && menuItem.getTitle().equals(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.disconnect))) {
                showDisconnectFromEWHubDialog(hubID);
            }
            return true;
        });
        disconnectMenu.show();
    }

    private void showDisconnectFromEWHubDialog(@NonNull String hubID) {
        toDisconnectHubID = hubID;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.disconnect_from_ewh_confirm_title))
                .setMessage(getString(R.string.disconnect_from_ewh_confirm_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> {
                            toDisconnectHubID = Common.NULL_STRING_VALUE;
                            MainActivity.getThisEcoWateringDevice().disconnectFromEWHub(requireContext(), hubID, (response) -> {
                                if(response != null && response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) {
                                    requireActivity().runOnUiThread(this::showDeviceDisconnectedDialog);
                                }
                                else {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            });
                        })
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> {
                            toDisconnectHubID = Common.NULL_STRING_VALUE;
                            dialogInterface.dismiss();
                        })
                .setOnDismissListener((dialog) -> toDisconnectHubID = Common.NULL_STRING_VALUE)
                .show();
    }

    private void showDeviceDisconnectedDialog() {
        isDisconnectedHubDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.disconnected_from_ewh_success_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDisconnectedHubDialogVisible = false;
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        }))
                .setCancelable(false)
                .show();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        }))
                .setCancelable(false)
                .create()
                .show();
    }
}
