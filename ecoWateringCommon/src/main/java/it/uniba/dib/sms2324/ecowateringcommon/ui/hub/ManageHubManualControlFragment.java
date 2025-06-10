package it.uniba.dib.sms2324.ecowateringcommon.ui.hub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.Calendar;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class ManageHubManualControlFragment extends ManageHubFragment {
    private OnHubManualActionChosenCallback onHubManualActionChosenCallback;
    public interface OnHubManualActionChosenCallback extends OnManageHubActionCallback{
        void automateEcoWateringSystem();
        void setIrrigationSystemState(boolean value);
        void setDataObjectRefreshing(boolean value);
        void scheduleIrrSys(int[] startingDate, int[] startingTime, int[] irrigationDuration);
    }
    private Button confirmSchedulingButton;
    private final InputFilter[] hoursInputFilters = new InputFilter[]{
            (source, start, end, dest, dStart, dEnd) -> {
                try {
                    String result = dest.toString().substring(0, dStart) +
                            source.subSequence(start, end) +
                            dest.toString().substring(dEnd);
                    int input = Integer.parseInt(result);
                    if(input >= 0 && input <= 11) return null;
                } catch (NumberFormatException ignored) {}
                return Common.VOID_STRING_VALUE;
            }
    };
    private final InputFilter[] minutesInputFilters = new InputFilter[]{
            (source, start, end, dest, dStart, dEnd) -> {
                try {
                    String result = dest.toString().substring(0, dStart) +
                            source.subSequence(start, end) +
                            dest.toString().substring(dEnd);
                    int input = Integer.parseInt(result);
                    if(input >= 0 && input <= 59) return null;
                } catch (NumberFormatException ignored) {}
                return Common.VOID_STRING_VALUE;
            }
    };
    private int[] irrigationDuration = new int[2];
    private EditText schedulingHoursEditText;
    private EditText schedulingMinutesEditText;
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().equals(Common.VOID_STRING_VALUE)) {
                if(confirmSchedulingButton.isEnabled()) {
                    confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), primary_color_70, requireContext().getTheme()));
                    confirmSchedulingButton.setEnabled(false);
                }
            }
            else if(((schedulingHoursEditText.getText().toString().equals(String.valueOf(0))) || (schedulingHoursEditText.getText().toString().equals(Common.VOID_STRING_VALUE))) &&
                    ((schedulingMinutesEditText.getText().toString().equals(String.valueOf(0))) || (schedulingMinutesEditText.getText().toString().equals(Common.VOID_STRING_VALUE)))) {
                if((confirmSchedulingButton.isEnabled())) {
                    confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), primary_color_70, requireContext().getTheme()));
                    confirmSchedulingButton.setEnabled(false);
                }
            }
            else if((!schedulingHoursEditText.getText().toString().equals(Common.VOID_STRING_VALUE)) && (!schedulingMinutesEditText.getText().toString().equals(Common.VOID_STRING_VALUE)) && (!confirmSchedulingButton.isEnabled())) {
                confirmSchedulingButton.setEnabled(true);
                confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), primary_color_50, requireContext().getTheme()));
            }
        }
    };
    private static int[] startingDate = new int[3];
    private static int[] startingTime = new int[2];
    private final DatePicker.OnDateChangedListener onDateChangedListener = (view, year, monthOfYear, dayOfMonth) -> {
        startingDate[0] = year;
        startingDate[1] = monthOfYear;
        startingDate[2] = dayOfMonth;
    };
    private final TimePicker.OnTimeChangedListener onTimeChangedListener = (view, hourOfDay, minute) -> {
        startingTime[0] = hourOfDay;
        startingTime[1] = minute;
    };
    private static boolean isSchedulingCardVisible;
    private static boolean isSwitchIrrigationSystemDialogVisible;
    private static boolean isIrrSysSchedulingConfirmDialogVisible;
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
            if(isSwitchIrrigationSystemDialogVisible) showSwitchIrrigationSystemDialog(view, !hub.getIrrigationSystem().getState());
            if(isSchedulingCardVisible) showSchedulingCard(view);
            if(isIrrSysSchedulingConfirmDialogVisible) showIrrSysSchedulingConfirmDialog(view);
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

        Button irrSysProgrammingButton = view.findViewById(R.id.irrSysProgrammingButton);
        irrSysProgrammingButton.setVisibility(View.VISIBLE);
        irrSysProgrammingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        irrSysProgrammingButton.setOnClickListener((v -> showSchedulingCard(view)));

        confirmSchedulingButton = view.findViewById(R.id.confirmSchedulingButton);
        if(confirmSchedulingButton.isEnabled())
            confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        else
            confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_70, requireContext().getTheme()));
        confirmSchedulingButton.setOnClickListener((v -> {
            if(isValidIntervalDate())
                requireActivity().runOnUiThread(() ->showIrrSysSchedulingConfirmDialog(view));
            else {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.invalid_pasted_time_toast), Toast.LENGTH_LONG).show());
                v.setOnClickListener(null);
            }
        }));

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
        if(view.findViewById(R.id.loadingCard).getVisibility() == View.VISIBLE)
            view.findViewById(R.id.loadingCard).setVisibility(View.GONE);
        irrigationSystemStateSwitchCompat.setOnCheckedChangeListener(
                (buttonView, isChecked) -> requireActivity().runOnUiThread(() ->
                        showSwitchIrrigationSystemDialog(view, isChecked))
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
            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
            if(isChecked && !hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null))
                onHubManualActionChosenCallback.setDataObjectRefreshing(true);
            else if (!isChecked && hub.isDataObjectRefreshing() && (onHubManualActionChosenCallback != null))
                onHubManualActionChosenCallback.setDataObjectRefreshing(false);
        });
    }

    private void showSchedulingCard(@NonNull View view) {
        isSchedulingCardVisible = true;
        view.findViewById(R.id.irrSysSchedulingCard).setVisibility(View.VISIBLE);

        Button cancelSchedulingButton = view.findViewById(R.id.cancelSchedulingButton);
        cancelSchedulingButton.setOnClickListener((v -> closeSchedulingCard(view)));

        long currentDate = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentDate);

        DatePicker schedulingStartingDatePicker = view.findViewById(R.id.schedulingStartingDatePicker);
        schedulingStartingDatePicker.setMinDate(currentDate);
        schedulingStartingDatePicker.setMaxDate(currentDate + (7 * 24 * 60 * 60 * 1000));
        schedulingStartingDatePicker.setOnDateChangedListener(onDateChangedListener);

        TimePicker schedulingStartingTimePicker = view.findViewById(R.id.schedulingStartingTimePicker);
        schedulingStartingTimePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        schedulingStartingTimePicker.setMinute(calendar.get(Calendar.MINUTE));
        schedulingStartingTimePicker.setOnTimeChangedListener(onTimeChangedListener);

        schedulingHoursEditText = view.findViewById(R.id.schedulingHoursEditText);
        schedulingHoursEditText.setText(String.valueOf(0));
        schedulingHoursEditText.setFilters(hoursInputFilters);
        schedulingHoursEditText.addTextChangedListener(textWatcher);

        schedulingMinutesEditText = view.findViewById(R.id.schedulingMinutesEditText);
        schedulingMinutesEditText.setText(String.valueOf(20));
        schedulingMinutesEditText.setFilters(minutesInputFilters);
        schedulingMinutesEditText.addTextChangedListener(textWatcher);
    }

    private void closeSchedulingCard(@NonNull View view) {
        isSchedulingCardVisible = false;
        view.findViewById(R.id.irrSysSchedulingCard).setVisibility(View.GONE);
    }

    private boolean isValidIntervalDate() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, startingDate[0]);
        start.set(Calendar.MONTH, startingDate[1]);
        start.set(Calendar.DAY_OF_MONTH, startingDate[2]);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        start.add(Calendar.HOUR_OF_DAY, startingTime[0]);
        start.add(Calendar.MINUTE, startingTime[1]);

        return start.getTimeInMillis() > System.currentTimeMillis();
    }

    private void stopAndStartFragmentRefreshing() {
        isRefreshManageHubFragmentRunning = false;
        refreshManageHubFragmentHandler.removeCallbacks(refreshManageHubFragmentRunnable);
        try { Thread.sleep(STOP_TIME_AFTER_STATE_CHANGE); }
        catch (InterruptedException ignored) {}
        isRefreshManageHubFragmentRunning = true;
        refreshManageHubFragmentHandler.post(refreshManageHubFragmentRunnable);
    }

    private void showSwitchIrrigationSystemDialog(@NonNull View view, boolean isChecked) {
        isSwitchIrrigationSystemDialogVisible = true;
        String title = getString(R.string.irrigation_system_switch_off_dialog);
        if(isChecked)
            title = getString(R.string.irrigation_system_switch_on_dialog);
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setPositiveButton(getString(R.string.confirm_button), (dialogInterface, i) -> {
                    isSwitchIrrigationSystemDialogVisible = false;
                    new Thread(this::stopAndStartFragmentRefreshing).start();
                    view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                    onHubManualActionChosenCallback.setIrrigationSystemState(isChecked);
                }).setNegativeButton(getString(R.string.close_button), (dialogInterface, i) -> {
                        isSwitchIrrigationSystemDialogVisible = false;
                        dialogInterface.dismiss();
                }).setCancelable(false)
                .show();
    }

    private void showIrrSysSchedulingConfirmDialog(@NonNull View view) {
        isIrrSysSchedulingConfirmDialogVisible = true;
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, startingDate[0]);
        start.set(Calendar.MONTH, startingDate[1]);
        start.set(Calendar.DAY_OF_MONTH, startingDate[2]);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.HOUR_OF_DAY, startingTime[0]);
        start.add(Calendar.MINUTE, startingTime[1]);

        StringBuilder message = new StringBuilder(getString(R.string.next_starting_label));
        message.append(": ").append(getString(R.string.date_builder_label, String.valueOf(startingDate[1]), String.valueOf(startingDate[2]), startingDate[0])).append(" - ")
                .append(startingTime[0]).append(":").append(startingTime[1]).append("\n");
        message.append(getString(R.string.next_stopping_label)).append(": ").append(getString(R.string.date_builder_label, String.valueOf(start.get(Calendar.MONTH)), String.valueOf(start.get(Calendar.DAY_OF_MONTH)), start.get(Calendar.YEAR))).append(" - ")
                .append(startingTime[0]).append(startingTime[1]);
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure_label))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        getString(R.string.confirm_button),
                        (dialogInterface, i) -> {
                            isIrrSysSchedulingConfirmDialogVisible = false;
                            new Thread(this::stopAndStartFragmentRefreshing).start();
                            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                            irrigationDuration[0] = Integer.parseInt(schedulingHoursEditText.getText().toString());
                            irrigationDuration[1] = Integer.parseInt(schedulingMinutesEditText.getText().toString());
                            this.onHubManualActionChosenCallback.scheduleIrrSys(startingDate, startingTime, irrigationDuration);
                        }
                ).setNegativeButton(
                        getString(R.string.cancel_button),
                        (dialogInterface, i) -> {
                            isIrrSysSchedulingConfirmDialogVisible = false;
                            dialogInterface.dismiss();
                        }
                ).show();
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
