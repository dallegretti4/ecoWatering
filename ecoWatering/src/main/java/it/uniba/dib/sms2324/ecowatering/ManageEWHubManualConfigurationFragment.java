package it.uniba.dib.sms2324.ecowatering;

import android.content.Context;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
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

public class ManageEWHubManualConfigurationFragment extends Fragment {
    private OnConfigurationUserActionCallback onConfigurationUserActionCallback;
    protected interface OnConfigurationUserActionCallback {
        void onAutomateIrrigationSystem();
        void onRefreshConfigurationFragment();
    }

    public ManageEWHubManualConfigurationFragment() {
        super(R.layout.fragment_manage_eco_watering_manual_configuration);
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
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.manual_ew_configuration_toolbar_title));
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
                    int itemId = menuItem.getItemId();
                    if(itemId == android.R.id.home) {
                        ManageEWHubActivity.popBackStackFragment();
                    }
                    else if(itemId == R.id.refreshEWHubItem) {
                        onConfigurationUserActionCallback.onRefreshConfigurationFragment();
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        // TURN TO AUTOMATED CONTROL SETUP
        SwitchCompat irrigationSystemAutomateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        irrigationSystemAutomateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onConfigurationUserActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });

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
                ManageEWHubActivity.selectedEWHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null) {
            stateImageView = view.findViewById(R.id.relativeHumiditySensorStateImageView);
            stateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireContext().getTheme()));
            stateImageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ewd_primary_color, requireActivity().getTheme())));
        }
    }
}
