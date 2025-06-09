package it.uniba.dib.sms2324.ecowateringcommon.ui.hub;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public abstract class ManageHubFragment extends Fragment {
    public static final long DATA_OBJECT_REFRESHING_DURATION = 1500;
    protected static final long STOP_TIME_AFTER_STATE_CHANGE = 3 * 1000;
    protected static final long REFRESH_FRAGMENT_FREQUENCY = 5 * 1000;
    protected static String calledFrom;
    protected static EcoWateringHub hub;
    protected static boolean isRefreshManageHubFragmentRunning;
    protected final int primary_color;
    protected final int primary_color_50;
    protected final int primary_color_70;
    private OnManageHubActionCallback onManageHubActionCallback;
    public interface OnManageHubActionCallback {
        void onManageHubBackPressed();
        void onManageHubRefreshFragment();
        void onManualSecondToolbarFunctionChosen();
        void refreshDataObject(EcoWateringHub.OnEcoWateringHubGivenCallback callback);
        void manageConnectedRemoteDevices();
        void configureSensor(String sensorType);
        void restartApp();
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if(onManageHubActionCallback != null) onManageHubActionCallback.onManageHubBackPressed();
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            if(calledFrom.equals(Common.CALLED_FROM_HUB)) menuInflater.inflate(R.menu.menu_manage_hub_manual_control_for_hub, menu);
            else menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();

            if(itemId == R.id.refreshItem) onManageHubActionCallback.onManageHubRefreshFragment();
            else if(((calledFrom.equals(Common.CALLED_FROM_HUB)) && (itemId == R.id.userProfileItem)) ||
                    ((calledFrom.equals(Common.CALLED_FROM_DEVICE)) && (itemId == android.R.id.home))) {
                onManageHubActionCallback.onManualSecondToolbarFunctionChosen();
            }
            return false;
        }
    };

    public ManageHubFragment() {
        this(calledFrom, hub);
    }

    public ManageHubFragment(String calledFromString, @NonNull EcoWateringHub hubObj) {
        super(R.layout.fragment_manage_hub);
        calledFrom = calledFromString;
        hub = hubObj;
        if(calledFromString.equals(Common.CALLED_FROM_HUB)) {
            this.primary_color = R.color.ew_primary_color_from_hub;
            this.primary_color_50 = R.color.ew_primary_color_50_from_hub;
            this.primary_color_70 = R.color.ew_primary_color_70_from_hub;
        }
        else {
            this.primary_color = R.color.ew_primary_color_from_device;
            this.primary_color_50 = R.color.ew_primary_color_50_from_device;
            this.primary_color_70 = R.color.ew_primary_color_70_from_device;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnManageHubActionCallback) {
            this.onManageHubActionCallback = (OnManageHubActionCallback) context;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        this.onManageHubActionCallback = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Common.showLoadingFragment(view, R.id.manageHubFragmentContainer, R.id.includeLoadingFragment);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        if(savedInstanceState == null) {
            EcoWateringHub.getEcoWateringHubJsonString(hub.getDeviceID(), (jsonResponse) -> {
                hub = new EcoWateringHub(jsonResponse);
                requireActivity().runOnUiThread(() -> manageHubViewSetup(view));
                requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manageHubFragmentContainer, R.id.includeLoadingFragment));
                Common.unlockLayout(requireActivity());
            });
        }
        else {
            requireActivity().runOnUiThread(() -> manageHubViewSetup(view));
            requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manageHubFragmentContainer, R.id.includeLoadingFragment));
            Common.unlockLayout(requireActivity());
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        isRefreshManageHubFragmentRunning = true;
        this.refreshManageHubFragmentHandler = new Handler(Looper.getMainLooper());
        this.refreshManageHubFragmentHandler.post(this.refreshManageHubFragmentRunnable);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRefreshManageHubFragmentRunning = false;
        this.refreshManageHubFragmentHandler.removeCallbacks(this.refreshManageHubFragmentRunnable);
    }
    protected Handler refreshManageHubFragmentHandler;
    protected final Runnable refreshManageHubFragmentRunnable = getRefreshManageHubFragmentRunnable();

    protected void manageHubViewSetup(@NonNull View view) {
        weatherCardSetup(view);
        remoteDevicesConnectedCardSetup(view);
        configurationCardSetup(view);
        specializedViewSetup();
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primary_color, requireActivity().getTheme())); // TOOLBAR COLOR
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);    // HIDE TITLE
            if(calledFrom.equals(Common.CALLED_FROM_DEVICE)) {  // HOME ICON SETUP
                view.findViewById(R.id.toolbarLogoImageView).setVisibility(View.GONE);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
                ((TextView) view.findViewById(R.id.toolbarTitleTextView)).setText(hub.getName());
            }
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
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
        remoteDevicesConnectedCard.setOnClickListener((v) -> this.onManageHubActionCallback.manageConnectedRemoteDevices());
    }

    private void configurationCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleConfigurationCards).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme())); // TITLE CARD COLOR
        
        // AMBIENT TEMPERATURE SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) || (hub.getSensorInfo().getAmbientTemperatureChosenSensor() == null)) {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
            view.findViewById(R.id.ambientTemperatureSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_70, requireContext().getTheme())); // TITLE CARD COLOR
        }
        else {
            view.findViewById(R.id.ambientTemperatureSensorNotConfiguredNotification).setVisibility(View.GONE);
            view.findViewById(R.id.ambientTemperatureSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme())); // TITLE CARD COLOR
        }
        view.findViewById(R.id.ambientTemperatureSensorCard).setOnClickListener((v) -> this.onManageHubActionCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));

        // LIGHT SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) || (hub.getSensorInfo().getLightChosenSensor() == null)) {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.VISIBLE);
            view.findViewById(R.id.lightSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_70, requireActivity().getTheme()));
        }
        else {
            view.findViewById(R.id.lightSensorNotConfiguredNotification).setVisibility(View.GONE);
            view.findViewById(R.id.lightSensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireActivity().getTheme()));
        }
        view.findViewById(R.id.lightSensorCard).setOnClickListener((v) -> this.onManageHubActionCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT));

        // RELATIVE HUMIDITY SENSOR CARD SETUP
        if((hub.getSensorInfo() == null) || (hub.getSensorInfo().getRelativeHumidityChosenSensor() == null)) {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.VISIBLE);
            view.findViewById(R.id.relativeHumiditySensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_70, requireContext().getTheme()));
        }
        else {
            view.findViewById(R.id.relativeHumiditySensorNotConfiguredNotification).setVisibility(View.GONE);
            view.findViewById(R.id.relativeHumiditySensorImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        }
        view.findViewById(R.id.relativeHumiditySensorCard).setOnClickListener((v) -> this.onManageHubActionCallback.configureSensor(SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
    }
    protected abstract void specializedViewSetup();
    protected abstract Runnable getRefreshManageHubFragmentRunnable();
}
