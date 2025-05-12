package it.uniba.dib.sms2324.ecowateringhub.setup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringhub.R;

public class StartFirstFragment extends Fragment {
    private static final String CHOSEN_HUB_NAME_OUT_STATE = "CHOSEN_HUB_NAME_OUT_STATE";
    private EditText hubNameEditText;
    private TextView locationFoundTextView;
    private Button nextButton;
    private Address tmpAddress = null;
    private static boolean isDialogShowed;
    private interface OnLocationFoundCallback {
        void onLocationFound(Address address);
    }
    private OnFirstStartFinishCallback onFirstStartFinishCallback;
    public interface OnFirstStartFinishCallback {
        void onFirstStartFinish(String hubName, @NonNull Address address);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFirstStartFinishCallback) {
            onFirstStartFinishCallback = (OnFirstStartFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onFirstStartFinishCallback = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_first, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // SETUP
        nextButtonSetup(view);
        hubNameEditTextSetup(view);
        locationFoundTextViewSetup(view);
        updateLocationButtonSetup(view);

        startFindLocation((address) -> {    // FORCE TO UPDATE LOCATION WHEN FRAGMENT START
            if(address != null) {
                tmpAddress = address;
                String stringAddress = address.getThoroughfare() + ", " + address.getLocality() + " - " + address.getCountryName();
                locationFoundTextView.setText(stringAddress);
            }
        });

        if(savedInstanceState != null) {    // CONFIGURATION CHANGED CASE
            if(savedInstanceState.getString(CHOSEN_HUB_NAME_OUT_STATE) != null ) {
                this.hubNameEditText.setText(savedInstanceState.getString(CHOSEN_HUB_NAME_OUT_STATE));
            }
            if(isDialogShowed) {
                showHubNameConfirmDialog();
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(!this.hubNameEditText.getText().toString().equals(Common.VOID_STRING_VALUE)) {
            outState.putString(CHOSEN_HUB_NAME_OUT_STATE, this.hubNameEditText.getText().toString());
        }
    }

    private void nextButtonSetup(@NonNull View view) {
        nextButton = view.findViewById(R.id.firstStartFinishButton);
        nextButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color_80, requireActivity().getTheme()));
        nextButton.setEnabled(false);
        nextButton.setOnClickListener((v) -> showHubNameConfirmDialog());
    }

    private void hubNameEditTextSetup(@NonNull View view) {
        this.hubNameEditText = view.findViewById(R.id.hubNameEditText);
        this.hubNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if ((!locationFoundTextView.getText().toString().equals(getString(R.string.address_not_found))) &&
                        (!hubNameEditText.getText().toString().equals("")) && (!nextButton.isEnabled())) {
                    nextButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme()));
                    nextButton.setEnabled(true);
                }
                if (hubNameEditText.getText().toString().equals("") && nextButton.isEnabled()) {
                    nextButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color_80, requireActivity().getTheme()));
                    nextButton.setEnabled(false);
                }
            }
        });
    }

    private void locationFoundTextViewSetup(@NonNull View view) {
        locationFoundTextView = view.findViewById(R.id.locationAddressTextView);
        locationFoundTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if((!hubNameEditText.getText().toString().equals("")) &&
                        (!locationFoundTextView.getText().toString().equals(getString(R.string.address_not_found))) &&
                        (!nextButton.isEnabled())) {
                    nextButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme()));
                    nextButton.setEnabled(true);
                }
            }
        });
    }

    private void updateLocationButtonSetup(@NonNull View view) {
        Button updateLocationButton = view.findViewById(R.id.updateLocationButton);
        updateLocationButton.setOnClickListener((v) ->
                startFindLocation((address) -> {
                    if(address != null) {
                        tmpAddress = address;
                        String stringAddress = address.getThoroughfare() + ", " + address.getLocality() + " - " + address.getCountryName();
                        locationFoundTextView.setText(stringAddress);
                    }
                })
        );
    }

    /**
     *
     * {@code @param:}
     *  OnLocationFoundCallback callback -> to manage the location found
     * To find location of user.
     */
    private void startFindLocation(OnLocationFoundCallback callback) {
        // CHECK PERMISSION
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.LOCATION_PERMISSION_REQUEST);
        }
        // FIND LOCATION
        else {
            enableGPS((resultCode) -> {
                if(resultCode == Common.GPS_ENABLED_RESULT) {
                    FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
                    fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.i(Common.THIS_LOG, "latitude: " + latitude + " - longitude: " + longitude);
                            callback.onLocationFound(getAddressFromCoordinate(latitude, longitude));
                        } else {
                            Log.i(Common.THIS_LOG, "location null");
                            callback.onLocationFound(null);
                        }
                    });
                }
            });
        }
    }

    /**
     * {@code @param:}
     *  OnGpsEnabledCallback callback;
     * If GPS is not enabled, requests to enable GPS
     */
    private void enableGPS(Common.OnIntegerResultGivenCallback callback) {
        com.google.android.gms.location.LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(requireActivity());
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    Log.i(Common.THIS_LOG, "GPS already enabled");
                    callback.getResult(Common.GPS_ENABLED_RESULT);
                })
                .addOnFailureListener(e -> {
                    if(e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(requireActivity(), Common.GPS_ENABLE_REQUEST);
                        }
                        catch (IntentSender.SendIntentException sendEx) {
                            sendEx.printStackTrace();
                        }
                    }
                });
    }

    /**
     *
     * {@code @param:}
     *  double latitude;
     *  double longitude;
     * Return an Address instance, created from coordinate.
     */
    private Address getAddressFromCoordinate(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if(addresses != null && !addresses.isEmpty()) {
                return addresses.get(0);
            }
            else {
                return null;
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ask confirm to the user, about the hub name chosen.
     * Positive button to callback;
     * Negative button to close dialog.
     */
    private void showHubNameConfirmDialog() {
        isDialogShowed = true;
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.name_label) + ": " + hubNameEditText.getText().toString();
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.hub_name_confirm_dialog_title))
                .setMessage(message)
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        ((dialogInterface, i) -> {
                            isDialogShowed = false;
                            onFirstStartFinishCallback.onFirstStartFinish(hubNameEditText.getText().toString(), tmpAddress);
                        }))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button),
                        ((dialogInterface, i) -> {
                            isDialogShowed = false;
                            dialogInterface.dismiss();
                        }))
                .setCancelable(false);
        dialog.show();
    }
}
