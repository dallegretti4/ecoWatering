package it.uniba.dib.sms2324.ecowatering.control;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class ManageEWHubManualControlFragment extends Fragment {
    private static final int REFRESH_FRAGMENT_FROM_DEVICE_INTERVAL = 5 * 1000;
    private boolean isRefreshFragment = false;
    private final Handler refreshManualControlFragmentHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshManualControlFragmentRunnable = new Runnable() {
        @Override
        public void run() {
            EcoWateringHub.getEcoWateringHubJsonString(ManageEWHubActivity.selectedEWHub.getDeviceID(), (jsonResponse) -> {
                ManageEWHubActivity.selectedEWHub = new EcoWateringHub(jsonResponse);
                Log.i(Common.THIS_LOG, "manageEWHubManualControlFragment -> refreshRunnable");
                requireActivity().runOnUiThread(() -> {
                    Common.showLoadingFragment(requireView(), R.id.mainFrameLayout, R.id.includeLoadingFragment);
                    weatherCardSetup(requireView());
                    remoteDevicesConnectedCardSetup(requireView());
                    isRefreshFragment = true;
                    irrigationSystemCardSetup(requireView());
                    automateSystemCardSetup(requireView());
                    sensorsCardSetup(requireView());
                    Common.hideLoadingFragment(requireView(), R.id.mainFrameLayout, R.id.includeLoadingFragment);
                });
            });
            // REPEAT RUNNABLE
            refreshManualControlFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FROM_DEVICE_INTERVAL);
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(requireContext(), MainActivity.class));
            requireActivity().finish();
        }
    };
    private TextView irrigationSystemValueStateTextView;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemID = menuItem.getItemId();
            if(itemID == android.R.id.home) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else if(itemID == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                onManualActionCallback.onRefreshMenuItem();
            }
            return false;
        }
    };
    private OnUserActionCallback onManualActionCallback;
    protected interface OnUserActionCallback {
        void onAutomateIrrigationSystem();
        void onRemoteEWDevicesConnectedCardListener();
        void onRefreshMenuItem();
    }

    public ManageEWHubManualControlFragment() {
        super(R.layout.fragment_manage_ew_hub_manual_control);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnUserActionCallback) {
            onManualActionCallback = (OnUserActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onManualActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // ON BACK PRESSED CALLBACK SETUP
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        toolbarSetup(view);
        weatherCardSetup(view);
        remoteDevicesConnectedCardSetup(view);
        irrigationSystemCardSetup(view);
        // SET CONFIGURATION CARD
        automateSystemCardSetup(view);
        sensorsCardSetup(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            ((TextView) view.findViewById(R.id.hubNameTextView)).setText(ManageEWHubActivity.selectedEWHub.getName());
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void weatherCardSetup(@NonNull View view) {
        TextView addressTextView = view.findViewById(R.id.weatherStateAddressTextView);
        addressTextView.setText(ManageEWHubActivity.selectedEWHub.getPosition());
        TextView weatherStateFirstDegreesTextView = view.findViewById(R.id.weatherStateFirstDegreesTextView);

        weatherStateFirstDegreesTextView.setText(String.valueOf(((int) ManageEWHubActivity.selectedEWHub.getAmbientTemperature())));
        ImageView weatherIconImageView = view.findViewById(R.id.weatherIconImageView);
        weatherIconImageView.setImageResource(ManageEWHubActivity.selectedEWHub.getWeatherInfo().getWeatherImageResourceId());

        // RELATIVE HUMIDITY - LIGHT CARD SETUP
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setText(String.valueOf((int)(ManageEWHubActivity.selectedEWHub.getRelativeHumidity())));
        TextView lightIndexTextView = view.findViewById(R.id.lightIndexTextView);
        lightIndexTextView.setText(String.valueOf((int)(ManageEWHubActivity.selectedEWHub.getIndexUV())));
    }

    private void remoteDevicesConnectedCardSetup(@NonNull View view) {
        TextView remoteDeviceConnectedNumberTextView = view.findViewById(R.id.remoteDeviceConnectedNumberTextView);
        TextView remoteDeviceConnectedTextView = view.findViewById(R.id.remoteDeviceConnectedTextView);
        if((ManageEWHubActivity.selectedEWHub.getRemoteDeviceList() != null) && (!ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().isEmpty())) {
            remoteDeviceConnectedNumberTextView.setText(String.valueOf(ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().size()));
        }
        else {
            remoteDeviceConnectedNumberTextView.setText("0");
        }
        remoteDeviceConnectedTextView.setText(getResources().getQuantityString(
                it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().size(),
                ManageEWHubActivity.selectedEWHub.getRemoteDeviceList().size()));
        ConstraintLayout remoteDevicesConnectedCard = view.findViewById(R.id.remoteDevicesConnectedCard);
        remoteDevicesConnectedCard.setOnClickListener((v) -> onManualActionCallback.onRemoteEWDevicesConnectedCardListener());
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        if(ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getIrrigationSystem().getState()) {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
            irrigationSystemStateSwitchCompat.setChecked(true);
        }
        else {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
            irrigationSystemStateSwitchCompat.setChecked(false);
        }
        if(isRefreshFragment) {
            isRefreshFragment = false;
        }
        else {
            irrigationSystemStateSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked && !isRefreshFragment) {
                    ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            Common.getThisDeviceID(requireContext()),
                            ManageEWHubActivity.selectedEWHub.getDeviceID(),
                            true,
                            (requestedState, outcome) -> {
                                if(outcome && requestedState) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
                                        showStateSwitchedOnDialog();
                                    });
                                }
                                else {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            });
                }
                else if(!isChecked && !isRefreshFragment) {
                    ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            Common.getThisDeviceID(requireContext()),
                            ManageEWHubActivity.selectedEWHub.getDeviceID(),
                            false,
                            (requestedState, outcome) -> {
                                if(outcome && !requestedState) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
                                        showStateSwitchedOffDialog();
                                    });
                                }
                                else {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            });
                }
            });
        }
    }

    private void automateSystemCardSetup(@NonNull View view) {
        SwitchCompat irrigationSystemAutomateSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        irrigationSystemAutomateSwitchCompat.setChecked(false);
        irrigationSystemAutomateSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onManualActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });
    }

    private void sensorsCardSetup(@NonNull View view) {
        ImageView stateImageView;
        // AMBIENT TEMPERATURE SENSOR
        if((ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
            stateImageView = view.findViewById(R.id.ambientTemperatureSensorStateImageView);
            stateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            stateImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ewd_primary_color, requireActivity().getTheme())));
        }
        // LIGHT SENSOR
        if((ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
            stateImageView = view.findViewById(R.id.lightSensorStateImageView);
            stateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            stateImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ewd_primary_color, requireActivity().getTheme())));
        }
        // RELATIVE HUMIDITY SENSOR
        if((ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
            stateImageView = view.findViewById(R.id.relativeHumiditySensorStateImageView);
            stateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            stateImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ewd_primary_color, requireActivity().getTheme())));
        }
    }

    /**
     * Notify the user irrigation system is switched on.
     * Positive button close dialog.
     */
    private void showStateSwitchedOnDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.irrigation_system_turned_on))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }

    /**
     * Notify the user irrigation system is switched off.
     * Positive button close dialog.
     */
    private void showStateSwitchedOffDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.irrigation_system_turned_off))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        startActivity(new Intent(requireContext(), MainActivity.class));
                        requireActivity().finish();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
