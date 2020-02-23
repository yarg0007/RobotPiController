package com.yarg0007.robotpicontroller.ssh;

import com.yarg0007.robotpicontroller.log.Logger;
import com.yarg0007.robotpicontroller.ssh.commands.CommandExpectPair;

import net.schmizz.keepalive.KeepAlive;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.Connection;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.sf.expectit.Expect;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SshManagerTest {

    SshManager sshManager;
    SSHClient sshClient;
    Session session;
    Logger logger;

    Connection connection;
    KeepAlive keepAlive;
    Session.Shell shell;
    Expect expect;

    @Before
    public void setup() throws Throwable {
        sshClient = Mockito.mock(SSHClient.class);
        session = Mockito.mock(Session.class);
        logger = new Logger();
        logger.setDisabledForTesting(true);

        connection = Mockito.mock(Connection.class);
        keepAlive = Mockito.mock(KeepAlive.class);
        shell = Mockito.mock(Session.Shell.class);
        expect = Mockito.mock(Expect.class);
    }

    @After
    public void teardown() throws Throwable {
        if (sshManager != null) {
            sshManager.closeSshConnection();
        }
    }

    @Test
    public void sshConnectionErrorStopsThread() throws Throwable {
        Mockito.doThrow(new IOException()).when(sshClient).connect(Mockito.anyString());
        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, logger);
        sshManager.start();
        Thread.sleep(2000);
        Assert.assertFalse("SSH Manager thread should not be running.", sshManager.isRunning());
    }

    @Test
    public void interactiveShellCreationExceptionResultsInErrorNotification() throws Throwable {

        setupBasicMocks();

        // Force the Connection Exception we are expecting to get a notification about.
        Mockito.doThrow(new ConnectionException("TEST")).when(session).startShell();

        // Setup the fake commands.
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("A", "B"));
        commands.add(new CommandExpectPair("B", "B"));
        SshCommandPayload errorPayload = new SshCommandPayload("errorID", commands);

        // Setup the observer to wait for the fake commands.
        SshObserverTester observerTester = new SshObserverTester(10);

        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, logger);
        sshManager.addObserver(observerTester);
        sshManager.openSshConnection();
        Thread.sleep(1000);
        sshManager.queuePayload(errorPayload);

        // Wait for the error notification
        observerTester.waitForError(errorPayload, "Error creating interactive shell. Exception: TEST");
    }

    @Test
    public void sshPromptNotReadyForInputThrowsErrorNotification() throws Throwable {

        setupBasicMocks();

        // This is how we inject the value that will simulate reading from the console.
        // In this case, the hard coded prompt is the terminal input $ character.
        // We don't want that to be present so we simulate that scenario with a garbage character.
        InputStream inputStream = new InputStream() {

            byte[] data = "x".getBytes();
            int index = 0;

            @Override
            public int read() throws IOException {

                // Return -1 when the byte array has been exhausted.
                if (index >= data.length) {
                    return -1;
                }

                byte b = data[index++];
                return b;
            }
        };

        // Return the input stream from our mock shell.
        Mockito.doReturn(inputStream).when(shell).getInputStream();

        // Setup the fake commands.
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("A", "B"));
        commands.add(new CommandExpectPair("B", "B"));
        SshCommandPayload errorPayload = new SshCommandPayload("errorID", commands);

        // Setup the observer to wait for the fake commands.
        SshObserverTester observerTester = new SshObserverTester(10);

        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, logger);
        sshManager.addObserver(observerTester);
        sshManager.openSshConnection();
        Thread.sleep(1000);
        sshManager.queuePayload(errorPayload);

        // Wait for the error notification
        observerTester.waitForError(errorPayload, "SSH prompt is not ready for input. $ not found.");
    }

    @Test
    public void commandExpectedOutputTimeoutWaitingForCharacterMatchThrowsErrorNotification() throws Throwable {

        setupBasicMocks();

        // This is how we inject the value that will simulate reading from the console.
        // In this case, we simulate an input stream that will force the timeout.
        InputStream inputStream = new InputStream() {

            @Override
            public int read() throws IOException {
                return 1;
            }
        };

        // Return the input stream from our mock shell.
        Mockito.doReturn(inputStream).when(shell).getInputStream();

        // Setup the fake commands.
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("A", "B"));
        commands.add(new CommandExpectPair("B", "B"));
        SshCommandPayload errorPayload = new SshCommandPayload("errorID", commands);

        // Setup the observer to wait for the fake commands.
        SshObserverTester observerTester = new SshObserverTester(10);

        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, logger);
        sshManager.addObserver(observerTester);
        sshManager.openSshConnection();
        Thread.sleep(1000);
        sshManager.queuePayload(errorPayload);

        // Wait for the error notification
        observerTester.waitForError(errorPayload, "SSH prompt is not ready for input. $ not found.");
    }

    @Test
    public void commandExpectedOutputMatchFailureThrowsErrorNotifiction() throws Throwable {

        setupBasicMocks();

        byte[] prompt = "$".getBytes();
        byte[] expectPrompt = "Z".getBytes();

        ArrayList<Byte> byteData = new ArrayList<>();
        for (byte b : prompt) {
            byteData.add(b);
        }

        byteData.add((byte)-1);

        for (byte b : expectPrompt) {
            byteData.add(b);
        }

        final Byte[] data = byteData.toArray(new Byte[0]);

        // This is how we inject the value that will simulate reading from the console.
        // In this case, we simulate an input stream that will force the timeout.
        InputStream inputStream = new InputStream() {

            int index = 0;

            @Override
            public int read() throws IOException {

                // Return -1 when the byte array has been exhausted.
                if (index >= data.length) {
                    return -1;
                }

                byte b = data[index++];
                return b;
            }
        };

        OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                return;
            }
        };

        // Return the input stream from our mock shell.
        Mockito.doReturn(inputStream).when(shell).getInputStream();
        Mockito.doReturn(outputStream).when(shell).getOutputStream();

        // Setup the fake commands.
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("A", "A"));
        commands.add(new CommandExpectPair("B", "B"));
        SshCommandPayload errorPayload = new SshCommandPayload("errorID", commands);

        // Setup the observer to wait for the fake commands.
        SshObserverTester observerTester = new SshObserverTester(10);

        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, logger);
        sshManager.addObserver(observerTester);
        sshManager.openSshConnection();
        Thread.sleep(1000);
        sshManager.queuePayload(errorPayload);

        // Wait for the error notification
        observerTester.waitForError(errorPayload, "Error occurred executing statement A. Exception message:");
    }

    public void receiveSuccessfulCommandExecutionNotification() throws Throwable {

    }

    public void removeObserverSoWeDontReceiveCompletionNotification() throws Throwable {

    }

    private void setupBasicMocks() throws Throwable {

        Mockito.doReturn(session).when(sshClient).startSession();
        Mockito.doReturn(connection).when(sshClient).getConnection();
        Mockito.doReturn(keepAlive).when(connection).getKeepAlive();
        Mockito.doReturn(shell).when(session).startShell();

        Mockito.doNothing().when(session).allocateDefaultPTY();

        Mockito.doNothing().when(keepAlive).setKeepAliveInterval(Mockito.anyInt());

        Mockito.doNothing().when(sshClient).connect(Mockito.anyString());
        Mockito.doNothing().when(sshClient).connect(Mockito.anyString(), Mockito.anyInt());
        Mockito.doNothing().when(sshClient).authPassword(Mockito.anyString(), Mockito.anyString());

        Mockito.doReturn(null).when(shell).getOutputStream();
        Mockito.doReturn(null).when(shell).getInputStream();
        Mockito.doReturn(null).when(shell).getErrorStream();
    }
}
