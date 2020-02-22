package com.yarg0007.robotpicontroller.ssh;

import com.yarg0007.robotpicontroller.log.Logger;

import net.schmizz.keepalive.KeepAlive;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.Connection;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.UserAuthException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;

public class SshManagerTest {

    SshManager sshManager;
    SSHClient sshClient;
    Session session;
    Logger logger;

    @Before
    public void setup() throws Throwable {
        sshClient = Mockito.mock(SSHClient.class);
        session = Mockito.mock(Session.class);
        logger = new Logger();
        logger.setDisabledForTesting(true);
    }

    @After
    public void teardown() throws Throwable {
        if (sshManager != null) {
            sshManager.closeSshConnection();
        }
    }

    @Test
    public void connectionErrorStopsThread() throws Throwable {
        Mockito.doThrow(new IOException()).when(sshClient).connect(Mockito.anyString());
        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, session, logger);
        sshManager.start();
        Thread.sleep(2000);
        Assert.assertFalse("SSH Manager thread should not be running.", sshManager.isRunning());
    }

    public void interactiveShellCreationExceptionResultsInErrorNotification() throws Throwable {

        Connection connection = Mockito.mock(Connection.class);
        KeepAlive keepAlive = Mockito.mock(KeepAlive.class);

        Mockito.doNothing().when(sshClient).connect(Mockito.anyString());
        Mockito.doReturn(connection).when(sshClient).getConnection();
        Mockito.doReturn(keepAlive).when(connection).getKeepAlive();
        Mockito.doNothing().when(keepAlive).setKeepAliveInterval(Mockito.anyInt());
        Mockito.doNothing().when(sshClient).authPassword(Mockito.anyString(), Mockito.anyString());

        sshManager = new SshManager("testHost", "testUser", "testPassword", sshClient, session, logger);
        sshManager.start();

        sshManager.addObserver(); // TODO - create an observer type that waits for success / error with the specified payload and timeout

    }

    public void initialCommandPromptMatchFailureThrowsErrorNotification() throws Throwable {

    }

    public void commandExpectedMatchFailureThrowsErrorNotifiction() throws Throwable {

    }

    public void receiveCompletionNotification() throws Throwable {

    }

    public void removeObserverSoWeDontReceiveCompletionNotification() throws Throwable {

    }
}
