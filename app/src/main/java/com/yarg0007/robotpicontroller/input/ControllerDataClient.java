package com.yarg0007.robotpicontroller.input;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ControllerDataClient {

    /** The datagram client. Setup to only allow a single client connection.*/
    private DatagramSocket clientDatagramSocket;

    /** Data buffer sent in packet */
    private byte[] data;

    /** Packet sent to server. */
    private DatagramPacket clientDatagramPacket;

    /** Server port to send packet to. */
    private int serverPort;

    /** Sever address to send packet data to. */
    private String serverAddress;

    /** Server to send packet to. */
    private InetAddress server;

    /** Maximum number of characters allowed in the data package. */
    private static final int MAX_DATA_CHAR_LEN = 32;

    /**
     * Default constructor.
     * @param serverAddress Server address to send data to.
     * @param serverPort Server port to send data to.
     */
    public ControllerDataClient(String serverAddress, int serverPort) {

        this.serverPort = serverPort;
        this.serverAddress = serverAddress;

        init();
    }

    public void sendData(String dataString) {

        if (dataString.length() >= MAX_DATA_CHAR_LEN) {
            return;
        }

        if (clientDatagramSocket == null || server == null) {
            return;
        }

        // append termination character.
        dataString = dataString + "?";

        data = dataString.getBytes();

        clientDatagramPacket = new DatagramPacket(
                data, data.length, server, this.serverPort);

        try {
            clientDatagramSocket.send(clientDatagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    /**
     * Initialize the server and socket.
     */
    private void init() {

        try {
            this.server = InetAddress.getByName(serverAddress);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            return;
        }

        try {
            clientDatagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
    }
}
