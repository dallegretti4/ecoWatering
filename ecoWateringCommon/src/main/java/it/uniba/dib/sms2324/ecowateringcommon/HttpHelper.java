package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpHelper {
    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Return true if device is connected to internet.
     */
    public static boolean isDeviceConnectedToInternet(@NonNull Context context) {
        ConnectivityManager cManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cManager != null) {
            NetworkCapabilities networkCapabilities = cManager.getNetworkCapabilities(cManager.getActiveNetwork());
            if(networkCapabilities != null) {
                return ((networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)));
            }
            else return false;
        }
        return false;
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String url -> to database server;
     *  {@code @NonNull} String jsonRequest -> to query database server;
     * To send a specific request to database server.
     */
    public static String sendHttpPostRequest(@NonNull String urlString, @NonNull String jsonRequest) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
            int responseCode = connection.getResponseCode();
            // GET RESPONSE FROM SERVER
            InputStream inputStream;
            if((responseCode >= 200) && (responseCode < 300)) {
                inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                return response.toString();
            }
            else {
                return Common.HTTP_RESPONSE_ERROR;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(connection != null) connection.disconnect();
        }
        return Common.HTTP_RESPONSE_ERROR;
    }

    public static String sendHttpGetRequest(@NonNull String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();
            return response.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(connection != null) connection.disconnect();
        }
        return Common.HTTP_RESPONSE_ERROR;
    }
}
