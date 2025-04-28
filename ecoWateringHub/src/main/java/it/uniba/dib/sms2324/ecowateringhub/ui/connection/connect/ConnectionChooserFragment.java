package it.uniba.dib.sms2324.ecowateringhub.ui.connection.connect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.ui.connection.ManageRemoteEWDevicesConnectedActivity;

public class ConnectionChooserFragment extends Fragment {
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_no_item, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemID = menuItem.getItemId();
            if(itemID == android.R.id.home) {
                ManageRemoteEWDevicesConnectedActivity.popBackStackFragment();
            }
            return false;
        }
    };

    private OnConnectionModeSelectedCallback onConnectionModeSelectedCallback;
    public interface OnConnectionModeSelectedCallback {
        void onModeSelected(@NonNull Fragment fragment);
    }

    public ConnectionChooserFragment() {
        super(it.uniba.dib.sms2324.ecowateringcommon.R.layout.fragment_connection_chooser);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectionModeSelectedCallback) {
            onConnectionModeSelectedCallback = (OnConnectionModeSelectedCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onConnectionModeSelectedCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
        toolbarSetup(view);
        // TITLE SETUP
        TextView titleTextView = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.connectionChooserTitleTextView);
        titleTextView.setText(R.string.connection_chooser_fragment_title);
        // BLUETOOTH BUTTON SETUP
        bluetoothButtonSetup(view);
        // WIFI BUTTON SETUP
        wifiButtonSetup(view);
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ew_primary_color, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.remote_device_connection_toolbar_title));
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void bluetoothButtonSetup(@NonNull View view) {
        Button bluetoothModeButton = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.bluetoothModeButton);
        bluetoothModeButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ew_primary_color, requireActivity().getTheme()));
        bluetoothModeButton.setOnClickListener((v) -> {
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
                    ((ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) ||
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        new String[] {
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        ManageRemoteEWDevicesConnectedActivity.BT_PERMISSION_REQUEST
                );
            }
            else {
                onConnectionModeSelectedCallback.onModeSelected(new BtConnectionFragment());
            }
        });
    }

    private void wifiButtonSetup(@NonNull View view) {
        Button wifiModeButton = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.wifiModeButton);
        wifiModeButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ew_primary_color, requireActivity().getTheme()));
        wifiModeButton.setOnClickListener((v) -> {
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) &&
                    (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.NEARBY_WIFI_DEVICES}, Common.WIFI_PERMISSION_REQUEST);
            }
            else {
                onConnectionModeSelectedCallback.onModeSelected(new WiFiConnectionFragment());
            }
        });
    }
}
