package it.uniba.dib.sms2324.ecowateringhub.configuration;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.AmbientTemperatureSensor;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.EcoWateringSensor;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.LightSensor;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.RelativeHumiditySensor;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class SensorConfigurationFragment extends Fragment {
    private static final String SENSOR_ADDED_SUCCESSFULLY_RESPONSE = "sensorAddedSuccessfully";
    private EcoWateringSensor configSensor;
    private List<Sensor> sensorList;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemID = menuItem.getItemId();
            if(itemID == android.R.id.home) {
                onSensorConfiguredCallback.onSensorConfigured(Common.ACTION_BACK_PRESSED);
            }
            else if(itemID == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                onSensorConfiguredCallback.onSensorConfigured(Common.REFRESH_FRAGMENT);
            }
            return false;
        }
    };
    private OnSensorConfiguredCallback onSensorConfiguredCallback;
    protected interface OnSensorConfiguredCallback {
        void onSensorConfigured(int sensorResult);
    }
    private static Sensor chosenSensor;
    private static boolean isSensorChosenDialogVisible;
    private static boolean isSensorAlreadyConfiguredDialogVisible;
    private static boolean isDetachSensorDialogVisible;
    private static boolean isSensorDetachedDialogVisible;
    private static boolean isSensorConfiguredDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;

    public SensorConfigurationFragment() {
        super(R.layout.fragment_sensor_configuration);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnSensorConfiguredCallback) {
            onSensorConfiguredCallback = (OnSensorConfiguredCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onSensorConfiguredCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        toolbarSetup(view);
        selectedSensorDetachButtonSetup(view);
        ((TextView) view.findViewById(R.id.sensorsConfigurationTitleTextView)).setText( // TITLE SETUP
                String.format(
                    getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensors_configuration_title),
                    EcoWateringConfigurationActivity.getConfigureSensorType())
        );
        sensorListViewSetup(view);

        if(savedInstanceState != null) { // DIALOG RECOVERING FROM CONFIGURATION CHANGED
            if(isSensorChosenDialogVisible) onSensorChosenDialog(chosenSensor);
            else if(isSensorAlreadyConfiguredDialogVisible) showSensorAlreadyConfiguredDialog();
            else if(isDetachSensorDialogVisible) showDetachSensorDialog();
            else if(isSensorDetachedDialogVisible) showSensorDetachedDialog();
            else if(isSensorConfiguredDialogVisible) showSensorConfiguredDialog();
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(EcoWateringConfigurationActivity.getConfigureSensorType());
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void selectedSensorDetachButtonSetup(@NonNull View view) {
        Button selectedSensorDetachButton = view.findViewById(R.id.selectedSensorDetachButton);
        if(Objects.equals(EcoWateringConfigurationActivity.getConfigureSensorType(), EcoWateringSensor.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE)) {
            configSensor = new AmbientTemperatureSensor(requireContext());
            if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                    (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID());
                view.findViewById(R.id.selectedSensorCardDivisor).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog());
            }
        }
        else if(EcoWateringConfigurationActivity.getConfigureSensorType().equals(EcoWateringSensor.CONFIGURE_SENSOR_TYPE_LIGHT)) {
            configSensor = new LightSensor(requireContext());
            if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor() != null) &&
                    (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor().getSensorID());
                view.findViewById(R.id.selectedSensorCardDivisor).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog());
            }
        }
        else if(EcoWateringConfigurationActivity.getConfigureSensorType().equals(EcoWateringSensor.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)) {
            configSensor = new RelativeHumiditySensor(requireContext());
            if((MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                    (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID());
                view.findViewById(R.id.selectedSensorCardDivisor).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog());
            }
        }
    }

    private void sensorListViewSetup(@NonNull View view) {
        ListView sensorListView = view.findViewById(R.id.sensorsListView);
        ArrayList<String> sensorStringList = new ArrayList<>();
        sensorList = new ArrayList<>();
        ArrayAdapter<String> sensorsListAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, sensorStringList);
        sensorListView.setAdapter(sensorsListAdapter);
        // NOT EMPTY CASE
        if(configSensor.getSensorListFromDevice() != null && (!configSensor.getSensorListFromDevice().isEmpty())) {
            for(Sensor sensor : configSensor.getSensorListFromDevice()) {
                sensorStringList.add(sensor.getName());
                sensorList.add(sensor);
                requireActivity().runOnUiThread(sensorsListAdapter::notifyDataSetChanged);
            }
            sensorListView.setOnItemClickListener((adapterView, v, position, l) -> onSensorChosenDialog(sensorList.get(position)));
        }
        // EMPTY CASE
        else {
            sensorListView.setVisibility(View.GONE);
            view.findViewById(R.id.sensorsConfigurationTitleTextView).setVisibility(View.GONE);
            view.findViewById(R.id.noSensorFoundedTextView).setVisibility(View.VISIBLE);
        }
    }

    private void onSensorChosenDialog(@NonNull Sensor sensor) {
        chosenSensor = sensor;
        isSensorChosenDialogVisible = true;
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_name_label) + chosenSensor.getName() + "\n" +
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_vendor_label) + chosenSensor.getVendor() + "\n" +
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_version_label) + chosenSensor.getVersion();
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_chosen_title_dialog))
                .setMessage(message)
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button), (dialogInterface, i) -> {
                    isSensorChosenDialogVisible = false;
                    setSensorOnEWHub(chosenSensor);
                })
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), (dialogInterface, i) -> {
                    isSensorChosenDialogVisible = false;
                    dialogInterface.dismiss();
                })
                .show();
    }

    private void setSensorOnEWHub(Sensor chosenSensor) {
        if(Objects.equals(EcoWateringConfigurationActivity.getConfigureSensorType(), EcoWateringSensor.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else if(Objects.equals(EcoWateringConfigurationActivity.getConfigureSensorType(), EcoWateringSensor.CONFIGURE_SENSOR_TYPE_LIGHT) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getLightSensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else if(Objects.equals(EcoWateringConfigurationActivity.getConfigureSensorType(), EcoWateringSensor.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.getThisEcoWateringHub().getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else {
            EcoWateringSensor.addNewEcoWateringSensor(requireContext(), chosenSensor, Common.getThisDeviceID(requireContext()), (response) -> {
                if(response.equals(SENSOR_ADDED_SUCCESSFULLY_RESPONSE)) {
                    requireActivity().runOnUiThread(this::showSensorConfiguredDialog);
                }
                else {
                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                }
            });
        }
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

    private void showDetachSensorDialog() {
        isDetachSensorDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.detach_sensor_dialog_title))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button), ((dialogInterface, i) -> {
                    isDetachSensorDialogVisible = false;
                    configSensor.detachSelectedSensor(requireContext(), (response) -> {
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
                        startActivity(new Intent(requireContext(), MainActivity.class));
                        requireActivity().finish();
                }).setCancelable(false)
                .show();
    }

    private void showSensorConfiguredDialog() {
        isSensorConfiguredDialogVisible = true;
        chosenSensor = null;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_added_successfully))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button), ((dialogInterface, i) -> {
                            isSensorConfiguredDialogVisible = false;
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        }))
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        chosenSensor = null;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
