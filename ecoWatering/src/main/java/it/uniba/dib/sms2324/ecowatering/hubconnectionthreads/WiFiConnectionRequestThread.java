package it.uniba.dib.sms2324.ecowatering.hubconnectionthreads;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import it.uniba.dib.sms2324.ecowatering.MainActivity;
import it.uniba.dib.sms2324.ecowatering.WiFiConnectionFragment;
import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class WiFiConnectionRequestThread extends Thread {
    private final Context context;
    private final WiFiConnectionFragment.OnWiFiConnectionResponseGivenCallback callback;

    public WiFiConnectionRequestThread(@NonNull Context context, WiFiConnectionFragment.OnWiFiConnectionResponseGivenCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(8898)) {
            Socket socket = serverSocket.accept();
            Log.i(Common.THIS_LOG, "accept()");
            // READ REQUEST NAME
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String requestName = reader.readLine();
            if(requestName.equals(Common.WIFI_SOCKET_REQUEST_NAME)) {
                // SEND THIS DEVICE ID
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(Common.getThisDeviceID(this.context));
                Log.i(Common.THIS_LOG, "device id sent");
                // READ RESPONSE FROM ECO WATERING HUB
                String response = reader.readLine();
                Log.i(Common.THIS_LOG, "response from reader: " + response);
                callback.getResponse(response);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
