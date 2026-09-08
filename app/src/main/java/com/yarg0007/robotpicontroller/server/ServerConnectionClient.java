package com.yarg0007.robotpicontroller.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerConnectionClient {

    private static final int TIMEOUT_MS = 10000;

    public boolean connect(String host, int port) throws IOException {
        URL url = new URL("http://" + host + ":" + port + "/connect");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        try {
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } finally {
            connection.disconnect();
        }
    }

    public boolean disconnect(String host, int port, boolean shutdown) throws IOException {
        URL url = new URL("http://" + host + ":" + port + "/disconnect");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String body = "{\"shutdown\":" + shutdown + "}";
        byte[] bodyBytes = body.getBytes("UTF-8");
        connection.setFixedLengthStreamingMode(bodyBytes.length);

        OutputStream out = connection.getOutputStream();
        out.write(bodyBytes);
        out.close();

        try {
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } finally {
            connection.disconnect();
        }
    }
}
