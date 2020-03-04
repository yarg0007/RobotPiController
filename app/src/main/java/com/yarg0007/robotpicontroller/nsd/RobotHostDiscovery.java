package com.yarg0007.robotpicontroller.nsd;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

public class RobotHostDiscovery implements NsdManager.DiscoveryListener {

    private boolean searching = false;

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        searching = false;
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        searching = false;
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        searching = true;
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        searching = false;
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {

    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {

    }
}
