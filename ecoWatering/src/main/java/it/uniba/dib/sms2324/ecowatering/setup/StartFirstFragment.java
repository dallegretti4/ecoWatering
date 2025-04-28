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

public class StartFirstFragment extends Fragment {
    private EditText deviceNameEditText;
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
        deviceNameEditText = view.findViewById(R.id.deviceNameEditText);
        Button finishButton = view.findViewById(R.id.firstStartFinishButton);
        finishButton.setEnabled(false);
        deviceNameEditText.addTextChangedListener(new TextWatcher() {
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
        });
        finishButton.setOnClickListener((v) -> showDeviceNameConfirmDialog());
    }

    private void showDeviceNameConfirmDialog() {
        String message = getString(R.string.name_label) + ": " + deviceNameEditText.getText().toString();
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure_label))
                .setMessage(message)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> onFirstStartFinishCallback.onFinish(deviceNameEditText.getText().toString()))
                .setNegativeButton(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button), ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setCancelable(false);
        dialog.show();
    }

}
