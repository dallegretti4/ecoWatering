package it.uniba.dib.sms2324.ecowateringhub.remotedeviceconnectionthreads;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.HttpHelper;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;
import it.uniba.dib.sms2324.ecowateringhub.WiFiConnectionFragment;

public class WiFiConnectionRequestThread extends Thread {
    private final Context context;
    private final String peerAddress;
    private static PrintWriter writer;
    private final WiFiConnectionFragment.OnWiFiConnectionResponseGivenCallback callback;

    public WiFiConnectionRequestThread(@NonNull Context context, @NonNull String peerAddress, WiFiConnectionFragment.OnWiFiConnectionResponseGivenCallback callback) {
        this.context = context;
        this.peerAddress = peerAddress;
        this.callback = callback;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(this.peerAddress, 8898)) {
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // SEND REQUEST NAME
            writer.println(Common.WIFI_SOCKET_REQUEST_NAME);
            // READ REMOTE DEVICE ID
            String remoteDeviceID = reader.readLine();
            Log.i(Common.THIS_LOG, "remote device id: " + remoteDeviceID);
            // SEND HTTP REQUEST TO UPDATE DATABASE SERVER
            String response = MainActivity.thisEcoWateringHub.addNewRemoteDevice(this.context, remoteDeviceID);
            Log.i(Common.THIS_LOG, "remote device response: " + response);
            writer.println(response);
            callback.getResponse(response);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
