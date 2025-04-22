package it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads;

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
import it.uniba.dib.sms2324.ecowateringhub.BtConnectionFragment;

public class BtDiscoveryThread extends Thread {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    protected final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device != null) {
                    if(!BtConnectionFragment.btDeviceList.contains(device)) {
                        @SuppressLint("MissingPermission") String deviceName = device.getName();
                        if(deviceName != null) {
                            Log.i(Common.THIS_LOG, "device founded");
                            BtConnectionFragment.btDeviceList.add(device);
                            BtConnectionFragment.deviceListAdapter.add(deviceName);
                            BtConnectionFragment.deviceListAdapter.notifyDataSetChanged();
                            Log.i(Common.THIS_LOG, BtConnectionFragment.deviceList.toString());
                        }
                    }
                }
            }
        }
    };

    public BtDiscoveryThread(@NonNull Context context, @NonNull BluetoothAdapter bluetoothAdapter) {
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
