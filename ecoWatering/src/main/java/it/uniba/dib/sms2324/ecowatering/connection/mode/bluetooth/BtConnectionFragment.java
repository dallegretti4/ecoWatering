package it.uniba.dib.sms2324.ecowatering.connection.mode.bluetooth;

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

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.R;
import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;

public class BtConnectionFragment extends Fragment {
    private static final int BT_DISCOVERABLE_DURATION = 180;
    private BluetoothAdapter bluetoothAdapter;
    private OnConnectionFinishCallback onConnectionFinishCallback;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(it.uniba.dib.sms2324.ecowateringcommon.R.menu.menu_refresh_item_only, menu);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemID = menuItem.getItemId();
            if(itemID == android.R.id.home)
                onConnectionFinishCallback.closeConnection();
            else if(itemID == it.uniba.dib.sms2324.ecowateringcommon.R.id.refreshItem)
                onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH);
            return false;
        }
    };
    private final ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            ((result) -> {
                if(result.getResultCode() == Activity.RESULT_CANCELED)
                    onConnectionFinishCallback.closeConnection();
                else
                    onConnectionFinishCallback.restartFragment(OnConnectionFinishCallback.CONNECTION_MODE_BLUETOOTH);
            }));

    private final ActivityResultLauncher<Intent> makeBtDiscoverableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            ((result) -> {
                if(result.getResultCode() != Activity.RESULT_CANCELED)
                    startAcceptingBtRequest();
                else
                    onConnectionFinishCallback.closeConnection();
            })
    );

    public BtConnectionFragment() {
        super(R.layout.fragment_bt_connection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectionFinishCallback) {
            onConnectionFinishCallback = (OnConnectionFinishCallback) context;
        }
    }

   @Override
   public void onDetach() {
        super.onDetach();
        onConnectionFinishCallback = null;
   }

   @Override
   public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
       Common.lockLayout(requireActivity());
       toolbarSetup(view);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)    // BLUETOOTH NOT SUPPORTED CASE
            showBtNotSupportedDialog();
        else  // ENABLE BLUETOOTH
            enableBluetooth(() -> {
                // MAKE DEVICE DISCOVERABLE
                Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_DISCOVERABLE_DURATION);
                makeBtDiscoverableLauncher.launch(makeDiscoverableIntent);
            });
   }

   @Override
   public void onStop() {
        super.onStop();
       this.makeBtDiscoverableLauncher.unregister();
       this.bluetoothAdapter = null;
       Common.unlockLayout(requireActivity());
   }

   private void toolbarSetup(@NonNull View view) {
       Toolbar toolbar = view.findViewById(R.id.toolBar);
       ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
       if(toolbar != null) {
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_connection_toolbar_title));
           toolbar.setTitleTextAppearance(requireContext(), it.uniba.dib.sms2324.ecowateringcommon.R.style.toolBarTitleStyle);
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
           Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setHomeAsUpIndicator(it.uniba.dib.sms2324.ecowateringcommon.R.drawable.back_icon);
           requireActivity().addMenuProvider(this.menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
       }
   }

   private void enableBluetooth(Common.OnMethodFinishCallback callback) {
       if(!bluetoothAdapter.isEnabled()) {
           enableBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
       }
       else {
           callback.canContinue();
       }
   }

   private void startAcceptingBtRequest() {
        new BtAcceptingRequestThread(requireContext(), this.bluetoothAdapter, ((response) -> {
           if(response.equals(OnConnectionFinishCallback.BT_ERROR_RESPONSE))
               requireActivity().runOnUiThread(this::showErrorDialog);
           else if(response.equals(OnConnectionFinishCallback.BT_ALREADY_CONNECTED_DEVICE_RESPONSE))
               onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT);
           else
               onConnectionFinishCallback.onConnectionFinish(OnConnectionFinishCallback.CONNECTION_CONNECTED_DEVICE_RESULT);
        })).start();
   }

   private void showBtNotSupportedDialog() {
       AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
               .setTitle(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_title))
               .setMessage(getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.bt_not_supported_dialog_message))
               .setPositiveButton(
                       getString(it.uniba.dib.sms2324.ecowateringcommon.R.string.close_button),
                       ((dialogInterface, i) -> onConnectionFinishCallback.closeConnection()))
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
                        ((dialogInterface, i) -> {
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        }))
                .setCancelable(false);
        dialog.show();
    }
}
