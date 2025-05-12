package it.uniba.dib.sms2324.ecowatering.setup;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class StartFirstFragment extends Fragment {
    private static final String USERNAME_OUT_STATE = "USERNAME_OUT_STATE";
    private EditText deviceNameEditText;
    private Button finishButton;
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override
        public void afterTextChanged(Editable editable) {
            if(!deviceNameEditText.getText().toString().equals("")) {
                finishButton.setEnabled(true);
            }
            else {
                if(finishButton.isEnabled()) finishButton.setEnabled(false);
            }
        }
    };
    private OnFirstStartFinishCallback onFirstStartFinishCallback;
    public interface OnFirstStartFinishCallback {
        void onFinish(String deviceName);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnFirstStartFinishCallback) {
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
        this.deviceNameEditText = view.findViewById(R.id.deviceNameEditText);
        this.finishButton = view.findViewById(R.id.firstStartFinishButton);
        this.finishButton.setEnabled(false); // LOCK FINISH BUTTON UNTIL USER CHOOSE A NAME
        this.finishButton.setOnClickListener((v) -> showDeviceNameConfirmDialog());
        this.deviceNameEditText.addTextChangedListener(this.textWatcher);   // LOGIC TO ENABLE THE FINISH BUTTON
        // CONFIGURATION CHANGED CASE
        if(savedInstanceState != null && savedInstanceState.getString(USERNAME_OUT_STATE) != null) {
            this.deviceNameEditText.setText(savedInstanceState.getString(USERNAME_OUT_STATE));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(!deviceNameEditText.getText().toString().equals(Common.VOID_STRING_VALUE)) {
            outState.putString(USERNAME_OUT_STATE, deviceNameEditText.getText().toString());
        }
    }

    private void showDeviceNameConfirmDialog() {
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.name_label) + ": " + deviceNameEditText.getText().toString();
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.are_you_sure_label))
                .setMessage(message)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> onFirstStartFinishCallback.onFinish(deviceNameEditText.getText().toString()))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button), ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setCancelable(false);
        dialog.show();
    }

}
