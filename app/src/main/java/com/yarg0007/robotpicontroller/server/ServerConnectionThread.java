package com.yarg0007.robotpicontroller.server;

import java.io.IOException;

public class ServerConnectionThread extends Thread {

    private final ServerConnectionClient client;
    private final String host;
    private final int port;
    private final boolean isConnect;
    private final boolean shutdown;
    private final ServerConnectionObserver observer;
    private final int retryDelayMs;

    public ServerConnectionThread(String host, int port, boolean isConnect,
                                  boolean shutdown, ServerConnectionObserver observer) {
        this.client = new ServerConnectionClient();
        this.host = host;
        this.port = port;
        this.isConnect = isConnect;
        this.shutdown = shutdown;
        this.observer = observer;
        this.retryDelayMs = 3000;
    }

    ServerConnectionThread(ServerConnectionClient client, String host, int port,
                           boolean isConnect, boolean shutdown, ServerConnectionObserver observer) {
        this.client = client;
        this.host = host;
        this.port = port;
        this.isConnect = isConnect;
        this.shutdown = shutdown;
        this.observer = observer;
        this.retryDelayMs = 100;
    }

    @Override
    public void run() {
        if (isConnect) {
            runConnect();
        } else {
            runDisconnect();
        }
    }

    private void runConnect() {
        int maxAttempts = 5;
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean success = client.connect(host, port);
                if (success) {
                    observer.onConnectSuccess();
                    return;
                } else {
                    observer.onConnectFailure("Server returned non-200 response to /connect");
                    return;
                }
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        observer.onConnectFailure("Connection error after " + maxAttempts + " attempts: "
                + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private void runDisconnect() {
        try {
            boolean success = client.disconnect(host, port, shutdown);
            if (success) {
                observer.onDisconnectComplete();
            } else {
                observer.onDisconnectFailure("Server returned non-200 response to /disconnect");
            }
        } catch (IOException e) {
            observer.onDisconnectFailure("Disconnect error: " + e.getMessage());
        }
    }
}
