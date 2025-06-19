package it.uniba.dib.sms2324.ecowateringcommon.ui.hub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
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
import java.util.Locale;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlan;

public class ManageHubManualControlFragment extends ManageHubFragment {
    private OnHubManualActionChosenCallback onHubManualActionChosenCallback;
    public interface OnHubManualActionChosenCallback extends OnManageHubActionCallback{
        void automateEcoWateringSystem();
        void setIrrigationSystemState(boolean value);
        void setDataObjectRefreshing(boolean value);
        void scheduleIrrSys(Calendar calendar, int[] irrigationDuration);
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
    private final static String[] stringIrrigationDuration = new String[2];
    private DatePicker schedulingStartingDatePicker;
    private TimePicker schedulingStartingTimePicker;
    private static boolean isScheduleCardVisible;
    private static boolean isSwitchIrrigationSystemDialogVisible;
    private static boolean isIrrSysScheduleConfirmDialogVisible;
    private static boolean isSchedulingCardVisible;
    private static boolean isDeleteSchedulingDialogVisible;
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
            schedulingHoursEditText.setText(stringIrrigationDuration[0]);
            schedulingMinutesEditText.setText(stringIrrigationDuration[1]);
            if(isSwitchIrrigationSystemDialogVisible) showSwitchIrrigationSystemDialog(view, !hub.getIrrigationSystem().getState());
            if(isScheduleCardVisible) showScheduleCard(view);
            if(isSchedulingCardVisible) showSchedulingCard((view));
            if(isDeleteSchedulingDialogVisible) showDeleteSchedulingDialog(view);
            if(isIrrSysScheduleConfirmDialogVisible) showIrrSysScheduleConfirmDialog(view);
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        stringIrrigationDuration[0] = schedulingHoursEditText.getText().toString();
        stringIrrigationDuration[1] = schedulingMinutesEditText.getText().toString();
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
        view.findViewById(R.id.irrSysSchedulingButtonsContainer).setVisibility(View.VISIBLE);
        // SCHEDULING CARD SETUP
        Button irrSysShowSchedulingButton = view.findViewById(R.id.irrSysShowSchedulingButton);
        if(hub.getIrrigationSystem().getIrrigationSystemScheduling() != null) {
            irrSysShowSchedulingButton.setEnabled(true);
            irrSysShowSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), R.color.ew_secondary_color, requireContext().getTheme()));
            irrSysShowSchedulingButton.setOnClickListener((v -> showSchedulingCard(view)));
        }
        else {
            irrSysShowSchedulingButton.setEnabled(false);
            irrSysShowSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), R.color.ew_secondary_color_90, requireContext().getTheme()));
        }
        // SCHEDULE CARD SETUP
        Button irrSysScheduleButton = view.findViewById(R.id.irrSysScheduleButton);
        irrSysScheduleButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        irrSysScheduleButton.setOnClickListener((v -> showScheduleCard(view)));

        confirmSchedulingButton = view.findViewById(R.id.confirmSchedulingButton);
        if(confirmSchedulingButton.isEnabled())
            confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_50, requireContext().getTheme()));
        else
            confirmSchedulingButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color_70, requireContext().getTheme()));
        confirmSchedulingButton.setOnClickListener((v -> {
            if(isValidIntervalDate())
                requireActivity().runOnUiThread(() -> showIrrSysScheduleConfirmDialog(view));
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

    private void showScheduleCard(@NonNull View view) {
        isScheduleCardVisible = true;
        view.findViewById(R.id.irrSysScheduleCard).setVisibility(View.VISIBLE);

        Button cancelSchedulingButton = view.findViewById(R.id.cancelSchedulingButton);
        cancelSchedulingButton.setOnClickListener((v -> hideScheduleCard(view)));

        long currentDate = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentDate);


        this.schedulingStartingDatePicker = view.findViewById(R.id.schedulingStartingDatePicker);
        schedulingStartingDatePicker.setMinDate(currentDate);
        schedulingStartingDatePicker.setMaxDate(currentDate + (21 * 24 * 60 * 60 * 1000));

        this.schedulingStartingTimePicker = view.findViewById(R.id.schedulingStartingTimePicker);
        if(!Locale.getDefault().getCountry().equals(Locale.ENGLISH.getCountry()))
            schedulingStartingTimePicker.setIs24HourView(true);
        schedulingStartingTimePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        schedulingStartingTimePicker.setMinute(calendar.get(Calendar.MINUTE)+1);

        schedulingHoursEditText = view.findViewById(R.id.schedulingHoursEditText);
        schedulingHoursEditText.setText(String.valueOf(0));
        schedulingHoursEditText.setFilters(hoursInputFilters);
        schedulingHoursEditText.addTextChangedListener(textWatcher);

        schedulingMinutesEditText = view.findViewById(R.id.schedulingMinutesEditText);
        schedulingMinutesEditText.setText(String.valueOf((int) IrrigationPlan.BASE_DAILY_IRRIGATION_MINUTES));
        schedulingMinutesEditText.setFilters(minutesInputFilters);
        schedulingMinutesEditText.addTextChangedListener(textWatcher);
        stringIrrigationDuration[0] = String.valueOf((int) IrrigationPlan.BASE_DAILY_IRRIGATION_MINUTES);
    }

    private void hideScheduleCard(@NonNull View view) {
        isScheduleCardVisible = false;
        view.findViewById(R.id.irrSysScheduleCard).setVisibility(View.GONE);
    }

    private void showSchedulingCard(@NonNull View view) {
        isSchedulingCardVisible = true;
        view.findViewById(R.id.irrSysSchedulingCard).setVisibility(View.VISIBLE);
        requireActivity().runOnUiThread(() -> hub.getIrrigationSystem().getIrrigationSystemScheduling().draw(
                requireContext(),
                view,
                this.primary_color_50,
                (v -> hideSchedulingCard(view)),
                (v -> {
                    isSchedulingCardVisible = false;
                    showDeleteSchedulingDialog(view);
                })
        ));
    }

    private void hideSchedulingCard(@NonNull View view) {
        isSchedulingCardVisible = false;
        view.findViewById(R.id.irrSysSchedulingCard).setVisibility(View.GONE);
    }

    private boolean isValidIntervalDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, schedulingStartingDatePicker.getYear());
        start.set(Calendar.MONTH, schedulingStartingDatePicker.getMonth());
        start.set(Calendar.DAY_OF_MONTH, schedulingStartingDatePicker.getDayOfMonth());
        start.set(Calendar.HOUR_OF_DAY, schedulingStartingTimePicker.getHour());
        start.set(Calendar.MINUTE, schedulingStartingTimePicker.getMinute());

        Log.i(Common.LOG_NORMAL, "---------> isValidIntervalDate start: " + start.getTime());
        Log.i(Common.LOG_NORMAL, "---------> isValidIntervalDate calen: " + calendar.getTime());
        return start.getTimeInMillis() > calendar.getTimeInMillis();
    }

    private void stopAndStartFragmentRefreshing() {
        isRefreshManageHubFragmentRunning = false;
        refreshManageHubFragmentHandler.removeCallbacks(refreshManageHubFragmentRunnable);
        try { Thread.sleep(STOP_TIME_AFTER_STATE_CHANGE); }
        catch (InterruptedException ignored) {}
        isRefreshManageHubFragmentRunning = true;
        refreshManageHubFragmentHandler.post(refreshManageHubFragmentRunnable);
    }

    private String getSchedulingMessage() {
        String minutesStartingTime = String.valueOf(schedulingStartingTimePicker.getMinute());
        if(schedulingStartingTimePicker.getMinute() < 10)
            minutesStartingTime = "0" + schedulingStartingTimePicker.getMinute();

        String day = String.valueOf(schedulingStartingDatePicker.getDayOfMonth());
        if(getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Common.LANGUAGE_ENGLISH))
            day = Common.concatDayEnglishLanguage(schedulingStartingDatePicker.getDayOfMonth(), day);

        StringBuilder message = new StringBuilder(getString(R.string.next_starting_label));
        message.append(":\n     ").append(getString(R.string.date_builder_extended_label, getResources().getStringArray(R.array.month_names)[schedulingStartingDatePicker.getMonth()], day, schedulingStartingDatePicker.getYear())).append(" - ")
                .append(schedulingStartingTimePicker.getHour()).append(":").append(minutesStartingTime).append("\n\n");

        Calendar startDateCalendar = Calendar.getInstance();
        startDateCalendar.set(Calendar.YEAR, schedulingStartingDatePicker.getYear());
        startDateCalendar.set(Calendar.MONTH, schedulingStartingDatePicker.getMonth());
        startDateCalendar.set(Calendar.DAY_OF_MONTH, schedulingStartingDatePicker.getDayOfMonth());
        startDateCalendar.set(Calendar.HOUR_OF_DAY, schedulingStartingTimePicker.getHour());
        startDateCalendar.set(Calendar.MINUTE, schedulingStartingTimePicker.getMinute());
        startDateCalendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(schedulingHoursEditText.getText().toString()));
        startDateCalendar.add(Calendar.MINUTE, Integer.parseInt(schedulingMinutesEditText.getText().toString()));

        day = String.valueOf(startDateCalendar.get(Calendar.DAY_OF_MONTH));
        if(getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Common.LANGUAGE_ENGLISH))
            day = Common.concatDayEnglishLanguage(startDateCalendar.get(Calendar.DAY_OF_MONTH), day);
        String date = getString(R.string.date_builder_extended_label, getResources().getStringArray(R.array.month_names)[startDateCalendar.get(Calendar.MONTH)-1], day, startDateCalendar.get(Calendar.YEAR));
        message.append(getString(R.string.next_stopping_label)).append(":\n     ").append(date).append(" - ")
                .append(startDateCalendar.get(Calendar.HOUR_OF_DAY)).append(":").append(startDateCalendar.get(Calendar.MINUTE));
        message.append("\n\n").append(getString(R.string.irr_sys_already_scheduled_title));
        return message.toString();
    }

    private void showSwitchIrrigationSystemDialog(@NonNull View view, boolean isChecked) {
        isSwitchIrrigationSystemDialogVisible = true;
        String title = getString(R.string.irrigation_system_switch_off_dialog);
        if(isChecked)
            title = getString(R.string.irrigation_system_switch_on_dialog);
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(getString(R.string.irr_sys_switch_message))
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

    private void showIrrSysScheduleConfirmDialog(@NonNull View view) {
        isIrrSysScheduleConfirmDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure_label))
                .setMessage(getSchedulingMessage())
                .setCancelable(false)
                .setPositiveButton(getString(R.string.confirm_button), (dialogInterface, i) -> {
                            isIrrSysScheduleConfirmDialogVisible = false;
                            new Thread(this::stopAndStartFragmentRefreshing).start();
                            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                            Calendar startDateCalendar = Calendar.getInstance();
                            startDateCalendar.set(Calendar.YEAR, schedulingStartingDatePicker.getYear());
                            startDateCalendar.set(Calendar.MONTH, schedulingStartingDatePicker.getMonth());
                            startDateCalendar.set(Calendar.DAY_OF_MONTH, schedulingStartingDatePicker.getDayOfMonth());
                            startDateCalendar.set(Calendar.HOUR_OF_DAY, schedulingStartingTimePicker.getHour());
                            startDateCalendar.set(Calendar.MINUTE, schedulingStartingTimePicker.getMinute());
                            int[] irrigationDuration = new int[2];
                            irrigationDuration[0] = Integer.parseInt(schedulingHoursEditText.getText().toString());
                            irrigationDuration[1] = Integer.parseInt(schedulingMinutesEditText.getText().toString());
                            this.onHubManualActionChosenCallback.scheduleIrrSys(startDateCalendar, irrigationDuration);
                            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                            hideScheduleCard(view);
                        }
                ).setNegativeButton(getString(R.string.cancel_button), (dialogInterface, i) -> {
                    isIrrSysScheduleConfirmDialogVisible = false;
                    dialogInterface.dismiss();
                }).show();
    }

    private void showDeleteSchedulingDialog(@NonNull View view) {
        isDeleteSchedulingDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_scheduling_title_dialog))
                .setPositiveButton(
                        getString(R.string.confirm_button),
                        ((dialogInterface, i) -> {
                            isDeleteSchedulingDialogVisible = false;
                            new Thread(this::stopAndStartFragmentRefreshing).start();
                            int[] nullInt = {0,0};
                            onHubManualActionChosenCallback.scheduleIrrSys(null, nullInt);
                            hideSchedulingCard(view);
                            view.findViewById(R.id.loadingCard).setVisibility(View.VISIBLE);
                        }))
                .setNegativeButton(
                        getString(R.string.close_button),
                        ((dialogInterface, i) -> {
                            isDeleteSchedulingDialogVisible = false;
                            dialogInterface.dismiss();
                        })
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
