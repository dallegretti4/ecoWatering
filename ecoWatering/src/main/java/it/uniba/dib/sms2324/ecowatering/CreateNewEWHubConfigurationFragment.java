package it.uniba.dib.sms2324.ecowatering;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;

public class CreateNewEWHubConfigurationFragment extends Fragment {

    private OnEWHubConfigurationAgreedCallback onEWHubConfigurationAgreedCallback;
    protected interface OnEWHubConfigurationAgreedCallback {
        void onEWHubConfigurationAgreed(EcoWateringHub hub);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnEWHubConfigurationAgreedCallback) {
            onEWHubConfigurationAgreedCallback = (OnEWHubConfigurationAgreedCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onEWHubConfigurationAgreedCallback = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_new_ew_hub_configuration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Button configurationAgreeButton = view.findViewById(R.id.configurationAgreeButton);
        configurationAgreeButton.setOnClickListener((v) -> {
            /**
             * TO-DO
             * - to implement configuration ;
             * - to add new configuration object into database server ;
             * - to add new configuration object in ManageEWHubActivity.selectedEWHub .
             */
            ManageEWHubActivity.selectedEWHub.setIsAutomated(
                    Common.getThisDeviceID(requireContext()),
                    true,
                    (jsonResponse) -> {
                        if(jsonResponse != null && !jsonResponse.equals("null") && !jsonResponse.equals(Common.HTTP_RESPONSE_ERROR)) {
                            onEWHubConfigurationAgreedCallback.onEWHubConfigurationAgreed(new EcoWateringHub(jsonResponse));
                        }
                        else {
                            requireActivity().runOnUiThread(this::showHttpErrorFaultDialog);
                        }

                    }
            );
        });
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Notify the user something went wrong with the database server.
     * Positive button restarts the app.
     */
    protected void showHttpErrorFaultDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            requireActivity().finish();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) requireActivity().finish();
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
