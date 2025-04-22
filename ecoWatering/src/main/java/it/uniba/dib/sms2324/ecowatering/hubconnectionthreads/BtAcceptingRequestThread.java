package it.uniba.dib.sms2324.ecowatering.hubconnectionthreads;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.uniba.dib.sms2324.ecowatering.BtConnectionFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class BtAcceptingRequestThread extends Thread {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BtConnectionFragment.OnBtRequestResponseGivenCallback callback;
    private interface OnMessageSentCallback {
        void onMessageSent() throws IOException;
    }

    public BtAcceptingRequestThread(
            @NonNull Context context,
            @NonNull BluetoothAdapter bluetoothAdapter,
            @NonNull BtConnectionFragment.OnBtRequestResponseGivenCallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    // NEED TO BE CALLED AFTER BLUETOOTH_CONNECT PERMISSION REQUEST
    @Override
    public void run() {
        try{
            // DEVICE ID SENDING
            @SuppressLint("MissingPermission") BluetoothServerSocket btServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Common.BT_SERVER_SOCKET_REQUEST_NAME, Common.getThisUUID());
            BluetoothSocket btSocket = btServerSocket.accept();
            OutputStream outputStream = btSocket.getOutputStream();
            InputStream inputStream = btSocket.getInputStream();
            outputStream.write(Common.getThisDeviceID(this.context).getBytes());
            outputStream.flush();
            // TRY YO GET RESPONSE
            byte[] buffer = new byte[1024];
            int byteRead = inputStream.read(buffer);
            String response = new String(buffer, 0, byteRead);
            Log.i(Common.THIS_LOG, "btAcceptThread response: " + response);
            callback.onResponseGiven(response);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
