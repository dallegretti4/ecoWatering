package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.app.AlertDialog;
import android.content.Context;
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
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;

public class ManageHubManualControlFragment extends Fragment {
    private static final int REFRESH_FRAGMENT_FROM_HUB_INTERVAL = 3 * 1000;
    private static final int WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION = 5 * 1000;
    private static String calledFrom;
    private static EcoWateringHub hub;
    private final int primary_color;
    private final int primary_color_50;
    private OnHubManualActionChosenCallback onHubManualActionChosenCallback;
    public interface OnHubManualActionChosenCallback {
        void onBackPressedFromManageHubManual();
        void refreshManageHubManualFragment();
        void onManualSecondToolbarFunctionChosen();
        void manageConnectedRemoteDevices();
        void automateEcoWateringSystem();
        void setIrrigationSystemState(boolean value);
        void setDataObjectRefreshing(boolean value);
        void configureSensor(String sensorType);
        void forceSensorsUpdate();
        void restartApp();
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onHubManualActionChosenCallback.onBackPressedFromManageHubManual();
        }
    };
    private Handler refreshManualControlFragmentHandler;
    private Runnable refreshManualControlFragmentRunnable;
    private static boolean isRefreshingFragmentRunning = false;
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
        if(context instanceof OnHubManualActionChosenCallback) {
            this.onHubManualActionChosenCallback = (OnHubManualActionChosenCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(Common.THIS_LOG, "onDetach()");
        this.onHubManualActionChosenCallback = null;
        Common.unlockLayout(requireActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Common.lockLayout(requireActivity());
        Common.showLoadingFragment(view, R.id.manageHubManualControlFragmentContainer, R.id.includeLoadingFragment);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        if(savedInstanceState == null) {
            EcoWateringHub.getEcoWateringHubJsonString(hub.getDeviceID(), (jsonResponse) -> {
                hub = new EcoWateringHub(jsonResponse);
                requireActivity().runOnUiThread(() -> {
                    fragmentLayoutSetup(view);
                    Common.hideLoadingFragment(view, R.id.manageHubManualControlFragmentContainer, R.id.includeLoadingFragment);
                    Common.unlockLayout(requireActivity());
                });
            });
        }
        else {  // CONFIGURATION CHANGED CASE
            fragmentLayoutSetup(view);
            Common.hideLoadingFragment(view, R.id.manageHubManualControlFragmentContainer , R.id.includeLoadingFragment);
            Common.unlockLayout(requireActivity());
            if(isSwitchIrrigationSystemDialogVisible) showSwitchIrrigationSystemDialog(view, !hub.getIrrigationSystem().getState());
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> {
            try {Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION);}
            catch (InterruptedException e) {throw new RuntimeException(e);}
            // AUTO REFRESH FRAGMENT
            this.refreshManualControlFragmentHandler = new Handler(Looper.getMainLooper());
            this.refreshManualControlFragmentRunnable = new Runnable() {
                @Override
                public void run() {
                    if(!hub.isAutomated()) {
                        Log.i(Common.THIS_LOG, "refreshManageHubFragment runnable");
                        new Thread(() -> { if(onHubManualActionChosenCallback != null) onHubManualActionChosenCallback.forceSensorsUpdate(); }).start();
                        EcoWateringHub.getEcoWateringHubJsonString(hub.getDeviceID(), (jsonResponse) -> {
                            hub = new EcoWateringHub(jsonResponse);
                            if(getView() != null) requireActivity().runOnUiThread(() -> fragmentLayoutSetup(getView()));
                        });
                        // REPEAT RUNNABLE
                        if(isRefreshingFragmentRunning) refreshManualControlFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FROM_HUB_INTERVAL);
                    }
                    else {  // OTHER DEVICE SWITCH TO AUTOMATE CONTROL SYSTEM
                        if(onHubManualActionChosenCallback != null) onHubManualActionChosenCallback.restartApp();
                    }
                }
            };
            isRefreshingFragmentRunning = true;
            this.refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(Common.THIS_LOG, "onPause()");
        isRefreshingFragmentRunning = false;
        if(this.refreshManualControlFragmentHandler != null) this.refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
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
                        onHubManualActionChosenCallback.refreshManageHubManualFragment();
                    }
                    else if((calledFrom.equals(Common.CALLED_FROM_HUB) && (itemId == R.id.userProfileItem)) ||
                            (calledFrom.equals(Common.CALLED_FROM_DEVICE) && (itemId == android.R.id.home))) {
                        onHubManualActionChosenCallback.onManualSecondToolbarFunctionChosen();
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
        weatherImageView.setImageResource(WeatherInfo.getWeatherImageResourceId(hub.getWeatherInfo().getWeatherCode()));
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
        ((TextView) view.findViewById(R.id.precipitationLabelTextView)).setText(getString(WeatherInfo.getPrecipitationStringResourceId(hub.getWeatherInfo().getPrecipitation())));
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
        remoteDevicesConnectedCard.setOnClickListener((v) -> this.onHubManualActionChosenCallback.manageConnectedRemoteDevices());
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleIrrigationSystemCard).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        TextView irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(null);
        if(hub.getIrrigationSystem().getState()) {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
            irrigationSystemStateSwitchCompat.setChecked(true);
        }
        else {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
            irrigationSystemStateSwitchCompat.setChecked(false);
        }
        // NEED TO HIDE LOADING CARD AFTER STATE CHANGED CASE
        if(view.findViewById(R.id.loadingCard).getVisibility() == View.VISIBLE) {
            view.findViewById(R.id.loadingCard).setVisibility(View.GONE);
        }
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(
                (buttonView, isChecked) -> requireActivity().runOnUiThread(() -> showSwitchIrrigationSystemDialog(view, isChecked))
        );
    }

    private void automateEcoWateringSystemCardSetup(@NonNull View view) {
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onHubManualActionChosenCallback.automateEcoWateringSystem();
                buttonView.setChecked(false);
            }
        });
    }

    private void backgroundRefreshCardSetup(@NonNull View view) {
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.backgroundRefreshSwitch);
        backgroundRefreshSwitch.setOnCheckedChangeListener(null);
        backgroundRefreshSwitch.setChecked(hub.isDataObjectRefreshing());
        backgroundRefreshSwitch.setEnabled(true);

        backgroundRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TO STOP FRAGMENT REFRESHING
            if(this.refreshManualControlFragmentHandler != null) this.refreshManualControlFragmentHandler.removeCallbacks(this.refreshManualControlFragmentRunnable);
            // BACKGROUND REFRESHING SETTING
            new Thread(() -> {
                try {Thread.sleep(1000);}
                catch (InterruptedException e) {e.printStackTrace();}
                if(isChecked && !hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null)) {
                    onHubManualActionChosenCallback.setDataObjectRefreshing(true);
                }
                else if (!isChecked && hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null)) {
                    onHubManualActionChosenCallback.setDataObjectRefreshing(false);
                }
            }).start();
            // TO RESTART FRAGMENT REFRESHING AFTER BACKGROUND REFRESHING SETTING
            new Thread(() -> {
                try {Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION);}
                catch (InterruptedException e) {e.printStackTrace();}
                if((this.refreshManualControlFragmentHandler != null) && (this.refreshManualControlFragmentRunnable != null))
                    this.refreshManualControlFragmentHandler.post(this.refreshManualControlFragmentRunnable); // TO RESTART FRAGMENT REFRESHING AFTER TIME
            }).start();
        });
    }

    private void configurationCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleConfigurationCards).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme())); // TITLE CARD
        // AMBIENT TEMPERATURE SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) || (hub.getSensorInfo().getAmbientTemperatureChosenSensor() == null)) {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.ambientTemperatureSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.ambientTemperatureSensorCard).setOnClickListener((v) -> onHubManualActionChosenCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));
        // LIGHT SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) || (hub.getSensorInfo().getLightChosenSensor() == null)) {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.lightSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.lightSensorCard).setOnClickListener((v) -> onHubManualActionChosenCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT));
        // RELATIVE HUMIDITY SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) ||
                (hub.getSensorInfo().getRelativeHumidityChosenSensor() == null)) {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.GONE);
        }
        view.findViewById(R.id.relativeHumiditySensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        view.findViewById(R.id.relativeHumiditySensorCard).setOnClickListener((v) -> onHubManualActionChosenCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
    }

    private void showSwitchIrrigationSystemDialog(@NonNull View view, boolean isChecked) {
        isSwitchIrrigationSystemDialogVisible = true;
        String title = getString(R.string.irrigation_system_switch_off_dialog);
        if(isChecked) { title = getString(R.string.irrigation_system_switch_on_dialog); }
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setPositiveButton(getString(R.string.confirm_button), (dialogInterface, i) -> {
                        isSwitchIrrigationSystemDialogVisible = false;
                        requireActivity().runOnUiThread(() -> {
                            Common.lockLayout(requireActivity());
                            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                        });
                        // TO STOP FRAGMENT REFRESHING
                        if(refreshManualControlFragmentHandler != null) {
                            this.refreshManualControlFragmentHandler.removeCallbacks(this.refreshManualControlFragmentRunnable);
                        }
                        // IRRIGATION SYSTEM STATE SETTING
                        new Thread(() -> {
                            try {Thread.sleep(1000);}
                            catch (InterruptedException ignored) {}
                            onHubManualActionChosenCallback.setIrrigationSystemState(isChecked);
                        }).start();
                        //TO RESTART FRAGMENT REFRESHING AFTER SYSTEM STATE SETTING
                        new Thread(() -> {
                            try { Thread.sleep(WAITING_TIME_AFTER_FRAGMENT_RESTART_REFRESHING_ACTION); }
                            catch (InterruptedException ignored) {}
                            Common.unlockLayout(requireActivity());
                            if(this.refreshManualControlFragmentHandler != null) this.refreshManualControlFragmentHandler.post(this.refreshManualControlFragmentRunnable);
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
                            onHubManualActionChosenCallback.restartApp();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        onHubManualActionChosenCallback.restartApp();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
