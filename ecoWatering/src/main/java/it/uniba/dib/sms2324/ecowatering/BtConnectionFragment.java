package it.uniba.dib.sms2324.ecowatering;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import it.uniba.dib.sms2324.ecowatering.hubconnectionthreads.BtAcceptingRequestThread;
import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class BtConnectionFragment extends Fragment {
    private static BluetoothAdapter bluetoothAdapter;
    private OnBtConnectionFinishCallback onBtConnectionFinishCallback;
    public interface OnBtConnectionFinishCallback {
        void onBtFinish(int btResultCode);
    }
    private final ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            ((result) -> {
                if(result.getResultCode() != Activity.RESULT_OK) {
                    ConnectToEWHubActivity.popBackStatFragment();
                }
                else {
                    onBtConnectionFinishCallback.onBtFinish(Common.BT_RESTART_FRAGMENT_RESULT);
                }
            }));

    public interface OnBtRequestResponseGivenCallback {
        void onResponseGiven(String response);
    }
    private final ActivityResultLauncher<Intent> makeBtDiscoverableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            ((result) -> {
                if(result.getResultCode() == Activity.RESULT_CANCELED) {
                    ConnectToEWHubActivity.popBackStatFragment();
                }
                else {
                    startAcceptingBtRequest();
                }
            })
    );

    private interface OnEnablingBluetoothCallback {
        void onEnablingBluetooth();
    }

    public BtConnectionFragment() {
        super(R.layout.fragment_bt_connection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnBtConnectionFinishCallback) {
            onBtConnectionFinishCallback = (OnBtConnectionFinishCallback) context;
        }
    }

   @Override
   public void onDetach() {
        super.onDetach();
        onBtConnectionFinishCallback = null;
   }

   @Override
   public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // TOOLBAR SETUP
       Toolbar toolbar = view.findViewById(R.id.toolBar);
       ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
       if(toolbar != null) {
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_connection_toolbar_title));
           toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
           requireActivity().addMenuProvider(new MenuProvider() {
               @Override
               public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                   menuInflater.inflate(R.menu.menu_manage_eco_watering_hub, menu);
               }

               @Override
               public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                   int itemID = menuItem.getItemId();
                   if(itemID == android.R.id.home) {
                       ConnectToEWHubActivity.popBackStatFragment();
                   }
                   else if(itemID == R.id.refreshEWHubItem) {
                       onBtConnectionFinishCallback.onBtFinish(Common.BT_RESTART_FRAGMENT_RESULT);
                   }
                   return false;
               }
           }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
       }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // BLUETOOTH NOT SUPPORTED CASE
        if(bluetoothAdapter == null) {
            showBtNotSupportedDialog();
        }
        // BLUETOOTH SUPPORTED CASE
        else {
            // ENABLE BLUETOOTH
            enableBluetooth(() -> {
                // MAKE DEVICE DISCOVERABLE
                Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Common.BT_DISCOVERABLE_DURATION);
                makeBtDiscoverableLauncher.launch(makeDiscoverableIntent);
            });
        }
   }

   protected void enableBluetooth(OnEnablingBluetoothCallback callback) {
       if(!bluetoothAdapter.isEnabled()) {
           enableBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
       }
       else {
           callback.onEnablingBluetooth();
       }
   }

   private void startAcceptingBtRequest() {
        new BtAcceptingRequestThread(requireContext(), bluetoothAdapter, ((response) -> {
            Log.i(Common.THIS_LOG, "brAcceptThread response: " + response);
           if(response.equals(Common.BT_ERROR_RESPONSE)) {
               requireActivity().runOnUiThread(this::showErrorDialog);
           }
           else if(response.equals(Common.BT_ALREADY_CONNECTED_DEVICE_RESPONSE)) {
               onBtConnectionFinishCallback.onBtFinish(Common.BT_ALREADY_CONNECTED_RESULT);
           }
           else {
               onBtConnectionFinishCallback.onBtFinish(Common.BT_CONNECTED_RESULT);
           }
        })).start();
   }

   private void showBtNotSupportedDialog() {
       AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
               .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_title))
               .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_message))
               .setPositiveButton(
                       getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                       ((dialogInterface, i) -> ConnectToEWHubActivity.popBackStatFragment()))
               .setCancelable(false);
       dialog.show();
   }

    /**
     * Notify the user that something went wrong.
     * Positive button dismiss dialog.
     */
    private void showErrorDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_title))
                .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.http_error_dialog_message))
                .setPositiveButton(
                        getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                        ((dialogInterface, i) -> requireActivity().finish()))
                .setCancelable(false);
        dialog.show();
    }
}
