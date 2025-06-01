package it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class BtDiscoveryThread extends Thread {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device != null) {
                    if(!BtConnectionFragment.getBtDeviceList().contains(device)) {
                        @SuppressLint("MissingPermission") String deviceName = device.getName();
                        if(deviceName != null) {
                            Log.i(Common.LOG_NORMAL, "device founded");
                            BtConnectionFragment.addToBtDeviceList(device);
                            BtConnectionFragment.addToDeviceListAdapter(deviceName);
                        }
                    }
                }
            }
        }
    };

    protected BtDiscoveryThread(@NonNull Context context, @NonNull BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        IntentFilter btFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.context.registerReceiver(deviceReceiver, btFoundIntentFilter);
    }
}
