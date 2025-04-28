package it.uniba.dib.sms2324.ecowateringhub.configuration;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringhub.R;

public class EcoWateringConfigurationActivity extends AppCompatActivity implements
        EcoWateringConfigurationFragment.OnConfigurationUserActionCallback,
        SensorConfigurationFragment.OnSensorConfiguredCallback {
    private static FragmentManager fragmentManager;
    private static String configureSensorType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ew_configuration);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fragmentManager = getSupportFragmentManager();
        changeFragment(new EcoWateringConfigurationFragment(), false);
    }

    @Override
    public void onAutomateIrrigationSystem() {}

    @Override
    public void configureSensor(String chosenConfigureSensorType) {
        configureSensorType = chosenConfigureSensorType;
        changeFragment(new SensorConfigurationFragment(), true);
    }

    @Override
    public void onSensorConfigured(int sensorResult) {
        if(sensorResult == Common.ACTION_BACK_PRESSED) {
            fragmentManager.popBackStack();
        }
        else if(sensorResult == Common.REFRESH_FRAGMENT) {
            fragmentManager.popBackStack();
            changeFragment(new SensorConfigurationFragment(), true);
        }
    }

    private void changeFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.manualEWConfigurationFrameLayout, fragment);
        if(addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    protected static String getConfigureSensorType() {
        return configureSensorType;
    }
}