package it.uniba.dib.sms2324.ecowateringhub;

import android.content.Context;
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

import it.uniba.dib.sms2324.ecowateringcommon.AmbientTemperatureSensor;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringSensor;
import it.uniba.dib.sms2324.ecowateringcommon.LightSensor;
import it.uniba.dib.sms2324.ecowateringcommon.RelativeHumiditySensor;

public class SensorConfigurationFragment extends Fragment {
    private static EcoWateringSensor configSensor;
    private List<Sensor> sensorList;
    private OnSensorConfiguredCallback onSensorConfiguredCallback;
    public interface OnSensorConfiguredCallback {
        void onSensorConfigured(int sensorResult);
    }

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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getToolbarTitle());
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            ((AppCompatActivity) requireActivity()).addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.menu_refresh_item_only, menu);
                }

                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    int itemID = menuItem.getItemId();
                    if(itemID == android.R.id.home) {
                        ManualEcoWateringConfigurationActivity.popBackStackFragment();
                    }
                    else if(itemID == R.id.refreshItem) {
                        onSensorConfiguredCallback.onSensorConfigured(Common.REFRESH_FRAGMENT);
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }

        // LEFT SIDE
        Button selectedSensorDetachButton = view.findViewById(R.id.selectedSensorDetachButton);
        if(Objects.equals(ManualEcoWateringConfigurationActivity.getConfigureSensorType(), Common.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE)) {
            configSensor = new AmbientTemperatureSensor(requireContext());
            if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                    (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID());
                ((View) view.findViewById(R.id.selectedSensorCardDivisor)).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog(Common.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));
            }
        }
        else if(ManualEcoWateringConfigurationActivity.getConfigureSensorType().equals(Common.CONFIGURE_SENSOR_TYPE_LIGHT)) {
            configSensor = new LightSensor(requireContext());
            if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                    (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID());
                ((View) view.findViewById(R.id.selectedSensorCardDivisor)).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog(Common.CONFIGURE_SENSOR_TYPE_LIGHT));
            }
        }
        else if(ManualEcoWateringConfigurationActivity.getConfigureSensorType().equals(Common.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)) {
            configSensor = new RelativeHumiditySensor(requireContext());
            if((MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                    (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {
                ((TextView) view.findViewById(R.id.selectedSensorValueTextView)).setText(MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID());
                ((View) view.findViewById(R.id.selectedSensorCardDivisor)).setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setVisibility(View.VISIBLE);
                selectedSensorDetachButton.setOnClickListener((v) -> showDetachSensorDialog(Common.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
            }
        }

        // RIGHT SIDE
        ((TextView) view.findViewById(R.id.sensorsConfigurationTitleTextView)).setText(getTextViewTitle());

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
            sensorListView.setOnItemClickListener((adapterView, v, position, l) -> onSensorChosen(position));
        }
        // EMPTY CASE
        else {
            sensorListView.setVisibility(View.GONE);
            view.findViewById(R.id.sensorsConfigurationTitleTextView).setVisibility(View.GONE);
            view.findViewById(R.id.noSensorFoundedTextView).setVisibility(View.VISIBLE);
        }
    }

    private String getToolbarTitle() {
        return ManualEcoWateringConfigurationActivity.getConfigureSensorType();
    }

    private String getTextViewTitle() {
        return String.format(
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensors_configuration_title),
                ManualEcoWateringConfigurationActivity.getConfigureSensorType());
    }

    private void onSensorChosen(int position) {
        Sensor chosenSensor = sensorList.get(position);
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_name_label) + chosenSensor.getName() + "\n" +
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_vendor_label) + chosenSensor.getVendor() + "\n" +
                getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_version_label) + chosenSensor.getVersion();
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_chosen_title_dialog))
                .setMessage(message)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> setSensorOnEWHub(chosenSensor))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void setSensorOnEWHub(Sensor chosenSensor) {
        if(Objects.equals(ManualEcoWateringConfigurationActivity.getConfigureSensorType(), Common.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else if(Objects.equals(ManualEcoWateringConfigurationActivity.getConfigureSensorType(), Common.CONFIGURE_SENSOR_TYPE_LIGHT) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else if(Objects.equals(ManualEcoWateringConfigurationActivity.getConfigureSensorType(), Common.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID().equals(EcoWateringSensor.getSensorId(chosenSensor)))) {
            requireActivity().runOnUiThread(this::showSensorAlreadyConfiguredDialog);
        }
        else {
            EcoWateringSensor.addNewEcoWateringSensor(requireContext(), chosenSensor, Common.getThisDeviceID(requireContext()), (response) -> {
                if(response.equals(Common.SENSOR_ADDED_SUCCESSFULLY_RESPONSE)) {
                    requireActivity().runOnUiThread(this::showSensorConfiguredDialog);
                }
                else {
                    requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                }
            });
        }
    }

    private void showSensorAlreadyConfiguredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_already_configured))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }

    private void showDetachSensorDialog(String sensorType) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.detach_sensor_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        ((dialogInterface, i) -> {
                            if(sensorType.equals(Common.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE)) {
                                MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().detachSelectedSensor(requireContext(), sensorType);
                            }
                            else if(sensorType.equals(Common.CONFIGURE_SENSOR_TYPE_LIGHT)) {
                                MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getLightSensor().detachSelectedSensor(requireContext(), sensorType);
                            }
                            else if(sensorType.equals(Common.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)) {
                                MainActivity.thisEcoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().detachSelectedSensor(requireContext(), sensorType);
                            }
                        }))
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    protected void showSensorConfiguredDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.sensor_added_successfully))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            EcoWateringHub.getEcoWateringHubJsonString(
                                    Common.getThisDeviceID(requireContext()),
                                    (jsonResponse) -> Common.restartApp(requireContext()));
                        }))
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> {
                            Common.restartApp(requireContext());
                            requireActivity().finish();
                        })
                )
                .setCancelable(false)
                .create()
                .show();
    }
}
