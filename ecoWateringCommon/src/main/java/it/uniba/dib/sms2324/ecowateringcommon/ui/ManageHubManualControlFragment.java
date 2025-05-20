package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
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

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class ManageHubManualControlFragment extends Fragment {
    private static final int REFRESH_FRAGMENT_FROM_HUB_INTERVAL = 3 * 1000;
    private static final int WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION = 4 * 1000;
    private static String calledFrom;
    private static EcoWateringHub hub;
    private final int primary_color;
    private final int primary_color_50;
    private OnHubActionChosenCallback onHubActionChosenCallback;
    public interface OnHubActionChosenCallback {
        void onBackPressedFromManageHub();
        void refreshFragment();
        void onSecondToolbarFunctionChosen();
        void manageConnectedRemoteDevices();
        void automateEcoWateringSystem();
        void setIrrigationSystemState(boolean value);
        void setDataObjectRefreshing(boolean value);
        void configureSensor(int sensorType);
        void forceSensorsUpdate(Common.OnMethodFinishCallback callback);
        void restartApp();
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onHubActionChosenCallback.onBackPressedFromManageHub();
        }
    };
    private final Handler refreshManualControlFragmentHandler = new Handler(Looper.getMainLooper());
    private static boolean isSwitchIrrigationSystemDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;

    public ManageHubManualControlFragment() {
        this(calledFrom, hub);
    }

    public ManageHubManualControlFragment(String calledFromString, EcoWateringHub hubObj) {
        super(R.layout.fragment_manage_hub_manual_control);
        calledFrom = calledFromString;
        hub = hubObj;
        if(calledFromString.equals(Common.CALLED_FROM_HUB)) {
            this.primary_color = R.color.ew_primary_color_from_hub;
            this.primary_color_50 = R.color.ew_primary_color_50_from_hub;
        }
        else {
            this.primary_color = R.color.ew_primary_color_from_device;
            this.primary_color_50 = R.color.ew_primary_color_50_from_device;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(Common.THIS_LOG, "onAttach()");
        if(context instanceof OnHubActionChosenCallback) {
            this.onHubActionChosenCallback = (OnHubActionChosenCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(Common.THIS_LOG, "onDetach()");
        this.onHubActionChosenCallback = null;
        Common.unlockLayout(requireActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Common.showLoadingFragment(view, R.id.manageHubManualControlFragmentContainer, R.id.includeLoadingFragment);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        if(savedInstanceState == null) {
            EcoWateringHub.getEcoWateringHubJsonString(hub.getDeviceID(), (jsonResponse) -> {
                hub = new EcoWateringHub(jsonResponse);
                requireActivity().runOnUiThread(() -> {
                    fragmentLayoutSetup(view);
                    Common.hideLoadingFragment(view, R.id.manageHubManualControlFragmentContainer, R.id.includeLoadingFragment);
                });
            });
        }
        else {  // CONFIGURATION CHANGED CASE
            fragmentLayoutSetup(view);
            Common.hideLoadingFragment(view, R.id.manageHubManualControlFragmentContainer , R.id.includeLoadingFragment);
            if(isSwitchIrrigationSystemDialogVisible) showSwitchIrrigationSystemDialog(!hub.getEcoWateringHubConfiguration().getIrrigationSystem().getState());
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }
    private final Runnable refreshManualControlFragmentRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(Common.THIS_LOG, "refreshManageHubFragment runnable");
            EcoWateringHub.getEcoWateringHubJsonString(hub.getDeviceID(), (jsonResponse) -> {
                hub = new EcoWateringHub(jsonResponse);
                if(onHubActionChosenCallback != null) {
                    onHubActionChosenCallback.forceSensorsUpdate(() -> {
                        if(getView() != null) requireActivity().runOnUiThread(() -> {
                            Common.unlockLayout(requireActivity());
                            fragmentLayoutSetup(getView());
                        });
                    });
                }
            });
            // REPEAT RUNNABLE
            refreshManualControlFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FROM_HUB_INTERVAL);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Log.i(Common.THIS_LOG, "onResume()");
        new Thread(() -> {
            try {Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION);}
            catch (InterruptedException e) {throw new RuntimeException(e);}
            // AUTO REFRESH FRAGMENT
            refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(Common.THIS_LOG, "onPause()");
        refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primary_color, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            if(calledFrom.equals(Common.CALLED_FROM_DEVICE)) {  // HOME ICON SETUP
                view.findViewById(R.id.toolbarLogoImageView).setVisibility(View.GONE);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
                ((TextView) view.findViewById(R.id.toolbarTitleTextView)).setText(hub.getName());
            }
            requireActivity().addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    if(calledFrom.equals(Common.CALLED_FROM_HUB)) menuInflater.inflate(R.menu.menu_manage_hub_manual_control_for_hub, menu);
                    else menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
                }
                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemId = menuItem.getItemId();
                    if(itemId == R.id.refreshItem) {
                        onHubActionChosenCallback.refreshFragment();
                    }
                    else if((calledFrom.equals(Common.CALLED_FROM_HUB) && (itemId == R.id.userProfileItem)) ||
                            (calledFrom.equals(Common.CALLED_FROM_DEVICE) && (itemId == android.R.id.home))) {
                        onHubActionChosenCallback.onSecondToolbarFunctionChosen();
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void fragmentLayoutSetup(@NonNull View view) {
        weatherCardSetup(view);
        remoteDevicesConnectedCardSetup(view);
        irrigationSystemCardSetup(view);
        automateEcoWateringSystemCardSetup(view);
        backgroundRefreshCardSetup(view);
        configurationCardSetup(view);
    }

    private void weatherCardSetup(@NonNull View view) {
        // WEATHER IMAGE VIEW SETUP
        view.findViewById(R.id.weatherIconImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        ImageView weatherImageView = view.findViewById(R.id.weatherIconImageView);
        weatherImageView.setImageResource(hub.getWeatherInfo().getWeatherImageResourceId());
        // AMBIENT TEMPERATURE TEXT VIEW SETUP
        TextView degreesTextView = view.findViewById(R.id.weatherStateFirstDegreesTextView);
        degreesTextView.setText(String.valueOf(((int) hub.getAmbientTemperature())));
        // HUB ADDRESS TEXT VIEW SETUP
        TextView addressTextView = view.findViewById(R.id.weatherStateAddressTextView);
        addressTextView.setText(hub.getPosition());
        // RELATIVE HUMIDITY PERCENT TEXT VIEW SETUP
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        relativeHumidityPercentTextView.setText(String.valueOf((int)(hub.getRelativeHumidity())));
        // UV INDEX TEXT VIEW SETUP
        TextView uvIndexTextView = view.findViewById(R.id.lightIndexTextView);
        uvIndexTextView.setText(String.valueOf((int)(hub.getIndexUV())));
        uvIndexTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        // PRECIPITATION CARD SETUP
        ((TextView) view.findViewById(R.id.precipitationValueTextView)).setText(String.valueOf(hub.getWeatherInfo().getPrecipitation()));
        view.findViewById(R.id.precipitationValueContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        ((TextView) view.findViewById(R.id.precipitationLabelTextView)).setText(getString(hub.getWeatherInfo().getPrecipitationStringResourceId()));
    }

    private void remoteDevicesConnectedCardSetup(@NonNull View view) {
        // CONNECTED REMOTE DEVICES NUMBER TEXT VIEW SETUP
        TextView remoteDeviceConnectedNumberTextView = view.findViewById(R.id.remoteDeviceConnectedNumberTextView);
        remoteDeviceConnectedNumberTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        remoteDeviceConnectedNumberTextView.setText(String.valueOf(hub.getRemoteDeviceList().size()));
        // CONNECTED REMOTE DEVICES TITLE TEXT VIEW SETUP
        TextView remoteDeviceConnectedTextView = view.findViewById(R.id.remoteDeviceConnectedTextView);
        remoteDeviceConnectedTextView.setText(getResources().getQuantityString(
                it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                hub.getRemoteDeviceList().size(),
                hub.getRemoteDeviceList().size()));
        // GO TO CONNECTED REMOTE DEVICES FRAGMENT CARD SETUP
        ConstraintLayout remoteDevicesConnectedCard = view.findViewById(R.id.connectedRemoteDevicesCard);
        remoteDevicesConnectedCard.setOnClickListener((v) -> this.onHubActionChosenCallback.manageConnectedRemoteDevices());
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleIrrigationSystemCard).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        TextView irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(null);
        if(hub.getEcoWateringHubConfiguration().getIrrigationSystem().getState()) {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
            irrigationSystemStateSwitchCompat.setChecked(true);
        }
        else {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
            irrigationSystemStateSwitchCompat.setChecked(false);
        }
        // NEED TO HIDE LOADING FRAGMENT AFTER STATE CHANGED CASE
        if(view.findViewById(R.id.includeLoadingFragment).getVisibility() == View.VISIBLE) {
            Common.hideLoadingFragment(view, R.id.manageHubManualControlFragmentContainer , R.id.includeLoadingFragment);
        }
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(
                (buttonView, isChecked) -> requireActivity().runOnUiThread(() -> showSwitchIrrigationSystemDialog(isChecked))
        );
    }

    private void automateEcoWateringSystemCardSetup(@NonNull View view) {
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onHubActionChosenCallback.automateEcoWateringSystem();
                buttonView.setChecked(false);
            }
        });
    }

    private void backgroundRefreshCardSetup(@NonNull View view) {
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.backgroundRefreshSwitch);
        backgroundRefreshSwitch.setOnCheckedChangeListener(null);
        backgroundRefreshSwitch.setChecked(hub.getEcoWateringHubConfiguration().isDataObjectRefreshing());
        backgroundRefreshSwitch.setEnabled(true);

        backgroundRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable); // TO STOP FRAGMENT REFRESHING
            new Thread(() -> {
                try {Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION);}
                catch (InterruptedException e) {e.printStackTrace();}
                refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable); // TO RESTART FRAGMENT REFRESHING AFTER TIME
            }).start();
            // LOGIC
            new Thread(() -> {
                try {Thread.sleep(1000);}
                catch (InterruptedException e) {e.printStackTrace();}
                if(isChecked && !hub.getEcoWateringHubConfiguration().isDataObjectRefreshing()) {
                    onHubActionChosenCallback.setDataObjectRefreshing(true);
                }
                else if (!isChecked && hub.getEcoWateringHubConfiguration().isDataObjectRefreshing()) {
                    onHubActionChosenCallback.setDataObjectRefreshing(false);
                }
            }).start();
        });
    }

    private void configurationCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleConfigurationCards).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme())); // TITLE CARD
        // AMBIENT TEMPERATURE SENSOR CARD SETUP
        if((hub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() == null) ||
                (hub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() == null)) {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.ambientTemperatureSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.ambientTemperatureSensorCard).setOnClickListener((v) -> onHubActionChosenCallback.configureSensor(Sensor.TYPE_AMBIENT_TEMPERATURE));
        // LIGHT SENSOR CARD SETUP
        if((hub.getEcoWateringHubConfiguration().getLightSensor() == null) ||
                (hub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() == null)) {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.lightSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.lightSensorCard).setOnClickListener((v) -> onHubActionChosenCallback.configureSensor(Sensor.TYPE_LIGHT));
        // RELATIVE HUMIDITY SENSOR CARD SETUP
        if((hub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() == null) ||
                (hub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() == null)) {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.relativeHumiditySensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.relativeHumiditySensorCard).setOnClickListener((v) -> onHubActionChosenCallback.configureSensor(Sensor.TYPE_RELATIVE_HUMIDITY));
    }

    private void showSwitchIrrigationSystemDialog(boolean isChecked) {
        isSwitchIrrigationSystemDialogVisible = true;
        String title = getString(R.string.irrigation_system_switch_off_dialog);
        if(isChecked) { title = getString(R.string.irrigation_system_switch_on_dialog); }
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setPositiveButton(getString(R.string.confirm_button), (dialogInterface, i) -> {
                        isSwitchIrrigationSystemDialogVisible = false;
                        requireActivity().runOnUiThread(() -> {
                            Common.lockLayout(requireActivity());
                            Common.showLoadingFragment(requireView(), R.id.manageHubManualControlFragmentContainer, R.id.includeLoadingFragment);
                        });
                        refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
                        new Thread(() -> {
                            try { Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION); }
                            catch (InterruptedException e) {e.printStackTrace();}
                            refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
                        }).start();
                        new Thread(() -> {
                            try {Thread.sleep(1000);}
                            catch (InterruptedException e) {e.printStackTrace();}
                            onHubActionChosenCallback.setIrrigationSystemState(isChecked);
                        }).start();
                }).setNegativeButton(getString(R.string.close_button), (dialogInterface, i) -> {
                        isSwitchIrrigationSystemDialogVisible = false;
                        dialogInterface.dismiss();
                }).setCancelable(false)
                .show();
    }

    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            onHubActionChosenCallback.restartApp();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        onHubActionChosenCallback.restartApp();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
