package it.uniba.dib.sms2324.ecowateringcommon.ui.hub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class ManageHubManualControlFragment extends ManageHubFragment {
    private OnHubManualActionChosenCallback onHubManualActionChosenCallback;
    public interface OnHubManualActionChosenCallback extends OnManageHubActionCallback{
        void automateEcoWateringSystem();
        void setIrrigationSystemState(boolean value);
        void setDataObjectRefreshing(boolean value);
    }
    private static boolean isSwitchIrrigationSystemDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;

    public ManageHubManualControlFragment() {
        this(calledFrom, hub);
    }

    public ManageHubManualControlFragment(String calledFromString, @NonNull EcoWateringHub hubObj) {
        super(calledFromString, hubObj);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnHubManualActionChosenCallback) {
            this.onHubManualActionChosenCallback = (OnHubManualActionChosenCallback) context;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        this.onHubManualActionChosenCallback = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState != null)  {  // CONFIGURATION CHANGED CASE
            if(isSwitchIrrigationSystemDialogVisible) showSwitchIrrigationSystemDialog(!hub.getIrrigationSystem().getState());
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void specializedViewSetup() {
        if(getView() != null) {
            View view = getView();
            irrigationSystemCardSetup(view);
            automateEcoWateringSystemCardSetup(view);
            backgroundRefreshCardSetup(view);
        }
    }

    @Override
    public Runnable getRefreshManageHubFragmentRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if(onHubManualActionChosenCallback != null) onHubManualActionChosenCallback.refreshDataObject(((ecoWateringHub) -> {
                    hub = ecoWateringHub;
                    if(hub.isAutomated()) onHubManualActionChosenCallback.restartApp();
                    else if(getView() != null) requireActivity().runOnUiThread(() -> manageHubViewSetup(getView()));
                }));
                if(isRefreshManageHubFragmentRunning) refreshManageHubFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FREQUENCY);
            }
        };
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleIrrigationSystemCard).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        TextView irrigationSystemValueStateTextView = view.findViewById(R.id.irrigationSystemValueStateTextView);
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(null);
        if(hub.getIrrigationSystem().getState()) {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
            irrigationSystemStateSwitchCompat.setChecked(true);
        }
        else {
            irrigationSystemValueStateTextView.setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
            irrigationSystemStateSwitchCompat.setChecked(false);
        }
        // NEED TO HIDE LOADING CARD AFTER STATE CHANGED CASE
        if(view.findViewById(R.id.loadingCard).getVisibility() == View.VISIBLE) {
            view.findViewById(R.id.loadingCard).setVisibility(View.GONE);
        }
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(
                (buttonView, isChecked) -> requireActivity().runOnUiThread(() -> showSwitchIrrigationSystemDialog(isChecked))
        );
    }

    private void automateEcoWateringSystemCardSetup(@NonNull View view) {
        TextView automatedStateMainTextView = view.findViewById(R.id.automatedStateMainTextView);
        automatedStateMainTextView.setText(getString(R.string.configuration_not_automated));
        automatedStateMainTextView.setTextColor(ResourcesCompat.getColorStateList(getResources(), R.color.ew_secondary_color_30, requireActivity().getTheme()));
        ((TextView) view.findViewById(R.id.automateStateSuggestTextView)).setText(getString(R.string.configuration_not_automated_message));
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setChecked(false);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) onHubManualActionChosenCallback.automateEcoWateringSystem();
        });
    }

    private void backgroundRefreshCardSetup(@NonNull View view) {
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.dataObjectRefreshSwitch);
        backgroundRefreshSwitch.setOnCheckedChangeListener(null);
        backgroundRefreshSwitch.setChecked(hub.isDataObjectRefreshing());
        backgroundRefreshSwitch.setEnabled(true);

        backgroundRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            new Thread(this::stopAndStartFragmentRefreshing).start();
            if(isChecked && !hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null)) {
                onHubManualActionChosenCallback.setDataObjectRefreshing(true);
            }
            else if (!isChecked && hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null)) {
                onHubManualActionChosenCallback.setDataObjectRefreshing(false);
            }
        });
    }

    private void stopAndStartFragmentRefreshing() {
        isRefreshManageHubFragmentRunning = false;
        refreshManageHubFragmentHandler.removeCallbacks(refreshManageHubFragmentRunnable);
        try { Thread.sleep(STOP_TIME_AFTER_STATE_CHANGE); }
        catch (InterruptedException ignored) {}
        isRefreshManageHubFragmentRunning = true;
        refreshManageHubFragmentHandler.post(refreshManageHubFragmentRunnable);
    }

    private void showSwitchIrrigationSystemDialog(boolean isChecked) {
        isSwitchIrrigationSystemDialogVisible = true;
        String title = getString(R.string.irrigation_system_switch_off_dialog);
        if(isChecked) { title = getString(R.string.irrigation_system_switch_on_dialog); }
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setPositiveButton(getString(R.string.confirm_button), (dialogInterface, i) -> {
                    isSwitchIrrigationSystemDialogVisible = false;
                    onHubManualActionChosenCallback.setIrrigationSystemState(isChecked);
                }).setNegativeButton(getString(R.string.close_button), (dialogInterface, i) -> {
                        isSwitchIrrigationSystemDialogVisible = false;
                        dialogInterface.dismiss();
                }).setCancelable(false)
                .show();
    }

    private void showHttpErrorFaultDialog() {
        isHttpErrorFaultDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.retry_button),
                        ((dialogInterface, i) -> {
                            isHttpErrorFaultDialogVisible = false;
                            this.onHubManualActionChosenCallback.restartApp();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        onHubManualActionChosenCallback.restartApp();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
