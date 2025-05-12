package it.uniba.dib.sms2324.ecowateringhub.entry;

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

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.service.PersistentService;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.connection.ManageConnectedRemoteEWDevicesActivity;
import it.uniba.dib.sms2324.ecowateringhub.configuration.EcoWateringConfigurationActivity;

public class ManualControlFragment extends Fragment {
    private static final int REFRESH_FRAGMENT_FROM_HUB_INTERVAL = 5 * 1000;
    private static final String IRRIGATION_SYSTEM_STATE_CHANGED_OUT_STATE = "IRRIGATION_SYSTEM_STATE_CHANGED_OUT_STATE";
    private static boolean isRefreshFragment = false;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            requireActivity().finish();
        }
    };
    private final Handler refreshManualControlFragmentHandler = new Handler(Looper.getMainLooper());
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
    public interface OnUserActionCallback {
        void onCardSelected(Class<?> cardActivityClass);
        void onAutomateIrrigationSystem();
        void refreshManualControlFragment();
        void onUserProfileSelected();
    }
    private final Runnable refreshManualControlFragmentRunnable = new Runnable() {
        @Override
        public void run() {
            EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
                MainActivity.setThisEcoWateringHub(new EcoWateringHub(jsonResponse));
                MainActivity.forceSensorsUpdate(requireContext(), () -> {
                    isRefreshFragment = true;
                    if(getView() != null) {
                        Log.i(Common.THIS_LOG, "ManualControlFragment -> refreshRunnable");
                        requireActivity().runOnUiThread(() -> {
                            weatherCardSetup(requireView());
                            relativeHumidityLightCardSetup(requireView());
                            remoteDevicesConnectedCardSetup(requireView());
                            irrigationSystemCardSetup(requireView());
                            configurationCardSetup(requireView());
                        });
                    }
                });
            });
            // REPEAT RUNNABLE
            refreshManualControlFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FROM_HUB_INTERVAL);
        }
    };
    private static boolean isStateChangedDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;

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
        Common.showLoadingFragment(view, R.id.manualControlFragmentContainer , R.id.includeLoadingFragment);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);

        if(savedInstanceState == null) {    // FORCE SENSOR - VIEWs SETUP
            EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> {
                MainActivity.setThisEcoWateringHub(new EcoWateringHub(jsonResponse));
                MainActivity.forceSensorsUpdate(requireContext(), () -> requireActivity().runOnUiThread(() -> {
                        frameLayoutSetup(view);
                        Common.hideLoadingFragment(view, R.id.manualControlFragmentContainer , R.id.includeLoadingFragment);
                }));
            });
        }
        else {  // CONFIGURATION CHANGED CASE
            frameLayoutSetup(view);
            Common.hideLoadingFragment(view, R.id.manualControlFragmentContainer , R.id.includeLoadingFragment);
            if(isStateChangedDialogVisible) showStateChangedDialog(savedInstanceState.getBoolean(IRRIGATION_SYSTEM_STATE_CHANGED_OUT_STATE));
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // AUTO REFRESH FRAGMENT
        refreshManualControlFragmentHandler.post(refreshManualControlFragmentRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshManualControlFragmentHandler.removeCallbacks(refreshManualControlFragmentRunnable);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isStateChangedDialogVisible && MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getIrrigationSystem().getState()) {
            outState.putBoolean(IRRIGATION_SYSTEM_STATE_CHANGED_OUT_STATE, true);
        }
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void frameLayoutSetup(@NonNull View view) {
        weatherCardSetup(view);
        relativeHumidityLightCardSetup(view);
        remoteDevicesConnectedCardSetup(view);
        irrigationSystemCardSetup(view);
        configurationCardSetup(view);
    }

    private void weatherCardSetup(@NonNull View view) {
        ImageView weatherImageView = view.findViewById(R.id.weatherIconImageView);
        weatherImageView.setImageResource(MainActivity.getThisEcoWateringHub().getWeatherInfo().getWeatherImageResourceId());
        TextView degreesTextView = view.findViewById(R.id.weatherStateFirstDegreesTextView);
        degreesTextView.setText(String.valueOf(((int) MainActivity.getThisEcoWateringHub().getAmbientTemperature())));
        TextView addressTextView = view.findViewById(R.id.weatherStateAddressTextView);
        addressTextView.setText(MainActivity.getThisEcoWateringHub().getPosition());
    }

    private void relativeHumidityLightCardSetup(@NonNull View view) {
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setText(String.valueOf((int)(MainActivity.getThisEcoWateringHub().getRelativeHumidity())));
        TextView lightIndexTextView = view.findViewById(R.id.lightIndexTextView);
        lightIndexTextView.setText(String.valueOf((int)(MainActivity.getThisEcoWateringHub().getIndexUV())));
    }

    private void remoteDevicesConnectedCardSetup(@NonNull View view) {
        TextView remoteDeviceConnectedNumberTextView = view.findViewById(R.id.remoteDeviceConnectedNumberTextView);
        remoteDeviceConnectedNumberTextView.setText(String.valueOf(MainActivity.getThisEcoWateringHub().getRemoteDeviceList().size()));
        TextView remoteDeviceConnectedTextView = view.findViewById(R.id.remoteDeviceConnectedTextView);
        remoteDeviceConnectedTextView.setText(getResources().getQuantityString(
                it.uniba.dib.sms2324.ecowateringcommon.R.plurals.remote_devices_connected_plurals,
                MainActivity.getThisEcoWateringHub().getRemoteDeviceList().size(),
                MainActivity.getThisEcoWateringHub().getRemoteDeviceList().size()));
        ConstraintLayout remoteDevicesConnectedCard = view.findViewById(R.id.remoteDevicesConnectedCard);
        remoteDevicesConnectedCard.setOnClickListener((v) -> onUserActionCallback.onCardSelected(ManageConnectedRemoteEWDevicesActivity.class));
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        if(MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getIrrigationSystem().getState()) {
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
                    MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            Common.getThisDeviceID(requireContext()),
                            Common.getThisDeviceID(requireContext()),
                            true,
                            (requestedState, outcome) -> {
                                if(requestedState && outcome) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
                                        showStateChangedDialog(true);
                                    });
                                }
                                else if(!requestedState && outcome) {
                                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                                }
                            });
                }
                else {
                    MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getIrrigationSystem().setState(
                            Common.getThisDeviceID(requireContext()),
                            Common.getThisDeviceID(requireContext()),
                            false,
                            ((requestedState, outcome) -> {
                                if(!requestedState && outcome) {
                                    requireActivity().runOnUiThread(() -> {
                                        irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
                                        showStateChangedDialog(false);
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
        configurationCard.setOnClickListener((v) -> onUserActionCallback.onCardSelected(EcoWateringConfigurationActivity.class));
        // AUTOMATE SYSTEM SETUP
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                onUserActionCallback.onAutomateIrrigationSystem();
                buttonView.setChecked(false);
            }
        });

        // BACKGROUND REFRESH SETUP
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.backgroundRefreshSwitch);
        backgroundRefreshSwitch.setChecked(PersistentService.isPersistentServiceRunning(requireActivity()));
        backgroundRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked && !PersistentService.isPersistentServiceRunning(requireActivity())) {
                PersistentService.startPersistentService(requireActivity(), requireContext());
            }
            else if (!isChecked && PersistentService.isPersistentServiceRunning(requireActivity())) {
                PersistentService.stopPersistentService(requireActivity(), requireContext());
            }
        });

        // SENSORS CONFIGURATION STATE SETUP
        if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {

            TextView sensorsConfigurationStateTextView = view.findViewById(R.id.sensorsConfigurationStateTextView);
            sensorsConfigurationStateTextView.setText(getString(R.string.sensors_configuration_complete));
            ConstraintLayout sensorsConfigurationStateCard = view.findViewById(R.id.sensorsConfigurationStateCard);
            sensorsConfigurationStateCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme())));
            ImageView sensorsConfigurationStateImageView = view.findViewById(R.id.sensorsConfigurationStateImageView);
            sensorsConfigurationStateImageView.setBackground(ResourcesCompat.getDrawable(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.drawable.check_circle_icon, requireActivity().getTheme()));
        }
    }

    /**
     * Notify the user irrigation system is switched on/off.
     * Positive button close dialog.
     */
    private void showStateChangedDialog(boolean newState) {
        isStateChangedDialogVisible = true;
        String title;
        if(newState) {
            title = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.irrigation_system_turned_on);
        }
        else {
            title = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.irrigation_system_turned_off);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isStateChangedDialogVisible = false;
                            dialogInterface.dismiss();
                        })
                )
                .setCancelable(false)
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
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