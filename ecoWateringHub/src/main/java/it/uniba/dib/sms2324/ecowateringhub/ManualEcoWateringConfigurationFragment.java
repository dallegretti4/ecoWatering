package it.uniba.dib.sms2324.ecowateringhub;

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
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.service.PersistentService;

public class ManualEcoWateringConfigurationFragment extends Fragment {
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                requireActivity().finish();
            }
            else if(itemId == R.id.refreshItem) {
                requireActivity().runOnUiThread(() -> Common.showLoadingFragment(requireView(), R.id.manualEWConfigurationFragmentContainer, R.id.includeLoadingFragment));
                EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
                    MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
                    MainActivity.forceSensorsUpdate(
                            requireContext(),
                            () -> requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(requireView(), R.id.manualEWConfigurationFragmentContainer, R.id.includeLoadingFragment)));
                });
            }
            return false;
        }
    };
    private OnConfigurationUserActionCallback onConfigurationUserActionCallback;
    protected interface OnConfigurationUserActionCallback {
        void onAutomateIrrigationSystem();
        void configureSensor(String configureSensorType);
    }

    public ManualEcoWateringConfigurationFragment() {
        super(R.layout.fragment_manual_ew_configuration);
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
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.manual_ew_configuration_toolbar_title));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        // AUTOMATE SYSTEM SWITCH BUTTON SETUP
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onConfigurationUserActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });

        // SENSORS AND WEATHER REAL TIME REFRESH SETUP
        SwitchCompat realTimeSensorsWeatherRefreshSwitch = view.findViewById(R.id.realTimeSensorsWeatherRefreshSwitch);
        realTimeSensorsWeatherRefreshSwitch.setChecked(MainActivity.isPersistentServiceRunning(requireActivity()));
        realTimeSensorsWeatherRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked && !MainActivity.isPersistentServiceRunning(requireActivity())) {
                MainActivity.startPersistentServiceWork(requireActivity(), requireContext());
            }
            else if(!isChecked && MainActivity.isPersistentServiceRunning(requireActivity())) {
                requireActivity().stopService(new Intent(requireContext(), PersistentService.class));
                MainActivity.setServiceStateInSharedPreferences(requireActivity(), PersistentService.PERSISTENT_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE);
            }
        });

        // AMBIENT TEMPERATURE SENSOR SETUP
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
            ImageView ambientTemperatureSensorImageView = view.findViewById(R.id.ambientTemperatureSensorStateImageView);
            ambientTemperatureSensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            ambientTemperatureSensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button ambientTemperatureConfigureButton = view.findViewById(R.id.ambientTemperatureSensorConfigurationButton);
        ambientTemperatureConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(Common.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));

        // LIGHT SENSOR SETUP
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
            ImageView lightSensorImageView = view.findViewById(R.id.lightSensorStateImageView);
            lightSensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            lightSensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button lightConfigureButton = view.findViewById(R.id.lightSensorConfigurationButton);
        lightConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(Common.CONFIGURE_SENSOR_TYPE_LIGHT));

        // RELATIVE HUMIDITY SENSOR SETUP
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
            ImageView relativeHumiditySensorImageView = view.findViewById(R.id.relativeHumiditySensorStateImageView);
            relativeHumiditySensorImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            relativeHumiditySensorImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
        }
        Button relativeHumidityConfigureButton = view.findViewById(R.id.relativeHumiditySensorConfigurationButton);
        relativeHumidityConfigureButton.setOnClickListener((v) -> onConfigurationUserActionCallback.configureSensor(Common.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
    }
}
