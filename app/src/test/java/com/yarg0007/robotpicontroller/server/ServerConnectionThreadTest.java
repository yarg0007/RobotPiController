package com.yarg0007.robotpicontroller.server;

import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class ServerConnectionThreadTest {

    private final ServerConnectionClient client = mock(ServerConnectionClient.class);
    private final ServerConnectionObserver observer = mock(ServerConnectionObserver.class);

    @Test
    public void connectSuccessCallsOnConnectSuccess() throws Exception {
        when(client.connect(anyString(), anyInt())).thenReturn(true);
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, true, false, observer);
        thread.start();
        thread.join(5000);
        verify(observer, times(1)).onConnectSuccess();
        verify(observer, never()).onConnectFailure(anyString());
    }

    @Test
    public void connectNon200CallsOnConnectFailure() throws Exception {
        when(client.connect(anyString(), anyInt())).thenReturn(false);
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, true, false, observer);
        thread.start();
        thread.join(5000);
        verify(observer, times(1)).onConnectFailure(anyString());
        verify(observer, never()).onConnectSuccess();
    }

    @Test
    public void connectIoExceptionRetriesAndCallsFailure() throws Exception {
        when(client.connect(anyString(), anyInt())).thenThrow(new IOException("refused"));
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, true, false, observer);
        thread.start();
        thread.join(5000);
        verify(client, times(5)).connect(anyString(), anyInt());
        verify(observer, times(1)).onConnectFailure(anyString());
    }

    @Test
    public void connectSucceedsOnThirdRetry() throws Exception {
        when(client.connect(anyString(), anyInt()))
                .thenThrow(new IOException("not ready"))
                .thenThrow(new IOException("not ready"))
                .thenReturn(true);
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, true, false, observer);
        thread.start();
        thread.join(5000);
        verify(client, times(3)).connect(anyString(), anyInt());
        verify(observer, times(1)).onConnectSuccess();
    }

    @Test
    public void disconnectSuccessCallsOnDisconnectComplete() throws Exception {
        when(client.disconnect(anyString(), anyInt(), anyBoolean())).thenReturn(true);
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, false, false, observer);
        thread.start();
        thread.join(5000);
        verify(observer, times(1)).onDisconnectComplete();
        verify(observer, never()).onDisconnectFailure(anyString());
    }

    @Test
    public void disconnectIoExceptionCallsOnDisconnectFailure() throws Exception {
        when(client.disconnect(anyString(), anyInt(), anyBoolean()))
                .thenThrow(new IOException("connection refused"));
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, false, false, observer);
        thread.start();
        thread.join(5000);
        verify(observer, times(1)).onDisconnectFailure(anyString());
    }

    @Test
    public void disconnectPassesShutdownFlag() throws Exception {
        when(client.disconnect(anyString(), anyInt(), eq(true))).thenReturn(true);
        ServerConnectionThread thread = new ServerConnectionThread(
                client, "localhost", 8001, false, true, observer);
        thread.start();
        thread.join(5000);
        verify(client, times(1)).disconnect(anyString(), anyInt(), eq(true));
    }
}
