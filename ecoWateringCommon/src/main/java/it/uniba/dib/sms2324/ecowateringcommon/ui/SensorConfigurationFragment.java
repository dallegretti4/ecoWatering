package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsAdapter;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;

public class SensorConfigurationFragment extends Fragment {
    private static final String STRING_SENSOR_CHOSEN_KEY = "STRING_SENSOR_CHOSEN";
    private static final String SENSOR_ADDED_SUCCESSFULLY_RESPONSE = "sensorAddedSuccessfully";
    private static EcoWateringHub hub;
    private static String calledFrom;
    private static String sensorType;
    private final int primary_color;
    private final int primary_color_50;
    private String stringChosenSensor;
    private OnSensorConfigurationActionCallback onSensorConfigurationActionCallback;
    public interface OnSensorConfigurationActionCallback {
        void onSensorConfigurationGoBack();
        void onSensorConfigurationRefreshFragment(String sensorType);
        void onSensorConfigurationRestartApp();
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if(onSensorConfigurationActionCallback != null) onSensorConfigurationActionCallback.onSensorConfigurationGoBack();
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                onSensorConfigurationActionCallback.onSensorConfigurationGoBack();
            }
            else if(itemId == R.id.refreshItem) {
                onSensorConfigurationActionCallback.onSensorConfigurationRefreshFragment(sensorType);
            }
            return false;
        }
    };
    private static boolean isDetachSensorDialogVisible;
    private static boolean isSensorChosenDialogVisible;
    private static boolean isSensorAlreadyConfiguredDialogVisible;
    private static boolean isSensorConfiguredDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;
    private static boolean isSensorDetachedDialogVisible;
    public SensorConfigurationFragment() {
        this(hub, calledFrom, sensorType);
    }
    public SensorConfigurationFragment(@NonNull EcoWateringHub hubObj, String calledFromString, String sensorTypeInt) {
        super(R.layout.fragment_sensor_configuration);
        hub = hubObj;
        calledFrom = calledFromString;
        sensorType = sensorTypeInt;
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
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
        if(context instanceof OnSensorConfigurationActionCallback) {
            this.onSensorConfigurationActionCallback = (OnSensorConfigurationActionCallback) context;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        this.onSensorConfigurationActionCallback = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Common.unlockLayout(requireActivity());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        selectedSensorDetachButtonSetup(view);
        ((TextView) view.findViewById(R.id.sensorsConfigurationTitleTextView)).setText(String.format(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensors_configuration_title), sensorType));   // FRAGMENT TITLE SETUP
        sensorListViewSetup(view);
        // CONFIGURATION CHANGED CASE
        if(savedInstanceState != null) {
            if(isDetachSensorDialogVisible) showDetachSensorDialog();
            else if(isSensorDetachedDialogVisible) showSensorDetachedDialog();
            else if(isSensorAlreadyConfiguredDialogVisible) showSensorAlreadyConfiguredDialog();
            else if(isSensorConfiguredDialogVisible) showSensorConfiguredDialog();
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
            else if(isSensorChosenDialogVisible && !savedInstanceState.getString(STRING_SENSOR_CHOSEN_KEY, Common.NULL_STRING_VALUE).equals(Common.NULL_STRING_VALUE)) {
                showSensorChosenDialog(savedInstanceState.getString(STRING_SENSOR_CHOSEN_KEY));
            }
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isSensorChosenDialogVisible) outState.putString(STRING_SENSOR_CHOSEN_KEY, this.stringChosenSensor);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primary_color, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(sensorType);
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void selectedSensorDetachButtonSetup(@NonNull View view) {
        view.findViewById(R.id.selectedSensorTitleCard).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        Button selectedSensorDetachButton = view.findViewById(R.id.selectedSensorDetachButton);
        if(hub.getSensorInfo() != null) {
            ArrayList<String> sensorArrayListID = new ArrayList<>();
            switch (sensorType) {
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE:
                    if (hub.getSensorInfo().getAmbientTemperatureChosenSensor() != null) {
                        sensorArrayListID = new ArrayList<>(Arrays.asList(hub.getSensorInfo().getAmbientTemperatureChosenSensor().split(SensorsInfo.EW_SENSOR_ID_SEPARATOR)));
                    }
                    break;
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT:
                    if (hub.getSensorInfo().getLightChosenSensor() != null) {
                        sensorArrayListID = new ArrayList<>(Arrays.asList(hub.getSensorInfo().getLightChosenSensor().split(SensorsInfo.EW_SENSOR_ID_SEPARATOR)));
                    }
                    break;
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY:
                    if (hub.getSensorInfo().getRelativeHumidityChosenSensor() != null) {
                        sensorArrayListID = new ArrayList<>(Arrays.asList(hub.getSensorInfo().getRelativeHumidityChosenSensor().split(SensorsInfo.EW_SENSOR_ID_SEPARATOR)));
                    }
                    break;
                default: break;
            }
            if(!sensorArrayListID.isEmpty()) {
                ((TextView) view.findViewById(R.id.selectedSensorNameTextView)).setText(sensorArrayListID.get(1));
                ((TextView) view.findViewById(R.id.selectedSensorVendorTextView)).setText(sensorArrayListID.get(2));
                view.findViewById(R.id.selectedSensorCardDivisor).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog());
            }
        }
    }

    private void sensorListViewSetup(@NonNull View view) {
        ListView sensorListView = view.findViewById(R.id.sensorsListView);
        ArrayList<String> sensorHelperStringList = new ArrayList<>();
        if(hub.getSensorInfo() != null) {
            // SENSOR LIST RECOVERING
            switch (sensorType) {
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE:
                    if(hub.getSensorInfo().getAmbientTemperatureSensorList() != null) sensorHelperStringList = new ArrayList<>(hub.getSensorInfo().getAmbientTemperatureSensorList());
                    break;
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT:
                    if(hub.getSensorInfo().getLightSensorList() != null) sensorHelperStringList = new ArrayList<>(hub.getSensorInfo().getLightSensorList());
                    break;
                case SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY:
                    if(hub.getSensorInfo().getRelativeHumiditySensorList() != null) sensorHelperStringList = new ArrayList<>(hub.getSensorInfo().getRelativeHumiditySensorList());
                    break;
                default: break;
            }
            // AT LEAST ONE SENSOR CASE
            if((!sensorHelperStringList.isEmpty())) {
                ArrayList<String> sensorStringList = new ArrayList<>();
                SensorsAdapter sensorsListAdapter = new SensorsAdapter(requireContext(), sensorStringList, sensorType, calledFrom);
                sensorListView.setAdapter(sensorsListAdapter);
                for(String stringSensor : sensorHelperStringList) {
                    sensorStringList.add(stringSensor);
                    sensorsListAdapter.notifyDataSetChanged();
                }
                sensorListView.setOnItemClickListener((adapterView, v, position, l) -> showSensorChosenDialog(sensorStringList.get(position)));
            }
            else {  // EMPTY CASE
                sensorListView.setVisibility(View.GONE);
                view.findViewById(R.id.sensorsConfigurationTitleTextView).setVisibility(View.GONE);
                view.findViewById(R.id.noSensorFoundedTextView).setVisibility(View.VISIBLE);
            }
        }
    }

    private void showDetachSensorDialog() {
        isDetachSensorDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.detach_sensor_dialog_title))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button), ((dialogInterface, i) -> {
                    isDetachSensorDialogVisible = false;
                    hub.getSensorInfo().detachSelectedSensor(requireContext(), hub.getDeviceID(), sensorType, (response) -> {
                        if (response.equals(HttpHelper.HTTP_RESPONSE_ERROR)) {
                            requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                        } else {
                            requireActivity().runOnUiThread(this::showSensorDetachedDialog);
                        }
                    });
                }))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    isDetachSensorDialogVisible = false;
                    dialogInterface.dismiss();
                })
                .show();
    }

    private void showSensorDetachedDialog() {
        isSensorDetachedDialogVisible = true;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.sensor_detached_title))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    isSensorDetachedDialogVisible = false;
                    if(this.onSensorConfigurationActionCallback != null) this.onSensorConfigurationActionCallback.onSensorConfigurationRestartApp();
                }).setCancelable(false)
                .show();
    }

    private void showSensorChosenDialog(String sensorID) {
        isSensorChosenDialogVisible = true;
        this.stringChosenSensor = sensorID;
        String message = getString(R.string.sensors_label) + ": " + sensorID;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_chosen_title_dialog))
                .setMessage(message)
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button), (dialogInterface, i) -> {
                    isSensorChosenDialogVisible = false;
                    if(checkSensorIsAlreadyChosen()) requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
                    else {
                        SensorsInfo.addNewSensor(requireContext(), hub.getDeviceID(), sensorID, sensorType, (response) -> {
                            if(response.equals(SENSOR_ADDED_SUCCESSFULLY_RESPONSE)) requireActivity().runOnUiThread(this::showSensorConfiguredDialog);
                            else requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                        });
                    }
                })
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    isSensorChosenDialogVisible = false;
                    dialogInterface.dismiss();
                })
                .show();
    }

    private boolean checkSensorIsAlreadyChosen() {
        if((hub != null) && (hub.getSensorInfo() != null)) {
            if((Objects.equals(sensorType, SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE)) &&
                    (hub.getSensorInfo().getAmbientTemperatureChosenSensor() != null) &&
                    (hub.getSensorInfo().getAmbientTemperatureChosenSensor().equals(stringChosenSensor))) {
                return true;
            }
            else if((Objects.equals(sensorType, SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT)) &&
                    (hub.getSensorInfo().getLightChosenSensor() != null) &&
                    (hub.getSensorInfo().getLightChosenSensor().equals(stringChosenSensor))) {
                return true;
            }
            else return (Objects.equals(sensorType, SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)) &&
                        (hub.getSensorInfo().getRelativeHumidityChosenSensor() != null) &&
                        (hub.getSensorInfo().getRelativeHumidityChosenSensor().equals(stringChosenSensor));
        }
        return false;
    }

    private void showSensorAlreadyConfiguredDialog() {
        isSensorAlreadyConfiguredDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_already_configured))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), ((dialogInterface, i) -> {
                    isSensorAlreadyConfiguredDialogVisible = false;
                    dialogInterface.dismiss();
                }))
                .show();
    }

    private void showSensorConfiguredDialog() {
        isSensorConfiguredDialogVisible = true;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_added_successfully))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), ((dialogInterface, i) -> {
                    isSensorConfiguredDialogVisible = false;
                    if(this.onSensorConfigurationActionCallback != null) this.onSensorConfigurationActionCallback.onSensorConfigurationRestartApp();
                }))
                .setCancelable(false)
                .create()
                .show();
    }

    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            if(this.onSensorConfigurationActionCallback != null) this.onSensorConfigurationActionCallback.onSensorConfigurationRestartApp();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
