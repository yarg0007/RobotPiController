package com.yarg0007.robotpicontroller.input;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ControllerDataClientTest {

    private DatagramSocket serverSocket;
    private int serverPort;

    @Before
    public void setUp() throws Exception {
        serverSocket = new DatagramSocket(0); // bind to random free port
        serverSocket.setSoTimeout(500);
        serverPort = serverSocket.getLocalPort();
    }

    @After
    public void tearDown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private String receive() throws Exception {
        byte[] buf = new byte[64];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        serverSocket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    @Test
    public void sendData_appendsTerminatorAndTransmits() throws Exception {
        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        client.open();
        client.sendData("0,0,0,0,0,0:?");
        assertEquals("0,0,0,0,0,0:??", receive());
    }

    @Test
    public void sendData_transmitsCorrectValues() throws Exception {
        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        client.open();
        client.sendData("100,-50,75,-25,1,0:?");
        assertEquals("100,-50,75,-25,1,0:??", receive());
    }

    @Test
    public void sendData_atLengthLimit_isDropped() throws Exception {
        // MAX_DATA_CHAR_LEN = 32; a 32-char string must be dropped
        String oversized = "12345678901234567890123456789012"; // exactly 32 chars
        assertEquals(32, oversized.length());

        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        client.open();
        client.sendData(oversized);

        boolean received = false;
        try {
            receive();
            received = true;
        } catch (java.net.SocketTimeoutException ignored) {}

        assertFalse("Message at length limit should be dropped", received);
    }

    @Test
    public void sendData_oneUnderLimit_isSent() throws Exception {
        // 31 chars (one below MAX_DATA_CHAR_LEN) must go through
        String payload = "1234567890123456789012345678901"; // 31 chars
        assertEquals(31, payload.length());

        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        client.open();
        client.sendData(payload);

        assertEquals(payload + "?", receive());
    }

    @Test
    public void sendData_beforeOpen_doesNotThrow() {
        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        // Must not throw even with no open socket
        client.sendData("0,0,0,0,0,0:?");
    }

    @Test
    public void sendData_worstCaseControlMessage_isUnderLimit() throws Exception {
        // Worst-case: all axes at -100, both booleans 1
        // "-100,-100,-100,-100,1,1:?" = 25 chars, well under 32
        ControllerDataClient client = new ControllerDataClient("localhost", serverPort);
        client.open();
        client.sendData("-100,-100,-100,-100,1,1:?");
        String received = receive();
        assertEquals("-100,-100,-100,-100,1,1:??", received);
    }
}
