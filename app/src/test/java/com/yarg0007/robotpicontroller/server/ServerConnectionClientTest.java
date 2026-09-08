package com.yarg0007.robotpicontroller.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.junit.Assert.*;

public class ServerConnectionClientTest {

    private static final int PORT = 19877;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile String lastRequestLine = null;
    private volatile String lastRequestBody = null;
    private volatile int responseCode = 200;
    private volatile boolean serverRunning = false;

    private final ServerConnectionClient client = new ServerConnectionClient();

    @Before
    public void setUp() throws Exception {
        lastRequestLine = null;
        lastRequestBody = null;
        responseCode = 200;
        serverSocket = new ServerSocket(PORT);
        serverRunning = true;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (serverRunning) {
                    try {
                        serverSocket.setSoTimeout(300);
                        Socket socket;
                        try {
                            socket = serverSocket.accept();
                        } catch (SocketTimeoutException e) {
                            continue;
                        }
                        handleRequest(socket);
                    } catch (IOException e) {
                        // server shutting down
                    }
                }
            }
        });
        serverThread.start();
    }

    private void handleRequest(Socket socket) throws IOException {
        // Use 1-byte buffer so reader doesn't consume body bytes into its buffer
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), 1);

        // Read request line
        lastRequestLine = reader.readLine();

        // Read headers, find Content-Length
        int contentLength = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }

        // Read body
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int read = 0;
            while (read < contentLength) {
                int n = reader.read(bodyChars, read, contentLength - read);
                if (n == -1) break;
                read += n;
            }
            lastRequestBody = new String(bodyChars, 0, read);
        }

        // Send response
        OutputStream out = socket.getOutputStream();
        String status = responseCode == 200 ? "HTTP/1.1 200 OK" : "HTTP/1.1 " + responseCode + " Error";
        String body = "{\"message\":\"ok\"}";
        String response = status + "\r\nContent-Type: application/json\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
        out.write(response.getBytes("UTF-8"));
        out.flush();
        socket.close();
    }

    @After
    public void tearDown() throws Exception {
        serverRunning = false;
        serverSocket.close();
        serverThread.join(2000);
    }

    @Test
    public void connectReturns200() throws Exception {
        boolean result = client.connect("localhost", PORT);
        assertTrue(result);
        assertNotNull(lastRequestLine);
        assertTrue(lastRequestLine.startsWith("GET"));
        assertTrue(lastRequestLine.contains("/connect"));
    }

    @Test
    public void connectReturns500ReturnsFalse() throws Exception {
        responseCode = 500;
        boolean result = client.connect("localhost", PORT);
        assertFalse(result);
    }

    @Test
    public void disconnectSendsShutdownFalse() throws Exception {
        boolean result = client.disconnect("localhost", PORT, false);
        assertTrue(result);
        assertNotNull(lastRequestLine);
        assertTrue(lastRequestLine.startsWith("POST"));
        assertTrue(lastRequestLine.contains("/disconnect"));
        assertNotNull(lastRequestBody);
        assertTrue(lastRequestBody.contains("\"shutdown\":false"));
    }

    @Test
    public void disconnectSendsShutdownTrue() throws Exception {
        boolean result = client.disconnect("localhost", PORT, true);
        assertTrue(result);
        assertNotNull(lastRequestBody);
        assertTrue(lastRequestBody.contains("\"shutdown\":true"));
    }

    @Test
    public void disconnectReturns500ReturnsFalse() throws Exception {
        responseCode = 500;
        boolean result = client.disconnect("localhost", PORT, false);
        assertFalse(result);
    }
}
