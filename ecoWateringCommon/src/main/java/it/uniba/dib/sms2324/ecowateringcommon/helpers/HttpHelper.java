package it.uniba.dib.sms2324.ecowateringcommon.helpers;

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
    public static final String HTTP_RESPONSE_ERROR = "error";
    private static final String REQUEST_MODE_GET = "GET";
    private static final String REQUEST_MODE_POST = "POST";
    private static final String REQUEST_PROPERTY_CONTENT_TYPE_LABEL = "Content-Type";
    private static final String REQUEST_PROPERTY_CONTENT_TYPE_VALUE = "application/json; utf-8";
    private static final String REQUEST_PROPERTY_ACCEPT_LABEL = "Accept";
    private static final String REQUEST_PROPERTY_ACCEPT_VALUE = "application/json";
    public static final String MODE_PARAMETER = "MODE";
    public static final String MODE_LIGHT_SENSOR_EVENT = "LIGHT_SENSOR_EVENT";
    public static final String MODE_DETACH_SENSOR = "DETACH_SENSOR";
    public static final String MODE_DEVICE_EXISTS = "DEVICE_EXISTS";
    public static final String MODE_ADD_NEW_DEVICE = "ADD_NEW_DEVICE";
    public static final String MODE_GET_DEVICE_OBJ = "GET_DEVICE_OBJ";
    public static final String MODE_DISCONNECT_FROM_EWH = "DISCONNECT_FROM_EWH";
    public static final String MODE_SET_DEVICE_NAME = "SET_DEVICE_NAME";
    public static final String MODE_DELETE_DEVICE_ACCOUNT = "DELETE_DEVICE_ACCOUNT";
    public static final String MODE_HUB_EXISTS = "HUB_EXISTS";
    public static final String MODE_ADD_NEW_HUB = "ADD_NEW_HUB";
    public static final String MODE_GET_HUB_OBJ = "GET_HUB_OBJ";
    public static final String MODE_ADD_REMOTE_DEVICE = "ADD_REMOTE_DEVICE";
    public static final String MODE_REMOVE_REMOTE_DEVICE = "REMOVE_REMOTE_DEVICE";
    public static final String MODE_ADD_NEW_SENSOR = "ADD_NEW_SENSOR";
    public static final String MODE_UPDATE_AMBIENT_TEMPERATURE_SENSOR = "UPDATE_AMBIENT_TEMPERATURE_SENSOR";
    public static final String MODE_UPDATE_LIGHT_SENSOR = "UPDATE_LIGHT_SENSOR";
    public static final String MODE_UPDATE_RELATIVE_HUMIDITY_SENSOR = "UPDATE_RELATIVE_HUMIDITY_SENSOR";
    public static final String MODE_SET_IRRIGATION_SYSTEM_STATE = "SET_IRRIGATION_SYSTEM_STATE";
    public static final String MODE_UPDATE_WEATHER_INFO = "UPDATE_WEATHER_INFO";
    public static final String MODE_SET_HUB_NAME = "SET_HUB_NAME";
    public static final String MODE_DELETE_HUB_ACCOUNT = "DELETE_HUB_ACCOUNT";
    public static final String MODE_SEND_REQUEST = "SEND_REQUEST";
    public static final String MODE_GET_DEVICE_REQUEST = "GET_DEVICE_REQUEST";
    public static final String MODE_SET_IS_DATA_OBJECT_REFRESHING = "SET_IS_DATA_OBJECT_REFRESHING";
    public static final String MODE_DELETE_DEVICE_REQUEST = "DELETE_DEVICE_REQUEST";
    public static final String MODE_GET_IRRIGATION_PLAN_PREVIEW = "GET_IRRIGATION_PLAN_PREVIEW";
    public static final String MODE_UPDATE_IRRIGATION_PLAN = "UPDATE_IRRIGATION_PLAN";
    public static final String MODE_SET_IS_AUTOMATED = "SET_IS_AUTOMATED";
    public static final String MODE_UPDATE_SENSOR_LIST = "UPDATE_SENSOR_LIST";
    public static final String TIME_PARAMETER = "TIME";
    public static final String SENSOR_TYPE_PARAMETER = "SENSOR_TYPE";
    public static final String SENSOR_ID_PARAMETER = "SENSOR_ID";
    public static final String HUB_PARAMETER = "HUB";
    public static final String NEW_NAME_PARAMETER = "NEW_NAME";
    public static final String REMOTE_DEVICE_PARAMETER = "REMOTE_DEVICE";
    public static final String VALUE_PARAMETER = "VALUE";
    public static final String REQUEST_PARAMETER = "REQUEST";


    /**
     *
     * @param context starting
     * @return true if device is connected to internet
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
            //CONNECTION & REQUEST SETUP
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(REQUEST_MODE_POST);
            connection.setRequestProperty(REQUEST_PROPERTY_CONTENT_TYPE_LABEL, REQUEST_PROPERTY_CONTENT_TYPE_VALUE);
            connection.setRequestProperty(REQUEST_PROPERTY_ACCEPT_LABEL, REQUEST_PROPERTY_ACCEPT_VALUE);
            connection.setDoOutput(true);
            // REQUEST SENDING
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
                return HTTP_RESPONSE_ERROR;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(connection != null) connection.disconnect();
        }
        return HTTP_RESPONSE_ERROR;
    }

    public static String sendHttpGetRequest(@NonNull String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(REQUEST_MODE_GET);

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
        return HTTP_RESPONSE_ERROR;
    }
}
