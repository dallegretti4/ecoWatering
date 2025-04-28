package it.uniba.dib.sms2324.ecowatering.connection.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
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

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowatering.connection.ConnectToEWHubActivity;

public class ConnectionChooserFragment extends Fragment {
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_no_item, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(!ConnectToEWHubActivity.isFirstActivity()) {
                if(itemId == android.R.id.home) {
                    startActivity(new Intent(requireContext(), MainActivity.class));
                    requireActivity().finish();
                }
            }
            return false;
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(requireContext(), MainActivity.class));
            requireActivity().finish();
        }
    };
    private OnConnectionModeSelectedCallback onConnectionModeSelectedCallback;
    public interface OnConnectionModeSelectedCallback {
        void onModeSelected(Fragment fragment);
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
        // ON BACK PRESSED CALLBACK SETUP
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        // TOOLBAR SETUP
        toolbarSetup(view);
        // TITLE TEXT VIEW SETUP
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
            view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.connectionChooserHomeLogoImageView).setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_app_logo_no_bg, requireContext().getTheme()));
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.ewd_primary_color, requireActivity().getTheme()));
            if(ConnectToEWHubActivity.isFirstActivity()) {
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
                view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.connectionChooserHomeLogoImageView).setVisibility(View.VISIBLE);
                view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.connectionChooserToolbarTitleTextView).setVisibility(View.VISIBLE);
            }
            else {
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(it.uniba.dib.sms2324.ecowateringcommon.R.string.remote_device_connection_toolbar_title);
            toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void bluetoothButtonSetup(@NonNull View view) {
        Button bluetoothModeButton = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.bluetoothModeButton);
        bluetoothModeButton.setBackgroundColor(ResourcesCompat.getColor(
                getResources(),
                R.color.ewd_primary_color,
                requireActivity().getTheme()
        ));
        bluetoothModeButton.setOnClickListener((v) -> {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.BLUETOOTH_CONNECT}, ConnectToEWHubActivity.FIRST_BT_CONNECT_PERMISSION_REQUEST);
            }
            else {
                onConnectionModeSelectedCallback.onModeSelected(new BtConnectionFragment());
            }
        });
    }

    private void wifiButtonSetup(@NonNull View view) {
        Button wifiModeButton = view.findViewById(it.uniba.dib.sms2324.ecowateringcommon.R.id.wifiModeButton);
        wifiModeButton.setBackgroundColor(ResourcesCompat.getColor(
                getResources(),
                R.color.ewd_primary_color,
                requireActivity().getTheme()
        ));
        wifiModeButton.setOnClickListener((v) -> {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.NEARBY_WIFI_DEVICES}, ConnectToEWHubActivity.FIRST_WIFI_PERMISSION_REQUEST);
            }
            else {
                onConnectionModeSelectedCallback.onModeSelected(new WiFiConnectionFragment());
            }
        });
    }
}
