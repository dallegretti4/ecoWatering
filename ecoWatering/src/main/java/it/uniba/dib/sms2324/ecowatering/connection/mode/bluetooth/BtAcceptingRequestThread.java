package it.uniba.dib.sms2324.ecowatering.connection.mode.bluetooth;

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

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class BtAcceptingRequestThread extends Thread {
    private static final String BT_SERVER_SOCKET_REQUEST_NAME = "ecoWateringBtRequest";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Common.OnStringResponseGivenCallback callback;

    protected BtAcceptingRequestThread(
            @NonNull Context context,
            @NonNull BluetoothAdapter bluetoothAdapter,
            @NonNull Common.OnStringResponseGivenCallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    // NEED TO BE CALLED AFTER BLUETOOTH_CONNECT PERMISSION REQUEST
    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        try(BluetoothServerSocket btServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(BT_SERVER_SOCKET_REQUEST_NAME, Common.getThisUUID())) {
            BluetoothSocket btSocket = btServerSocket.accept();
            // DEVICE ID SENDING
            OutputStream outputStream = btSocket.getOutputStream();
            InputStream inputStream = btSocket.getInputStream();
            outputStream.write(Common.getThisDeviceID(this.context).getBytes());
            outputStream.flush();
            // TRY YO GET RESPONSE
            byte[] buffer = new byte[1024];
            int byteRead = inputStream.read(buffer);
            String response = new String(buffer, 0, byteRead);
            Log.i(Common.THIS_LOG, "btAcceptThread response: " + response);
            callback.getResponse(response);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
