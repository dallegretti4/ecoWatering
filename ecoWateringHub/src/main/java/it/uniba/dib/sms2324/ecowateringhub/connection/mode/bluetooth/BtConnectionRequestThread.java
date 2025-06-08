package it.uniba.dib.sms2324.ecowateringhub.connection.mode.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.OnConnectionFinishCallback;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class BtConnectionRequestThread extends Thread {
    private final Context context;
    private final BluetoothDevice device;
    private final Common.OnStringResponseGivenCallback callback;
    private BluetoothSocket bluetoothSocket;

    public BtConnectionRequestThread(@NonNull Context context, @NonNull BluetoothDevice device, Common.OnStringResponseGivenCallback callback) {
        this.context = context;
        this.device = device;
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(Common.getThisUUID())){
            this.bluetoothSocket = socket;
            this.bluetoothSocket.connect();
            InputStream inputStream = this.bluetoothSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int byteRead = inputStream.read(buffer);
            String remoteDeviceID = new String(buffer, 0, byteRead);
            // SEND HTTP REQUEST TO UPDATE DATABASE SERVER
            String response = MainActivity.getThisEcoWateringHub().addNewRemoteDevice(this.context, remoteDeviceID);
            try {   // RESPONSE SENDING
                OutputStream outputStream = this.bluetoothSocket.getOutputStream();
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
            this.callback.getResponse(OnConnectionFinishCallback.BT_ERROR_RESPONSE);
        }
    }

    public void closeSocket() {
        try {
            if(this.bluetoothSocket != null)
                this.bluetoothSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            interrupt();
        }
    }
}
