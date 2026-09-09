package com.yarg0007.robotpicontroller.server;

public interface ServerConnectionObserver {
    void onConnectSuccess();
    void onConnectFailure(String message);
    void onDisconnectComplete();
    void onDisconnectFailure(String message);
}
