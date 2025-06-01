package it.uniba.dib.sms2324.ecowateringcommon.ui.hub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystemActivityLogInstance;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystemActivityLogInstanceAdapter;

public class ManageHubAutomaticControlFragment extends ManageHubFragment {
    private Toast irrigationSystemToast;
    private Toast backgroundRefreshingToast;
    private ConstraintLayout automatedIrrigationSystemInfoCard;
    private ConstraintLayout historyCard;
    private static boolean isIrrigationInfoCardVisible;
    private static boolean isActivityLogHistoryVisible;
    private static boolean isHttpErrorFaultDialogVisible;
    private static boolean isControlSystemManuallyDialogVisible;
    private OnManageHubAutomaticControlActionCallback onManageHubAutomaticControlActionCallback;
    public interface OnManageHubAutomaticControlActionCallback extends ManageHubFragment.OnManageHubActionCallback {
        void controlSystemManually();
    }

    public ManageHubAutomaticControlFragment() {
        this(ManageHubFragment.calledFrom, ManageHubFragment.hub);
    }
    public ManageHubAutomaticControlFragment(String calledFromString, @NonNull EcoWateringHub hubObj) {
        super(calledFromString, hubObj);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnManageHubAutomaticControlActionCallback) {
            this.onManageHubAutomaticControlActionCallback = (OnManageHubAutomaticControlActionCallback) context;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        this.onManageHubAutomaticControlActionCallback = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState != null) {
            if(isIrrigationInfoCardVisible) showAutomatedIrrigationSystemInfoCard(view);
            if(isActivityLogHistoryVisible) showIrrigationSystemHistory(view);
            // DIALOG RECOVERING
            if(isControlSystemManuallyDialogVisible) showControlSystemManuallyDialog();
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void specializedViewSetup() {
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
                if(onManageHubAutomaticControlActionCallback != null) onManageHubAutomaticControlActionCallback.refreshDataObject((ecoWateringHub) -> {
                    hub = ecoWateringHub;
                    if (!hub.isAutomated()) onManageHubAutomaticControlActionCallback.restartApp();
                    else if (getView() != null) requireActivity().runOnUiThread(() -> manageHubViewSetup(getView()));
                });
                if(isRefreshManageHubFragmentRunning) refreshManageHubFragmentHandler.postDelayed(this, REFRESH_FRAGMENT_FREQUENCY);
            }
        };
    }

    private void irrigationSystemCardSetup(@NonNull View view) {
        view.findViewById(R.id.titleIrrigationSystemCard).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme())); // TITLE BACKGROUND COLOR
        //  NEXT IRRIGATION SYSTEM SWITCH ON/OFF TIME INFO
        TextView irrigationSystemNextActionTime = view.findViewById(R.id.irrigationSystemNextActionTimeTextView);
        irrigationSystemNextActionTime.setVisibility(View.VISIBLE);
        String irrigationSystemNextActionPlannedString = hub.getIrrigationPlan().getNextIrrigationActionTime(requireContext(), hub.getIrrigationSystem().getState());
        irrigationSystemNextActionTime.setText(irrigationSystemNextActionPlannedString);
        //  SWITCH COMPAT SETUP
        SwitchCompat irrigationSystemStateSwitchCompat = view.findViewById(R.id.irrigationSystemStateSwitchCompat);
        irrigationSystemStateSwitchCompat.setFocusable(false);
        irrigationSystemStateSwitchCompat.setClickable(false);
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(null);
        irrigationSystemStateSwitchCompat.setChecked(hub.getIrrigationSystem().getState());
        //  IRRIGATION SYSTEM STATE TEXTVIEW SETUP
        if(hub.getIrrigationSystem().getState()) ((TextView) view.findViewById(R.id.irrigationSystemValueStateTextView)).setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.on_value));
        else ((TextView) view.findViewById(R.id.irrigationSystemValueStateTextView)).setText(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.off_value));
        // FAKE CONSTRAINT LAYOUT TO SIMULATE SWITCH COMPAT CLICK
        ConstraintLayout irrigationSystemStateSwitchCompatClickable = view.findViewById(R.id.irrigationSystemStateSwitchCompatClickable);
        irrigationSystemStateSwitchCompatClickable.setVisibility(View.VISIBLE);
        irrigationSystemStateSwitchCompatClickable.setClickable(true);
        irrigationSystemStateSwitchCompatClickable.setFocusable(true);
        irrigationSystemStateSwitchCompatClickable.setOnClickListener((v -> {
            this.irrigationSystemToast = Toast.makeText(requireContext(), getString(R.string.cannot_control_irrigation_system_manually_toast), Toast.LENGTH_LONG);
            if((this.irrigationSystemToast.getView() == null) || ((this.irrigationSystemToast.getView() != null) && (!this.irrigationSystemToast.getView().isShown()))) requireActivity().runOnUiThread(() -> this.irrigationSystemToast.show());
            v.setOnClickListener(null);
        }));
        // EXPAND AUTOMATE IRRIGATION SYSTEM INFO SETUP
        ConstraintLayout expandAutomateIrrigationSystemInfoContainer = view.findViewById(R.id.expandAutomateIrrigationSystemInfoContainer);
        expandAutomateIrrigationSystemInfoContainer.setVisibility(View.VISIBLE);
        automatedIrrigationSystemInfoCard = view.findViewById(R.id.automatedIrrigationSystemInfoCard);
        expandAutomateIrrigationSystemInfoContainer.setOnClickListener((v -> {
            if(automatedIrrigationSystemInfoCard.getVisibility() == View.VISIBLE) hideAutomatedIrrigationSystemInfoCard(view);
            else showAutomatedIrrigationSystemInfoCard(view);
        }));
    }

    private void automateEcoWateringSystemCardSetup(@NonNull View view) {
        TextView automatedStateMainTextView = view.findViewById(R.id.automatedStateMainTextView);
        automatedStateMainTextView.setText(getString(R.string.automated_system_label));
        automatedStateMainTextView.setTextColor(ResourcesCompat.getColorStateList(getResources(), R.color.ew_primary_color_from_hub, requireActivity().getTheme()));
        ((TextView) view.findViewById(R.id.automateStateSuggestTextView)).setText(getString(R.string.automated_system_message_label));
        SwitchCompat automateControlSwitchCompat = view.findViewById(R.id.irrigationSystemAutomateControlSwitchCompat);
        automateControlSwitchCompat.setOnCheckedChangeListener(null);
        automateControlSwitchCompat.setChecked(true);
        automateControlSwitchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isChecked) requireActivity().runOnUiThread(this::showControlSystemManuallyDialog);
        });
    }

    private void backgroundRefreshCardSetup(@NonNull View view) {
        // TO SIMULATE CLICK
        ConstraintLayout dataObjectRefreshCard = view.findViewById(R.id.dataObjectRefreshCard);
        dataObjectRefreshCard.setFocusable(true);
        dataObjectRefreshCard.setClickable(true);
        dataObjectRefreshCard.setOnClickListener((v) -> {
            this.backgroundRefreshingToast = Toast.makeText(requireContext(), getString(R.string.cannot_disable_background_refreshing_toast), Toast.LENGTH_LONG);
            if((this.backgroundRefreshingToast.getView() == null) || ((this.backgroundRefreshingToast.getView() != null) && (!this.backgroundRefreshingToast.getView().isShown()))) {
                requireActivity().runOnUiThread(() -> this.backgroundRefreshingToast.show());
                v.setOnClickListener(null);
            }
        });
        // SWITCH SETUP
        SwitchCompat backgroundRefreshSwitch = view.findViewById(R.id.dataObjectRefreshSwitch);
        backgroundRefreshSwitch.setOnCheckedChangeListener(null);
        backgroundRefreshSwitch.setChecked(true);
        backgroundRefreshSwitch.setClickable(false);
        backgroundRefreshSwitch.setFocusable(false);
    }

    private void showAutomatedIrrigationSystemInfoCard(@NonNull View view) {
        isIrrigationInfoCardVisible = true;
        view.findViewById(R.id.openIrrigationSystemInfoButtonImageView).setRotation(90);
        // MAKE INFORMATION VISIBLE
        automatedIrrigationSystemInfoCard.setVisibility(View.VISIBLE);
        // ACTIVATION DAYS AGO SETUP
        if((hub.getIrrigationSystem().getActivityLog() != null) && (!hub.getIrrigationSystem().getActivityLog().isEmpty()) && (hub.getIrrigationSystem().getActivityLog().get(0).getAutomatedSystemDays() > 0)) {
            String daysAgo = requireContext().getResources().getQuantityString(R.plurals.system_automated_timing_measurement_label, hub.getIrrigationSystem().getActivityLog().get(0).getAutomatedSystemDays(), hub.getIrrigationSystem().getActivityLog().get(0).getAutomatedSystemDays());
            ((TextView) view.findViewById(R.id.systemAutomatedTimingTextView)).setText(daysAgo);
        }
        else ((TextView) view.findViewById(R.id.systemAutomatedTimingTextView)).setText(getString(R.string.today_label));
        //  IRRIGATION MINUTES SAVED SETUP
        ((TextView) view.findViewById(R.id.irrigationTimeSavedTextView)).setText(String.valueOf(hub.getIrrigationSystem().getIrrigationSavedMinutes()));
        ((TextView) view.findViewById(R.id.irrigationTimeEstimatedTodayTextView)).setText(String.valueOf(((int) hub.getIrrigationPlan().getIrrigationDailyPlanList().get(0).getIrrigationMinutesPlan())));
        //  IRRIGATION HISTORY BUTTON SETUP
        Button showIrrigationSystemHistoryButton = view.findViewById(R.id.showIrrigationSystemHistoryButton);
        showIrrigationSystemHistoryButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        showIrrigationSystemHistoryButton.setOnClickListener((v -> showIrrigationSystemHistory(view)));
    }

    private void hideAutomatedIrrigationSystemInfoCard(@NonNull View view) {
        isIrrigationInfoCardVisible = false;
        view.findViewById(R.id.openIrrigationSystemInfoButtonImageView).setRotation(270);
        automatedIrrigationSystemInfoCard.setVisibility(View.GONE);
    }

    private void showIrrigationSystemHistory(@NonNull View view) {
        isActivityLogHistoryVisible = true;
        historyCard = view.findViewById(R.id.historyCard);
        historyCard.setVisibility(View.VISIBLE);
        ListView historyListView = view.findViewById(R.id.historyListView);
        ArrayList<IrrigationSystemActivityLogInstance> activityLogArrayList = new ArrayList<>();
        IrrigationSystemActivityLogInstanceAdapter adapter = new IrrigationSystemActivityLogInstanceAdapter(requireContext(), activityLogArrayList, calledFrom);
        historyListView.setAdapter(adapter);
        if((hub.getIrrigationSystem() != null) && (hub.getIrrigationSystem().getActivityLog() != null) && (!hub.getIrrigationSystem().getActivityLog().isEmpty())) {
            for(IrrigationSystemActivityLogInstance instance : hub.getIrrigationSystem().getActivityLog()) {
                activityLogArrayList.add(instance);
                requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
            }
        }
        // HIDE BUTTON SETUP
        ImageView hideHistoryImageViewButton = view.findViewById(R.id.hideHistoryImageViewButton);
        hideHistoryImageViewButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), R.color.ew_secondary_color, requireContext().getTheme()));
        hideHistoryImageViewButton.setOnClickListener((v -> {
            isActivityLogHistoryVisible = false;
            historyCard.setVisibility(View.GONE);
        }));
    }

    private void showControlSystemManuallyDialog() {
        isControlSystemManuallyDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure_label))
                .setMessage(getString(R.string.control_system_manually_dialog_message))
                .setPositiveButton(
                        getString(R.string.confirm_button),
                        (dialogInterface, i) -> {
                            isControlSystemManuallyDialogVisible = false;
                            this.onManageHubAutomaticControlActionCallback.controlSystemManually();
                        }
                ).setNegativeButton(
                        getString(R.string.close_button),
                        (dialogInterface, i) -> {
                            isControlSystemManuallyDialogVisible = false;
                            dialogInterface.dismiss();
                        }
                ).setCancelable(false)
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
                            onManageHubAutomaticControlActionCallback.restartApp();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        onManageHubAutomaticControlActionCallback.restartApp();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
