package it.uniba.dib.sms2324.ecowateringhub;

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

public class StartFirstFragment extends Fragment {
    private EditText hubNameEditText;
    private TextView locationFoundTextView;
    private Button finishButton;
    private static Address tmpAddress = null;
    private OnFirstStartFinishCallback onFirstStartFinishCallback;
    protected static OnGpsEnabledCallback onGpsEnabledCallback;

    protected interface OnFirstStartFinishCallback {
        void onFirstStartFinish(String hubName, @NonNull Address address);
    }
    protected interface OnLocationFoundCallback {
        void onLocationFound(Address address);
    }
    protected interface OnGpsEnabledCallback {
        void onGpsEnabled(int resultCode);
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
        finishButton = view.findViewById(R.id.firstStartFinishButton);
        finishButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color_80, requireActivity().getTheme()));
        finishButton.setEnabled(false);
        hubNameEditText = view.findViewById(R.id.hubNameEditText);
        locationFoundTextView = view.findViewById(R.id.locationAddressTextView);
        Button updateLocationButton = view.findViewById(R.id.updateLocationButton);

        hubNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if ((!locationFoundTextView.getText().toString().equals(getString(R.string.address_not_found))) &&
                        (!hubNameEditText.getText().toString().equals("")) && (!finishButton.isEnabled())) {
                    finishButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme()));
                    finishButton.setEnabled(true);
                }
                if (hubNameEditText.getText().toString().equals("") && finishButton.isEnabled()) {
                    finishButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color_80, requireActivity().getTheme()));
                    finishButton.setEnabled(false);
                }
            }
        });

        locationFoundTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if((!hubNameEditText.getText().toString().equals("")) &&
                        (!locationFoundTextView.getText().toString().equals(getString(R.string.address_not_found))) &&
                        (!finishButton.isEnabled())) {
                    finishButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color, requireActivity().getTheme()));
                    finishButton.setEnabled(true);
                }
            }
        });

        updateLocationButton.setOnClickListener((v) ->
            startFindLocation((address) -> {
                if(address != null) {
                    tmpAddress = address;
                    String stringAddress = address.getThoroughfare() + ", " + address.getLocality() + " - " + address.getCountryName();
                    locationFoundTextView.setText(stringAddress);
                }
            })
        );

        finishButton.setOnClickListener((v) -> showHubNameConfirmDialog());

        // FORCE TO UPDATE LOCATION
        startFindLocation((address) -> {
            if(address != null) {
                tmpAddress = address;
                String stringAddress = address.getThoroughfare() + ", " + address.getLocality() + " - " + address.getCountryName();
                locationFoundTextView.setText(stringAddress);
            }
        });
    }

    /**
     *
     * {@code @param:}
     *  OnLocationFoundCallback callback -> to manage the location found
     * To find location of user.
     */
    protected void startFindLocation(OnLocationFoundCallback callback) {
        // CHECK PERMISSION
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, Common.LOCATION_PERMISSION_REQUEST);
        }
        // FIND LOCATION
        else {
            enableGPS(onGpsEnabledCallback = (resultCode) -> {
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
    private void enableGPS(StartFirstFragment.OnGpsEnabledCallback callback) {
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
                    callback.onGpsEnabled(Common.GPS_ENABLED_RESULT);
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
                Log.i(Common.THIS_LOG, "country: " + addresses.get(0).getCountryName());
                Log.i(Common.THIS_LOG, "locality: " + addresses.get(0).getLocality());
                Log.i(Common.THIS_LOG, "Thoroughfare: " + addresses.get(0).getThoroughfare());
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.hub_name_confirm_dialog_title))
                .setMessage(getString(R.string.hub_name_confirm_dialog_message))
                .setPositiveButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        ((dialogInterface, i) -> onFirstStartFinishCallback.onFirstStartFinish(hubNameEditText.getText().toString(), tmpAddress)))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button),
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setCancelable(false);
        dialog.show();
    }
}
