package it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringhub.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class BtConnectionRequestThread extends Thread {
    private final Context context;
    private final BluetoothDevice device;
    private final BtConnectionFragment.OnBtConnectionResponseGivenCallback callback;
    private interface OnRemoteDeviceAddingCallback {
        void onRemoteDeviceAdding(String response);
    }

    public BtConnectionRequestThread(@NonNull Context context, @NonNull BluetoothDevice device, BtConnectionFragment.OnBtConnectionResponseGivenCallback callback) {
        this.context = context;
        this.device = device;
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(Common.getThisUUID())){
            socket.connect();
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int byteRead = inputStream.read(buffer);
            String remoteDeviceID = new String(buffer, 0, byteRead);
            // SEND HTTP REQUEST TO UPDATE DATABASE SERVER
            String response = MainActivity.thisEcoWateringHub.addNewRemoteDevice(this.context, remoteDeviceID);
            Log.i(Common.THIS_LOG, "response sending");
            // RESPONSE SENDING
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            this.callback.getResponse(response);
        }
        // FAILURE TO CONNECT CASE
        catch(IOException e) {
            e.printStackTrace();
            this.callback.getResponse(Common.BT_ERROR_RESPONSE);
        }
    }
}
