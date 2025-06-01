package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationDailyPlan;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlanPreview;

public class AutomateSystemFragment extends Fragment {
    private static EcoWateringHub hub;
    private static String calledFrom;
    private static IrrigationPlanPreview irrigationPlanPreview;
    private final int primary_color;
    private final int primary_color_50;
    private OnAutomateSystemActionCallback onAutomateSystemActionCallback;
    public interface OnAutomateSystemActionCallback {
        void onAutomateSystemGoBack();
        void restartApp();
        void onAutomateSystemAgreeIrrigationPlan(@NonNull IrrigationPlanPreview irrigationPlanPreview);
    }
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onAutomateSystemActionCallback.onAutomateSystemGoBack();
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_no_item, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if(menuItem.getItemId() == android.R.id.home) {
                onAutomateSystemActionCallback.onAutomateSystemGoBack();
            }
            return false;
        }
    };
    private static boolean isAgreeIrrigationPlanConfirmDialogVisible;
    private static boolean isHttpErrorFaultDialogVisible;

    public AutomateSystemFragment() {
        this(hub, calledFrom);
    }
    public AutomateSystemFragment(EcoWateringHub hubObj, String calledFromString) {
        super(R.layout.fragment_automate_system);
        hub = hubObj;
        calledFrom = calledFromString;
        if(calledFromString.equals(Common.CALLED_FROM_HUB)) {
            this.primary_color = R.color.ew_primary_color_from_hub;
            this.primary_color_50 = R.color.ew_primary_color_50_from_hub;
        }
        else {
            this.primary_color = R.color.ew_primary_color_from_device;
            this.primary_color_50 = R.color.ew_primary_color_50_from_device;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnAutomateSystemActionCallback) {
            this.onAutomateSystemActionCallback = (OnAutomateSystemActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.onAutomateSystemActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback); // ON BACK PRESSED CALLBACK SETUP
        toolbarSetup(view);
        automateSystemAgreeButtonSetup(view);
        if(savedInstanceState == null) {
            Common.showLoadingFragment(view, R.id.mainFragmentLayout, R.id.includeLoadingFragment);
            IrrigationPlanPreview.getIrrigationPlanPreviewJsonString(hub, Common.getThisDeviceID(requireContext()), (response) -> {
                irrigationPlanPreview = new IrrigationPlanPreview(response);
                horizontalScrollViewMode(view);
                requireActivity().runOnUiThread(() -> Common.hideLoadingFragment(view, R.id.mainFragmentLayout, R.id.includeLoadingFragment));
            });
        }
        else {  // CONFIGURATION CHANGED CASE
            horizontalScrollViewMode(view);
            if(isAgreeIrrigationPlanConfirmDialogVisible) showAgreeIrrigationPlanConfirmDialog();
            else if(isHttpErrorFaultDialogVisible) showHttpErrorFaultDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Common.unlockLayout(requireActivity());
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primary_color, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(ResourcesCompat.getDrawable(getResources(), R.drawable.back_icon, requireContext().getTheme()));
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void horizontalScrollViewMode(@NonNull View view) {
        LinearLayout container = view.findViewById(R.id.scrollViewInnerLinearLayout);
        for(IrrigationDailyPlan irrigationDailyPlan : irrigationPlanPreview.getIrrigationDailyPlanList()) {
            container.addView(irrigationDailyPlan.drawIrrigationDailyPlan(requireContext(), container, this.primary_color_50));
        }
    }

    private void automateSystemAgreeButtonSetup(@NonNull View view) {
        Button automateSystemAgreeButton = view.findViewById(R.id.automateSystemAgreeButton);
        automateSystemAgreeButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), this.primary_color, requireContext().getTheme()));
        automateSystemAgreeButton.setOnClickListener((v) -> requireActivity().runOnUiThread(this::showAgreeIrrigationPlanConfirmDialog));
    }

    private void showAgreeIrrigationPlanConfirmDialog() {
        isAgreeIrrigationPlanConfirmDialogVisible = true;
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.irrigation_plan_agree_dialog_title))
                .setMessage(getString(R.string.irrigation_plan_agree_dialog_message))
                .setPositiveButton(
                        getString(R.string.confirm_button),
                        (dialogInterface, i) -> {
                            isAgreeIrrigationPlanConfirmDialogVisible = false;
                            this.onAutomateSystemActionCallback.onAutomateSystemAgreeIrrigationPlan(irrigationPlanPreview);
                        }
                ).setNegativeButton(getString(R.string.close_button), (dialogInterface, i) -> {
                    isAgreeIrrigationPlanConfirmDialogVisible = false;
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
                            this.onAutomateSystemActionCallback.restartApp();
                        })
                )
                .setOnKeyListener((dialogInterface, keyCode, keyEvent) -> {
                    if(keyCode == KeyEvent.KEYCODE_BACK) {
                        isHttpErrorFaultDialogVisible = false;
                        this.onAutomateSystemActionCallback.restartApp();
                    }
                    return false;
                })
                .setCancelable(false)
                .create()
                .show();
    }
}
