package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;

public class ConnectionChooserFragment extends Fragment {
    private static String calledFrom;
    private static boolean isFirstActivity;
    private final int primaryColor;
    private Button bluetoothModeButton;
    private Button wifiModeButton;
    private OnConnectionChooserActionCallback onConnectionChooserActionCallback;
    public interface OnConnectionChooserActionCallback {
        void onConnectionChooserBackPressed();
        void onModeSelected(String mode);
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onConnectionChooserActionCallback.onConnectionChooserBackPressed();
        }
    };

   @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectionChooserActionCallback) {
            this.onConnectionChooserActionCallback = (OnConnectionChooserActionCallback) context;
        }
   }

   @Override
   public void onDetach() {
        super.onDetach();
        this.onConnectionChooserActionCallback = null;
   }

   public ConnectionChooserFragment() {
       this(calledFrom, isFirstActivity);
   }

    public ConnectionChooserFragment(String calledFromString, boolean isFirstActivityValue) {
        super(R.layout.fragment_connection_chooser);
        calledFrom = calledFromString;
        isFirstActivity = isFirstActivityValue;
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
            this.primaryColor = R.color.ew_primary_color_from_hub;
        }
        else {
            this.primaryColor = R.color.ew_primary_color_from_device;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        fragmentSetup(view);
        // ACCESS FINE LOCATION PERMISSION REQUIRED
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.LOCATION_PERMISSION_REQUEST);
        }
        else {
            bluetoothModeButton.setOnClickListener((v) -> onConnectionChooserActionCallback.onModeSelected(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH));
            wifiModeButton.setOnClickListener((v) -> onConnectionChooserActionCallback.onModeSelected(OnConnectionFinishCallback.CONNECTION_MODE_WIFI));
        }
    }

    @Override
    public void onResume() {
       super.onResume();
       if(SharedPreferencesHelper.readBooleanOnSharedPreferences(requireContext(), SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY)) {
           Log.i(Common.THIS_LOG, "ConnectionChooserFragment -> onResume()");
           SharedPreferencesHelper.writeBooleanOnSharedPreferences(requireContext(), SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_FILE_NAME, SharedPreferencesHelper.IS_USER_RETURNED_FROM_SETTING_VALUE_KEY, false);
           // ACCESS FINE LOCATION PERMISSION REQUIRED
           if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.LOCATION_PERMISSION_REQUEST);
           }
           else {
               bluetoothModeButton.setOnClickListener((v) -> onConnectionChooserActionCallback.onModeSelected(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH));
               wifiModeButton.setOnClickListener((v) -> onConnectionChooserActionCallback.onModeSelected(OnConnectionFinishCallback.CONNECTION_MODE_WIFI));
           }
       }
    }


    private void fragmentSetup(@NonNull View view) {
        toolbarSetup(view);
        titleSetup(view);
        // BLUETOOTH BUTTON SETUP
        bluetoothModeButton = view.findViewById(R.id.bluetoothModeButton);
        bluetoothModeButton.setBackgroundColor(ResourcesCompat.getColor(
                getResources(),
                this.primaryColor,
                requireActivity().getTheme()
        ));
        // WIFI BUTTON SETUP
        wifiModeButton = view.findViewById(R.id.wifiModeButton);
        wifiModeButton.setBackgroundColor(ResourcesCompat.getColor(
                getResources(),
                this.primaryColor,
                requireActivity().getTheme()
        ));
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primaryColor, requireActivity().getTheme()));
            // HUB CASE
            if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(R.string.remote_device_connection_toolbar_title));
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(R.drawable.back_icon);
            }
            // DEVICE CASE
            else {
                if(isFirstActivity) {
                    view.findViewById(R.id.connectionChooserHomeLogoImageView).setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_app_logo_no_bg_device, requireContext().getTheme()));
                    Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
                    view.findViewById(R.id.connectionChooserHomeLogoImageView).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.connectionChooserToolbarTitleTextView).setVisibility(View.VISIBLE);
                }
                else {
                    Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(R.string.connect_to_hub_toolbar_title);
                    Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                    Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(R.drawable.back_icon);
                }
            }
            toolbar.setTitleTextAppearance(requireContext(), R.style.toolBarTitleStyle);
            menuSetup();
        }
    }

    private void menuSetup() {
        if(!isFirstActivity) {
            requireActivity().addMenuProvider(new MenuProvider() {
                @Override
                public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.menu_no_item, menu);
                }

                @Override
                public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                    if(menuItem.getItemId() == android.R.id.home) {
                        onConnectionChooserActionCallback.onConnectionChooserBackPressed();
                    }
                    return false;
                }
            }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void titleSetup(@NonNull View view) {
        TextView titleTextView = view.findViewById(R.id.connectionChooserTitleTextView);
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
            titleTextView.setText(R.string.connection_chooser_fragment_title_hub);
        }
        else {
            titleTextView.setText(R.string.connection_chooser_fragment_title_device);
        }
    }
}
