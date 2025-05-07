package it.uniba.dib.sms2324.ecowateringcommon.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class UserProfileFragment extends Fragment {
    private static final String EDITED_USER_NAME = "EDITED_USER_NAME";
    private final int primaryColor;
    private final int primaryColor70;
    private static String calledFrom;
    private static String currentUserName;
    private EditText userNameEditText;
    private ConstraintLayout enableEditUserNameButton;
    private ConstraintLayout editUserNameButtonsContainer;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if(editUserNameButtonsContainer.getVisibility() == View.VISIBLE) {
                showChangesWillBeLostDialog();
            }
            else {
                onUserProfileActionCallback.onUserProfileGoBack();
            }
        }
    };
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if(itemId == android.R.id.home) {
                if(editUserNameButtonsContainer.getVisibility() == View.VISIBLE) {
                    showChangesWillBeLostDialog();
                }
                else {
                    onUserProfileActionCallback.onUserProfileGoBack();
                }
            }
            else if(itemId == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem) {
                onUserProfileActionCallback.onUserProfileRefresh();
            }
            return false;
        }
    };
    private OnUserProfileActionCallback onUserProfileActionCallback;
    public interface OnUserProfileActionCallback {
        void onUserProfileGoBack();
        void onUserProfileRefresh();
        void restartApp();
    }
    public UserProfileFragment() {
        this(calledFrom);
        Log.i(Common.THIS_LOG, "UserProfileFragment no parameter constructor, calledFrom: " + calledFrom);
    }
    public UserProfileFragment(String calledFromString) {
        super(R.layout.fragment_user_profile);
        calledFrom = calledFromString;
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
            this.primaryColor = R.color.ew_primary_color_from_hub;
            this.primaryColor70 = R.color.ew_primary_color_70_from_hub;
        }
        else {
            this.primaryColor = R.color.ew_primary_color_from_device;
            this.primaryColor70 = R.color.ew_primary_color_70_from_device;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnUserProfileActionCallback) {
            onUserProfileActionCallback = (OnUserProfileActionCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onUserProfileActionCallback = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);    // ON BACK PRESSED CALLBACK SETUP

        toolbarSetup(view);
        fragmentLayoutSetup(view);

        if(savedInstanceState == null) {    // FIRST USER NAME RECOVERING
            getCurrentName((name) -> {
                currentUserName = name;
                requireActivity().runOnUiThread(() -> {
                    this.userNameEditText.setText(currentUserName);
                    lockUserNameEdit();
                });
            });
            Log.i(Common.THIS_LOG, "UserProfileFragment -> onViewCreated() savedInstanceState: null");
        }
        else {    // EDITED USER NAME RECOVERING FROM CONFIGURATION CHANGES
            Log.i(Common.THIS_LOG, "UserProfileFragment -> onViewCreated() savedInstanceState: not null");
            if(savedInstanceState.getString(EDITED_USER_NAME) != null) {
                Log.i(Common.THIS_LOG, "UserProfileFragment -> onViewCreated() savedInstanceState.getString(EDITED_USER_NAME) not null: " + savedInstanceState.getString(EDITED_USER_NAME));
                unlockUserNameEdit();
                this.userNameEditText.setText(savedInstanceState.getString(EDITED_USER_NAME));
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(editUserNameButtonsContainer.getVisibility() == View.VISIBLE) {
            outState.putString(EDITED_USER_NAME, userNameEditText.getText().toString());
        }
    }

    private void toolbarSetup(@NonNull View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if(toolbar != null) {
            toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primaryColor, requireActivity().getTheme()));
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowTitleEnabled(false);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }

    private void fragmentLayoutSetup(@NonNull View view) {
        this.editUserNameButtonsContainer = view.findViewById(R.id.editUserNameButtonsContainer);
        userNameEditTextSetup(view);
        this.enableEditUserNameButton = view.findViewById(R.id.enableEditUserNameButton);
        this.enableEditUserNameButton.setOnClickListener((v) -> unlockUserNameEdit());

        Button editUserNameCancelButton = view.findViewById(R.id.editUserNameCancelButton);
        editUserNameCancelButton.setOnClickListener((v) -> {
            this.userNameEditText.setText(currentUserName);
            lockUserNameEdit();
        });

        Button editUserNameConfirmButton = view.findViewById(R.id.editUserNameConfirmButton);
        editUserNameConfirmButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), this.primaryColor, requireActivity().getTheme()));
        editUserNameConfirmButton.setOnClickListener((v) -> showEditUserNameConfirmDialog());

        Button deleteAccountButton = view.findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener((v) -> showDeleteAccountConfirmDialog());
    }

    private void getCurrentName(Common.OnStringResponseGivenCallback callback) {
        if(calledFrom.equals(Common.CALLED_FROM_DEVICE)) {
            EcoWateringDevice.getEcoWateringDeviceJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> callback.getResponse((new EcoWateringDevice(jsonResponse)).getName()));
        }
        else if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
            EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(requireContext()), (jsonResponse) -> callback.getResponse((new EcoWateringHub(jsonResponse)).getName()));
        }
    }

    private void userNameEditTextSetup(@NonNull View view) {
        this.userNameEditText = view.findViewById(R.id.userNameEditText);
        this.userNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("") || s.toString().equals(currentUserName)) {
                    view.findViewById(R.id.editUserNameConfirmButton).setEnabled(false);
                    view.findViewById(R.id.editUserNameConfirmButton).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), primaryColor70, requireActivity().getTheme()));
                }
                else {
                    view.findViewById(R.id.editUserNameConfirmButton).setEnabled(true);
                    view.findViewById(R.id.editUserNameConfirmButton).setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), primaryColor, requireActivity().getTheme()));
                }
            }
        });
    }

    private void lockUserNameEdit() {
        this.enableEditUserNameButton.setEnabled(true);
        this.enableEditUserNameButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.color.ew_secondary_color, requireContext().getTheme()));
        this.editUserNameButtonsContainer.setVisibility(View.GONE);
        this.userNameEditText.setEnabled(false);
        this.userNameEditText.setTextColor(ResourcesCompat.getColor(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.color.black_white_30, requireActivity().getTheme()));
    }

    private void unlockUserNameEdit() {
        this.enableEditUserNameButton.setEnabled(false);
        this.enableEditUserNameButton.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), it.uniba.dib.sms2324.ecowateringcommon.R.color.ew_secondary_color_90, requireContext().getTheme()));
        this.editUserNameButtonsContainer.setVisibility(View.VISIBLE);
        this.userNameEditText.setEnabled(true);
        this.userNameEditText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.black_white_0, requireActivity().getTheme()));
    }

    private void showChangesWillBeLostDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.changes_not_saved_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.changes_not_saved_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> onUserProfileActionCallback.onUserProfileGoBack())
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void showEditUserNameConfirmDialog() {
        String message = getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.new_name_label) + ": " + this.userNameEditText.getText().toString();
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure_label))
                .setMessage(message)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> {
                            if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
                                EcoWateringHub.setName(requireContext(), this.userNameEditText.getText().toString(), (response) -> {
                                    if(response.equals(EcoWateringHub.HUB_NAME_CHANGED_RESPONSE)) {
                                        onUserProfileActionCallback.onUserProfileRefresh();
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showErrorDialog);
                                    }
                                });
                            }
                            else {
                                EcoWateringDevice.setName(requireContext(), this.userNameEditText.getText().toString(), (response) -> {
                                    if(response.equals(EcoWateringDevice.DEVICE_NAME_CHANGED_RESPONSE)) {
                                        onUserProfileActionCallback.onUserProfileRefresh();
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showErrorDialog);
                                    }
                                });
                            }
                        })
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.cancel_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void showDeleteAccountConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.delete_account_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.delete_account_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.confirm_button),
                        (dialogInterface, i) -> {
                            if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
                                EcoWateringHub.deleteAccount(requireContext(), (response) -> {
                                    if(response.equals(EcoWateringHub.DEVICE_HUB_ACCOUNT_RESPONSE)) {
                                        requireActivity().runOnUiThread(this::showDeviceAccountDeletedDialog);
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showErrorDialog);
                                    }
                                });
                            }
                            else {
                                EcoWateringDevice.deleteAccount(requireContext(), (response) -> {
                                    if(response.equals(EcoWateringDevice.DEVICE_DELETE_ACCOUNT_RESPONSE)) {
                                        requireActivity().runOnUiThread(this::showDeviceAccountDeletedDialog);
                                    }
                                    else {
                                        requireActivity().runOnUiThread(this::showErrorDialog);
                                    }
                                });
                            }
                        })
                .setNegativeButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void showDeviceAccountDeletedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(it.uniba.dib.sms2324.ecowateringcommon.R.string.device_account_successful_deleted_title)
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> onUserProfileActionCallback.restartApp())
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        (dialogInterface, i) -> onUserProfileActionCallback.restartApp())
                .setCancelable(false)
                .show();
    }
}