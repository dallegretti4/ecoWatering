package it.uniba.dib.sms2324.ecowateringhub.configuration;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.EcoWateringSensor;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class EcoWateringConfigurationFragment extends Fragment {
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
            else if(itemId == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                requireActivity().runOnUiThread(() -> Common.showLoadingFragment(requireView(), R.id.manualEWConfigurationFragmentContainer, R.id.includeLoadingFragment));
                EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
                    MainActivity.setThisEcoWateringHub(new EcoWateringHub(jsonResponse));
                    MainActivity.forceSensorsUpdate(
                            requireContext(),
                            () -> requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(requireView(), R.id.manualEWConfigurationFragmentContainer, R.id.includeLoadingFragment)));
                });
            }
            return false;
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(requireContext(), MainActivity.class));
            requireActivity().finish();
        }
    };
    private OnConfigurationUserActionCallback onConfigurationUserActionCallback;
    protected interface OnConfigurationUserActionCallback {
        void onAutomateIrrigationSystem();
        void configureSensor(String configureSensorType);
    }

    public EcoWateringConfigurationFragment() {
        super(R.layout.fragment_ew_configuration);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnConfigurationUserActionCallback) {
            onConfigurationUserActionCallback = (OnConfigurationUserActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onConfigurationUserActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        automateSystemSwitchCompactSetup(view);
        backgroundRefreshSetup(view);
        // SENSORS SETUP
        ambientTemperatureSensorSetup(view);
        lightSensorSetup(view);
        relativeHumiditySensorSetup(view);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.manual_ew_configuration_toolbar_title));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void automateSystemSwitchCompactSetup(@NonNull View view) {
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onConfigurationUserActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });
    }

    private void backgroundRefreshSetup(@NonNull View view) {
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.backgroundRefreshSwitch);
        backgroundRefreshSwitch.setChecked(EcoWateringForegroundService.isEcoWateringServiceRunning(requireActivity()));
        backgroundRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked && !EcoWateringForegroundService.isEcoWateringServiceRunning(requireActivity())) {
                EcoWateringForegroundService.startEcoWateringService(MainActivity.getThisEcoWateringHub(), requireContext(), EcoWateringForegroundService.DATA_OBJECT_REFRESHING_SERVICE_TYPE);
            }
            else if(!isChecked && EcoWateringForegroundService.isEcoWateringServiceRunning(requireActivity())) {
                EcoWateringForegroundService.stopEcoWateringService(MainActivity.getThisEcoWateringHub(), requireContext(), EcoWateringForegroundService.DATA_OBJECT_REFRESHING_SERVICE_TYPE);
            }
        });
    }

    private void ambientTemperatureSensorSetup(@NonNull View view) {
        if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
            ImageView ambientTemperatureSensorImageView = view.findViewById(R.id.ambientTemperatureSensorStateImageView);
            ambientTemperatureSensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            ambientTemperatureSensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button ambientTemperatureConfigureButton = view.findViewById(R.id.ambientTemperatureSensorConfigurationButton);
        ambientTemperatureConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(EcoWateringSensor.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));
    }

    private void lightSensorSetup(@NonNull View view) {
        if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
            ImageView lightSensorImageView = view.findViewById(R.id.lightSensorStateImageView);
            lightSensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            lightSensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button lightConfigureButton = view.findViewById(R.id.lightSensorConfigurationButton);
        lightConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(EcoWateringSensor.CONFIGURE_SENSOR_TYPE_LIGHT));
    }

    private void relativeHumiditySensorSetup(@NonNull View view) {
        if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
            ImageView relativeHumiditySensorImageView = view.findViewById(R.id.relativeHumiditySensorStateImageView);
            relativeHumiditySensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            relativeHumiditySensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button relativeHumidityConfigureButton = view.findViewById(R.id.relativeHumiditySensorConfigurationButton);
        relativeHumidityConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(EcoWateringSensor.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
    }
}
