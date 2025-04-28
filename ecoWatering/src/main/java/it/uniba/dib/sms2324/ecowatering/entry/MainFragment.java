package it.uniba.dib.sms2324.ecowatering.entry;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
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
import it.uniba.dib.sms2324.ecowatering.control.ManageEWHubActivity;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHubAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHubComparator;

public class MainFragment extends Fragment {
    private static final String BUNDLE_EWH_LIST_KEY = "EWH_LIST";
    private List<EcoWateringHub> ecoWateringHubList;
    private ArrayAdapter<EcoWateringHub> ecoWateringHubAdapter;
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
            else if(itemId == R.id.refreshMainFragmentItem) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else if(itemId == R.id.userProfileItem) {
                onMainFragmentActionCallback.onMainFragmentUserProfileChosen();
            }
            return false;
        }
    };
    private OnMainFragmentActionCallback onMainFragmentActionCallback;
    public interface OnMainFragmentActionCallback {
        void onMainFragmentUserProfileChosen();
    }

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
        // TOOLBAR SETUP
        toolbarSetup(view);
        // LIST & ADAPTER SETUP
        ecoWateringHubList = new ArrayList<>();
        ecoWateringHubAdapter = new EcoWateringHubAdapter(requireContext(), ecoWateringHubList);
        requireActivity().runOnUiThread(() -> ecoWateringHubAdapter.notifyDataSetChanged());
        // LIST VIEW (PORTRAIT CONFIGURATION)/ GRID VIEW (LANDSCAPE CONFIGURATION) SETUP
        listGridViewSetup(view);

        // REFILL EWH LIST FROM CONFIGURATION CHANGE CASE
        if(savedInstanceState != null && savedInstanceState.getParcelableArrayList(BUNDLE_EWH_LIST_KEY) != null) {
            refillEcoWateringHubList(view, savedInstanceState);
        }
        // FILL EWH LIST FROM DATABASE SERVER CASE
        else {
            uploadEcoWateringHubList(view);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(BUNDLE_EWH_LIST_KEY, new ArrayList<>(ecoWateringHubList));
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void listGridViewSetup(@NonNull View view) {
        // SET ADAPTER PORTRAIT LAYOUT CASE
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ListView ecoWateringHubListView = view.findViewById(R.id.hubListListView);
            ecoWateringHubListView.setAdapter(ecoWateringHubAdapter);
            ecoWateringHubListView.setOnItemClickListener((adapterView, v, position, l) -> manageEWHub(position));
            ecoWateringHubListView.setOnItemLongClickListener(
                    (adapterView, v, position, l) -> {
                        showDisconnectPopUpMenu(v, position);
                        return true;
                    });
        }
        // SET ADAPTER LANDSCAPE LAYOUT CASE
        else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            GridView ecoWateringHubGridView = view.findViewById(R.id.hubListGridView);
            ecoWateringHubGridView.setAdapter(ecoWateringHubAdapter);
            ecoWateringHubGridView.setOnItemClickListener((adapterView, v, position, l) -> manageEWHub(position));
            ecoWateringHubGridView.setOnItemLongClickListener(
                    (adapterView, v, position, l) -> {
                        showDisconnectPopUpMenu(v, position);
                        return true;
                    });
        }
    }

    private void refillEcoWateringHubList(@NonNull View view, Bundle savedInstanceState) {
        ArrayList<EcoWateringHub> helpEcoWateringHubList = savedInstanceState.getParcelableArrayList(BUNDLE_EWH_LIST_KEY);
        if(helpEcoWateringHubList != null) {
            ecoWateringHubList.clear();
            requireActivity().runOnUiThread(ecoWateringHubAdapter::notifyDataSetChanged);
            for(EcoWateringHub hub : helpEcoWateringHubList) {
                ecoWateringHubList.add(hub);
                requireActivity().runOnUiThread(() -> ecoWateringHubAdapter.notifyDataSetChanged());
            }
            Common.hideLoadingFragment(view, R.id.mainFrameLayout, R.id.includeLoadingFragment);
        }
    }

    private void uploadEcoWateringHubList(@NonNull View view) {
        ArrayList<EcoWateringHub> hubListHelper = new ArrayList<>(); // AUXILIARY ARRAY LIST
        EcoWateringDevice.getEcoWateringDeviceJsonString(Common.getThisDeviceID(requireContext()), (jsonDeviceResponse) -> {
            MainActivity.thisEcoWateringDevice = new EcoWateringDevice(jsonDeviceResponse);
            // NO HUB CONNECTED CASE
            if(MainActivity.thisEcoWateringDevice.getEcoWateringHubList().isEmpty() || (MainActivity.thisEcoWateringDevice.getEcoWateringHubList() == null)) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else {
                for(String hubID : MainActivity.thisEcoWateringDevice.getEcoWateringHubList()) {
                    EcoWateringHub.getEcoWateringHubJsonString(hubID, (jsonHubResponse) -> {
                        hubListHelper.add(new EcoWateringHub(jsonHubResponse));
                        // LAST HUB CASE
                        if(hubListHelper.size() == MainActivity.thisEcoWateringDevice.getEcoWateringHubList().size()) {
                            hubListHelper.sort(new EcoWateringHubComparator());
                            ecoWateringHubList.clear();
                            requireActivity().runOnUiThread(ecoWateringHubAdapter::notifyDataSetChanged);
                            for(int i=0; i<hubListHelper.size(); i++) {
                                ecoWateringHubList.add(hubListHelper.get(i));
                                requireActivity().runOnUiThread(ecoWateringHubAdapter::notifyDataSetChanged);
                            }
                            requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.mainFrameLayout, R.id.includeLoadingFragment));
                        }
                    });
                }
            }
        });
    }

    private void manageEWHub(int position) {
        Bundle b = new Bundle();
        b.putParcelable(Common.MANAGE_EWH_INTENT_OBJ, ecoWateringHubList.get(position));
        Intent manageEWHIntent = new Intent(requireContext(), ManageEWHubActivity.class);
        manageEWHIntent.putExtra(Common.MANAGE_EWH_INTENT_OBJ, b);
        startActivity(manageEWHIntent);
        requireActivity().finish();
    }

    private void showDisconnectPopUpMenu(@NonNull View view, int position) {
        String hubID = ecoWateringHubList.get(position).getDeviceID();
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
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.disconnect_from_ewh_confirm_title))
                .setMessage(getString(R.string.disconnect_from_ewh_confirm_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> MainActivity.thisEcoWateringDevice.disconnectFromEWHub(requireContext(), hubID, (response) -> {
                            if(response != null && response.equals(Common.REMOVE_REMOTE_DEVICE_RESPONSE)) {
                                requireActivity().runOnUiThread(this::showDeviceDisconnectedDialog);
                            }
                            else {
                                requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                            }
                        }))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void showDeviceDisconnectedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.disconnected_from_ewh_success_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
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
