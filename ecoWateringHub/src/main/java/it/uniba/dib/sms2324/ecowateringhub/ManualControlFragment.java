package it.uniba.dib.sms2324.ecowateringhub;

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
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.service.PersistentService;

public class ManualControlFragment extends Fragment {
    private static boolean isRefreshFragment = false;
    private final Handler refreshManualControlFragmentHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshManualControlFragmentRunnable = new Runnable() {
        @Override
        public void run() {
            EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
                MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
                MainActivity.forceSensorsUpdate(requireContext(), () -> {
                    isRefreshFragment = true;
                    Log.i(Common.THIS_LOG, "ManualControlFragment -> refreshRunnable");
                    requireActivity().runOnUiThread(() -> {
                        Common.showLoadingFragment(requireView(), R.id.manualControlFragmentContainer, R.id.includeLoadingFragment);
                        weatherCardSetup(requireView());
                        relativeHumidityLightCardSetup(requireView());
                        remoteDevicesConnectedCardSetup(requireView());
                        irrigationSystemCardSetup(requireView());
                        configurationCardSetup(requireView());
                        Common.hideLoadingFragment(requireView(), R.id.manualControlFragmentContainer, R.id.includeLoadingFragment);
                    });
                });
            });
            // REPEAT RUNNABLE
            refreshManualControlFragmentHandler.postDelayed(this, Common.REFRESH_FRAGMENT_FROM_HUB_INTERVAL);
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_manual_control_fragment, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == R.id.refreshItem) {
                onUserActionCallback.refreshManualControlFragment();
            }
            else if(itemId == R.id.userProfileItem) {
                onUserActionCallback.onUserProfileSelected();
            }
            return false;
        }
    };
    private TextView irrigationSystemValueStateTextView;
    private OnUserActionCallback onUserActionCallback;
    protected interface OnUserActionCallback {
        void onCardSelected(Class<?> cardActivityClass);
        void onAutomateIrrigationSystem();
        void refreshManualControlFragment();
        void onUserProfileSelected();
    }

    public ManualControlFragment() {
        super(R.layout.fragment_manual_control);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnUserActionCallback) {
            onUserActionCallback = (OnUserActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onUserActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().runOnUiThread(() -> Common.showLoadingFragment(view, R.id.manualControlFragmentContainer, R.id.includeLoadingFragment));
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        // FORCE SENSOR - VIEWs SETUP
        EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
            MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            MainActivity.forceSensorsUpdate(requireContext(), () -> {
                requireActivity().runOnUiThread(() -> weatherCardSetup(view));
                requireActivity().runOnUiThread(() -> relativeHumidityLightCardSetup(view));
                requireActivity().runOnUiThread(() -> remoteDevicesConnectedCardSetup(view));
                requireActivity().runOnUiThread(() -> irrigationSystemCardSetup(view));
                requireActivity().runOnUiThread(() -> configurationCardSetup(view));
                requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.manualControlFragmentContainer, R.id.includeLoadingFragment));
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.showLoadingFragment(requireView(), R.id.manualControlFragmentContainer, R.id.includeLoadingFragment);
        refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
    }

    private void weatherCardSetup(@NonNull View view) {
        ImageView weatherImageView = view.findViewById(R.id.weatherIconImageView);
        weatherImageView.setImageResource(MainActivity.thisEcoWateringHub.getWeatherInfo().getWeatherImageResourceId());
        TextView degreesTextView = view.findViewById(R.id.weatherStateFirstDegreesTextView);
        degreesTextView.setText(String.valueOf(((int) MainActivity.thisEcoWateringHub.getAmbientTemperature())));
        TextView addressTextView = view.findViewById(R.id.weatherStateAddressTextView);
        addressTextView.setText(MainActivity.thisEcoWateringHub.getPosition());
    }

    private void relativeHumidityLightCardSetup(@NonNull View view) {
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setText(String.valueOf((int)(MainActivity.thisEcoWateringHub.getRelativeHumidity())));
        TextView lightIndexTextView = view.findViewById(R.id.lightIndexTextView);
        lightIndexTextView.setText(String.valueOf((int)(MainActivity.thisEcoWateringHub.getIndexUV())));
    }

    private void remoteDevicesConnectedCardSetup(@NonNull View view) {
        TextView remoteDeviceConnectedNumberTextView = view.findViewById(R.id.remoteDeviceConnectedNumberTextView);
        remoteDeviceConnectedNumberTextView.setText(String.valueOf(MainActivity.thisEcoWateringHub.getRemoteDeviceList().size()));
        TextView remoteDeviceConnectedTextView = view.findViewById(R.id.remoteDeviceConnectedTextView);
        remoteDeviceConnectedTextView.setText(getResources().getQuantityString(
                it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                MainActivity.thisEcoWateringHub.getRemoteDeviceList().size(),
                MainActivity.thisEcoWateringHub.getRemoteDeviceList().size()));
        ConstraintLayout remoteDevicesConnectedCard = view.findViewById(R.id.remoteDevicesConnectedCard);
        remoteDevicesConnectedCard.setOnClickListener((v) -> onUserActionCallback.onCardSelected(ManageRemoteEWDevicesConnectedActivity.class));
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        if(MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getIrrigationSystem().getState()) {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
            irrigationSystemStateSwitchCompat.setChecked(true);
        }
        else {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
            irrigationSystemStateSwitchCompat.setChecked(false);
        }
        if(!isRefreshFragment) {
            irrigationSystemStateSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked) {
                    MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            MainActivity.thisEcoWateringHub.getDeviceID(),
                            true,
                            (requestedState, outcome) -> {
                                if(requestedState && outcome) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
                                        showStateSwitchedOnDialog();
                                    });
                                }
                                else if(!requestedState && outcome) {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            });
                }
                else {
                    MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            MainActivity.thisEcoWateringHub.getDeviceID(),
                            false,
                            ((requestedState, outcome) -> {
                                if(!requestedState && outcome) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
                                        showStateSwitchedOffDialog();
                                    });
                                }
                                else {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            })
                    );
                }
            });
        }
        else {
            isRefreshFragment = false;
        }
    }

    private void configurationCardSetup(@NonNull View view) {
        ConstraintLayout configurationCard = view.findViewById(R.id.configurationCard);
        configurationCard.setOnClickListener((v) -> onUserActionCallback.onCardSelected(ManualEcoWateringConfigurationActivity.class));
        // AUTOMATE SYSTEM SETUP
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onUserActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });

        // SENSORS AND WEATHER REALTIME REFRESH SETUP
        SwitchCompat realTimeSensorsWeatherRefreshSwitch = view.findViewById(R.id.realTimeSensorsWeatherRefreshSwitch);
        realTimeSensorsWeatherRefreshSwitch.setChecked(MainActivity.isPersistentServiceRunning(requireActivity()));
        realTimeSensorsWeatherRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked && !MainActivity.isPersistentServiceRunning(requireActivity())) {
                MainActivity.startPersistentServiceWork(requireActivity(), requireContext());
            }
            else if (!isChecked && MainActivity.isPersistentServiceRunning(requireActivity())) {
                requireActivity().stopService(new Intent(requireContext(), PersistentService.class));
                MainActivity.setServiceStateInSharedPreferences(requireActivity(), PersistentService.PERSISTENT_SERVICE_IS_NOT_RUNNING_FROM_SHARED_PREFERENCE);
            }
        });

        // SENSORS CONFIGURATION STATE SETUP
        if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {

            TextView sensorsConfigurationStateTextView = view.findViewById(R.id.sensorsConfigurationStateTextView);
            sensorsConfigurationStateTextView.setText(getString(R.string.sensors_configuration_complete));
            ConstraintLayout sensorsConfigurationStateCard = view.findViewById(R.id.sensorsConfigurationStateCard);
            sensorsConfigurationStateCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
            ImageView sensorsConfigurationStateImageView = view.findViewById(R.id.sensorsConfigurationStateImageView);
            sensorsConfigurationStateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireActivity().getTheme()));
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
                        ((dialogInterface, i) -> dialogInterface.dismiss())
                )
                .setCancelable(false)
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
                        ((dialogInterface, i) -> dialogInterface.dismiss())
                )
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    private void showHttpErrorFaultDialog() {
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